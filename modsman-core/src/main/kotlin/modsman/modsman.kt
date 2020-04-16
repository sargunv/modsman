package modsman

import com.sangupta.murmur.Murmur2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
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
    numConcurrent: Int
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
                    modlist.config.requiredGameVersions.all { configVersion ->
                        file.gameVersion.any { fileVersion ->
                            configVersion in fileVersion
                        }
                    } &&
                    modlist.config.excludedGameVersions.none { configVersion ->
                        file.gameVersion.any { fileVersion ->
                            configVersion in fileVersion
                        }
                    }
            }
            .maxBy { file -> file.fileDate }
        return ret ?: throw ChooseFileException(modlist.config)
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
        return Files.readAllBytes(jarPath)
    }

    private suspend fun fingerprint(jarPath: Path): Long {
        val whitespace = setOf<Byte>(9, 10, 13, 32)
        val data = io { readToBytes(jarPath) }
            .filter { byte -> byte !in whitespace }.toByteArray()
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
        download(file.downloadUrl, file.fileName)
        val mod = ModEntry(
            projectId = projectId,
            projectName = projectName,
            fileId = file.fileId,
            fileName = file.fileName
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

        download(file.downloadUrl, file.fileName)
        return modlist.updateInstalled(mod.projectId, file.fileId, file.fileName)
    }

    @FlowPreview
    suspend fun addMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return io {
            curseforgeClient.getAddonsAsync(projectIds).await()
        }.parallelMapToResultFlow(downloadPool) { cfAddon ->
            installMod(cfAddon.addonId, cfAddon.name)
        }
    }

    @FlowPreview
    fun setPinnedMods(projectIds: List<Int>, pinned: Boolean): Flow<Result<ModEntry>> {
        return projectIds
            .mapNotNull(modlist::get)
            .toFlow { mod ->
                modlist.addOrUpdate(mod.copy(pinned = pinned))
                Result.success(mod)
            }
    }

    @FlowPreview
    suspend fun removeMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return projectIds
            .mapNotNull(modlist::get)
            .parallelMapToResultFlow(downloadPool) { mod ->
                io { deleteFile(mod) }
                modlist.remove(mod.projectId)
            }
            .filterNot { result -> result.exceptionOrNull() is ProjectNotFoundException }
    }

    @FlowPreview
    suspend fun upgradeMods(projectIds: List<Int>): Flow<Result<Pair<ModEntry, ModEntry>>> {
        return projectIds
            .mapNotNull(modlist::get)
            .parallelMapToResultFlow(downloadPool) { mod ->
                if (mod.pinned)
                    throw PinnedException(mod)
                mod to upgradeMod(mod)
            }
            .filter { result -> result.map { (old, new) -> old != new }.getOrElse { true } }
    }

    @FlowPreview
    suspend fun getOutdatedMods(): Flow<Result<Pair<ModEntry, String>>> {
        return modlist.mods
            .parallelMapToResultFlow(downloadPool) { mod ->
                if (mod.pinned)
                    throw PinnedException(mod)
                try {
                    mod to getBestFile(mod.projectId)
                } catch (e: ChooseFileException) {
                    throw UpgradeException(mod, e)
                }
            }
            .filter { result -> result.map { (mod, file) -> mod.fileId != file.fileId }.getOrElse { true } }
            .map { result -> result.map { (mod, file) -> mod to file.fileName } }
    }

    @FlowPreview
    suspend fun reinstallMods(projectIds: List<Int>): Flow<Result<ModEntry>> {
        return projectIds
            .mapNotNull { id -> modlist[id]?.fileId }
            .let { requests -> io { curseforgeClient.getFilesAsync(requests).await() } }
            .mapNotNull { (idStr, files) -> if (files.isEmpty()) null else modlist[idStr.toInt()]!! to files[0] }
            .parallelMapToResultFlow(downloadPool) { (mod, file) ->
                io { download(file.downloadUrl, mod.fileName) }
                mod
            }
    }

    @FlowPreview
    suspend fun matchMods(jars: List<Path>): Flow<Result<ModEntry>> {
        try {
            val fingerprintToJarPath = jars.map { jarPath -> fingerprint(jarPath) to jarPath.toAbsolutePath() }.toMap()
            val matchResult = io { curseforgeClient.fingerprintAsync(fingerprintToJarPath.keys.toList()).await() }
            if (matchResult.exactMatches.isEmpty()) {
                return emptyFlow()
            }

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
                Result.success(mod)
            }
        } catch (e: Throwable) {
            return flowOf(Result.failure(e))
        }
    }

    override fun close() {
        modlist.close()
        downloadPool.shutdown()
    }
}
