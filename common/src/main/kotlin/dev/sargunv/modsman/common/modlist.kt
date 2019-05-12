package dev.sargunv.modsman.common

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

data class ModEntry(
    val projectId: Int,
    val projectName: String,
    val fileId: Int,
    val fileName: String
)

data class Modlist(
    val config: Config,
    val mods: List<ModEntry>
) {
    data class Config(
        val gameVersion: String
    )

    companion object {

        internal const val fileName = ".modlist.json"

        internal val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create()!!
    }
}

class ModlistManager(val modsPath: Path, modlist: Modlist) : Closeable {

    val config = modlist.config
    private val modMap = modlist.mods.map { it.projectId to it }.toMap().toMutableMap()

    val mods get() = (modMap as Map<Int, ModEntry>).values

    fun addOrUpdate(mod: ModEntry) {
        modMap[mod.projectId] = mod
    }

    operator fun get(projectId: Int) = modMap[projectId]

    fun remove(projectId: Int): ModEntry? {
        return modMap.remove(projectId)
    }

    fun updateInstalled(projectId: Int, fileId: Int, fileName: String): ModEntry {
        val mod = modMap[projectId]!!.copy(
            fileId = fileId,
            fileName = fileName
        )
        modMap[projectId] = mod
        return mod
    }

    override fun close() {
        Files.newBufferedWriter(modsPath.resolve(Modlist.fileName)).use { writer ->
            Modlist.gson.toJson(
                Modlist(config = config, mods = modMap.values.toList()),
                writer
            )
        }
    }

    companion object {
        fun init(modsPath: Path, gameVersion: String): ModlistManager {
            modsPath.resolve(Modlist.fileName).let {
                if (Files.exists(it))
                    throw FileAlreadyExistsException(it.toFile())
                else
                    return ModlistManager(
                        modsPath = modsPath,
                        modlist = Modlist(config = Modlist.Config(gameVersion), mods = arrayListOf())
                    )
            }
        }

        fun load(modsPath: Path): ModlistManager {
            return ModlistManager(
                modsPath = modsPath,
                modlist = Modlist.gson.fromJson(
                    Files.newBufferedReader(modsPath.resolve(Modlist.fileName)),
                    Modlist::class.java
                )
            )
        }
    }
}
