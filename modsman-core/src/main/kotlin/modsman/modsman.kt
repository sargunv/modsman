package modsman

import com.sangupta.murmur.Murmur2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors

class Modsman(
    modsPath: Path,
    val numConcurrent: Int
) : Closeable {

    val modlist = ModlistManager.load(modsPath)
    private val curseforgeClient = CurseforgeService.createClient()
    private val downloadPool = Executors.newFixedThreadPool(numConcurrent)

    private val ModEntry.filePath get() = modlist.modsPath.resolve(fileName)

    private fun chooseBestFile(files: List<CurseforgeFile>): CurseforgeFile {
        val ret = files
            .filter { file ->
                file.isAvailable &&
                    !file.isAlternate &&
                    file.gameVersion.any { version ->
                        modlist.config.gameVersion in version
                    }
            }
            .maxBy { file -> file.fileDate }
            ?.let { it }
        return ret ?: throw ChooseFileException(modlist.config.gameVersion)
    }

    private fun deleteFile(mod: ModEntry): Boolean {
        return Files.deleteIfExists(mod.filePath)
    }

    private suspend fun download(url: String, fileName: String) = withContext(Dispatchers.IO) {
        Files.copy(
            OkHttpClient().newCall(Request.Builder().url(url).build()).execute().body()!!.byteStream(),
            modlist.modsPath.resolve(fileName),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    private fun readToBytes(jarPath: Path): ByteArray {
        return Files.newInputStream(jarPath).use { it.readAllBytes() }
    }

    private suspend fun fingerprint(jarPath: Path): Long {
        val whitespace = setOf<Byte>(9, 10, 13, 32)
        val data = io { readToBytes(jarPath) }
            .filter { it !in whitespace }.toByteArray()
        return Murmur2.hash(data, data.size, 1)
    }

    private suspend fun getBestFile(projectId: Int): CurseforgeFile {
        val files = withContext(Dispatchers.IO) {
            curseforgeClient.getAddonFilesAsync(projectId).await()
        }
        return chooseBestFile(files)
    }

    private suspend fun installMod(projectId: Int, projectName: String): ModEntry {
        val file = try {
            getBestFile(projectId)
        } catch (e: ChooseFileException) {
            throw InstallException(projectName, e)
        }
        download(file.downloadUrl, file.fileNameOnDisk)
        val mod = ModEntry(
            projectId = projectId,
            projectName = projectName,
            fileId = file.fileId,
            fileName = file.fileNameOnDisk
        )
        modlist.addOrUpdate(mod)
        return mod
    }

    private suspend fun upgradeMod(mod: ModEntry): ModEntry {
        val file = try {
            getBestFile(mod.projectId)
        } catch (e: ChooseFileException) {
            throw UpgradeException(mod, e)
        }

        if (file.fileId == mod.fileId)
            return mod

        withContext(Dispatchers.IO) {
            Files.deleteIfExists(mod.filePath)
        }

        download(file.downloadUrl, file.fileNameOnDisk)
        return modlist.updateInstalled(mod.projectId, file.fileId, file.fileNameOnDisk)
    }

    @FlowPreview
    suspend fun addMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return io {
            curseforgeClient.getAddonsAsync(projectIds).await()
        }.parallelMapToResultFlow(downloadPool) {
            installMod(it.addonId, it.name)
        }
    }

    @FlowPreview
    suspend fun removeMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return projectIds
            .mapNotNull(modlist::get)
            .parallelMapToResultFlow(downloadPool) {
                io { deleteFile(it) }
                modlist.remove(it.projectId)
            }
            .filterNot { it.exceptionOrNull() is ProjectNotFoundException }
    }

    @FlowPreview
    suspend fun upgradeMods(projectIds: List<Int>): Flow<Result<Pair<ModEntry, ModEntry>>> {
        return projectIds
            .mapNotNull(modlist::get)
            .parallelMapToResultFlow(downloadPool) { mod -> mod to upgradeMod(mod) }
            .filter { it.map { (old, new) -> old != new }.getOrElse { true } }
    }

    @FlowPreview
    suspend fun getOutdatedMods(): Flow<Result<Pair<ModEntry, String>>> {
        return modlist.mods
            .parallelMapToResultFlow(downloadPool) { mod ->
                try {
                    mod to getBestFile(mod.projectId)
                } catch (e: ChooseFileException) {
                    throw UpgradeException(mod, e)
                }
            }
            .filter { it.map { (mod, file) -> mod.fileId != file.fileId }.getOrElse { true } }
            .map { it.map { (mod, file) -> mod to file.fileNameOnDisk } }
    }

    @FlowPreview
    suspend fun reinstallMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return projectIds
            .mapNotNull { id -> modlist[id]?.let { mod -> CurseforgeFileRequest(mod.projectId, mod.fileId) } }
            .let { requests -> io { curseforgeClient.getFilesAsync(requests).await() } }
            .mapNotNull { (idStr, files) -> if (files.isEmpty()) null else modlist[idStr.toInt()]!! to files[0] }
            .parallelMapToResultFlow(downloadPool) { (mod, file) ->
                io { download(file.downloadUrl, mod.fileName) }
                mod
            }
    }

    @FlowPreview
    suspend fun matchMods(jars: List<Path>): Flow<Result<ModEntry>> {
        val fingerprintToJarPath = jars.map { jarPath -> fingerprint(jarPath) to jarPath.toAbsolutePath() }.toMap()
        val matchResult = io { curseforgeClient.fingerprintAsync(fingerprintToJarPath.keys.toList()).await() }
        val projectIdToFile = matchResult.exactMatches
            .map { result -> result.projectId to result.file }
            .toMap()
        val projectIdToAddon = io { curseforgeClient.getAddonsAsync(projectIdToFile.keys.toList()).await() }
            .map { addon -> addon.addonId to addon }
            .toMap()
        return projectIdToAddon.entries.toFlow { (projectId, addon) ->
            val file = projectIdToFile.getValue(projectId)
            val jarPath = fingerprintToJarPath.getValue(file.packageFingerprint)
            val mod = ModEntry(
                projectId = projectId,
                projectName = addon.name,
                fileId = file.fileId,
                fileName = modlist.modsPath.toAbsolutePath().relativize(jarPath).toString()
            )
            modlist.addOrUpdate(mod)
            Result.success(mod) // TODO properly Result-ify this
        }
    }

    fun save() = modlist.save()

    override fun close() {
        modlist.close()
        downloadPool.shutdown()
    }
}
