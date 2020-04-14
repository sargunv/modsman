package modsman

import com.google.gson.*
import java.io.Closeable
import java.lang.reflect.Type
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
        val requiredGameVersions: List<String>
    )

    companion object {

        const val fileName = ".modlist.json"

        internal val gson = GsonBuilder()
            .registerTypeAdapter(
                Config::class.java,
                JsonDeserializer { element: JsonElement,
                                   _: Type,
                                   _: JsonDeserializationContext ->
                    val obj = element.asJsonObject
                    Config(
                        when {
                            obj.has("required_game_versions") ->
                                obj.get("required_game_versions").asJsonArray.map { it.asString }
                            obj.has("game_version") ->
                                listOf(obj.get("game_version").asString)
                            else -> emptyList()
                        }
                    )
                }
            )
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

    fun remove(projectId: Int): ModEntry {
        return modMap.remove(projectId) ?: throw ProjectNotFoundException(projectId)
    }

    fun updateInstalled(projectId: Int, fileId: Int, fileName: String): ModEntry {
        val mod = modMap[projectId]!!.copy(
            fileId = fileId,
            fileName = fileName
        )
        modMap[projectId] = mod
        return mod
    }

    fun save() {
        Files.newBufferedWriter(modsPath.resolve(Modlist.fileName)).use { writer ->
            Modlist.gson.toJson(
                Modlist(config = config, mods = modMap.values.toList()),
                writer
            )
        }
    }

    override fun close() = save()

    companion object {
        fun init(modsPath: Path, gameVersions: List<String>): ModlistManager {
            modsPath.resolve(Modlist.fileName).let {
                if (Files.exists(it))
                    throw FileAlreadyExistsException(it.toFile())
                else
                    return ModlistManager(
                        modsPath = modsPath,
                        modlist = Modlist(config = Modlist.Config(gameVersions), mods = arrayListOf())
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
