package modsman.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import modsman.BuildConfig
import modsman.ModlistConfig
import modsman.ModlistManager
import modsman.Modsman
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import kotlin.system.exitProcess

internal sealed class CommandBase {
    abstract suspend fun run(jc: JCommander): Int
}

internal sealed class ProjectsCommand : CommandBase() {
    @Parameter(required = true, description = "PROJECT_ID [PROJECT_ID ...]")
    lateinit var projectIds: List<Int>
}

internal sealed class AllProjectsCommand(
    private val proxy: ProjectsCommand
) : CommandBase() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        proxy.projectIds = RootCommand.createModsman().modlist.mods.map { it.projectId }
        return proxy.run(jc)
    }
}

internal object RootCommand : CommandBase() {
    @Parameter(names = ["--help", "-h"], description = "show this help message and exit", help = true, order = 0)
    var help: Boolean = false

    @Parameter(names = ["--version", "-v"], description = "show program's version number and exit", help = true, order = 1)
    var version: Boolean = false

    @Parameter(names = ["--max-concurrent", "-C"], description = "the max number of mods to download concurrently", order = 2)
    var maxConcurrent: Int = 10

    @Parameter(names = ["--mods-folder", "-M"], description = "path to 'mods' directory", order = 3)
    var modsFolder: String = "."

    override suspend fun run(jc: JCommander): Int {
        return if (version) {
            JCommander.getConsole().println(BuildConfig.VERSION)
            0
        } else {
            jc.usage()
            1
        }
    }

    fun createModsman(): Modsman {
        try {
            return Modsman(Paths.get(modsFolder), maxConcurrent)
        } catch (e: NoSuchFileException) {
            JCommander.getConsole().println("Couldn't find file: ${e.message}")
            JCommander.getConsole().println("Did you forget to run `modsman init`?")
            exitProcess(1)
        }
    }
}

@Parameters(commandNames = ["init"], commandDescription = "initialize a new mod list")
internal object InitCommand : CommandBase() {
    @Parameter(
        names = ["--require-version", "-R"],
        required = true,
        description = "required game version tag; specify multiple times for multiple versions"
    )
    lateinit var requiredGameVersions: List<String>

    @Parameter(
        names = ["--exclude-version", "-X"],
        required = true,
        description = "excluded game version tag; specify multiple times for multiple versions"
    )
    lateinit var excludedGameVersions: List<String>

    override suspend fun run(jc: JCommander): Int {
        ModlistManager.init(
            Paths.get(RootCommand.modsFolder),
            ModlistConfig(
                requiredGameVersions = requiredGameVersions,
                excludedGameVersions = excludedGameVersions
            )
        ).close()
        return 0
    }
}

@Parameters(commandNames = ["add"], commandDescription = "download one or more mods")
internal object AddCommand : ProjectsCommand() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            modsman.addMods(projectIds).collectPrintingFailures {
                println("Downloaded '${it.projectName}' to '${it.fileName}'")
            }
        }
        return 0
    }
}

@Parameters(commandNames = ["remove"], commandDescription = "delete one or more mods")
internal object RemoveCommand : ProjectsCommand() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            modsman.removeMods(projectIds).collectPrintingFailures {
                println("Deleted '${it.fileName}'")
            }
        }
        return 0
    }
}

@Parameters(commandNames = ["upgrade"], commandDescription = "update one or more mods")
internal object UpgradeCommand : ProjectsCommand() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            modsman.upgradeMods(projectIds).collectPrintingFailures { (_, new) ->
                println("Upgraded '${new.projectName}' to '${new.fileName}'")
            }
        }
        return 0
    }
}

@Parameters(commandNames = ["reinstall"], commandDescription = "force download one or more mods")
internal object ReinstallCommand : ProjectsCommand() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            modsman.reinstallMods(projectIds).collectPrintingFailures {
                println("Downloaded '${it.projectName}' as '${it.fileName}'")
            }
        }
        return 0
    }
}

@Parameters(commandNames = ["upgrade-all"], commandDescription = "update all mods in the mod list")
internal object UpgradeAllCommand : AllProjectsCommand(UpgradeCommand)

@Parameters(commandNames = ["remove-all"], commandDescription = "delete all mods in the mod list")
internal object RemoveAllCommand : AllProjectsCommand(RemoveCommand)

@Parameters(commandNames = ["reinstall-all"], commandDescription = "force download all mods in the mod list")
internal object ReinstallAllCommand : AllProjectsCommand(ReinstallCommand)

@Parameters(commandNames = ["discover"], commandDescription = "add existing jars to the mod list")
internal object DiscoverCommand : CommandBase() {
    @Parameter(required = true, description = "JAR [JAR ...]")
    lateinit var jarNames: List<String>

    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            val jarPaths = jarNames.map { Paths.get(it) }
            modsman.matchMods(jarPaths).collectPrintingFailures {
                println("Matched '${it.projectName}' to '${it.fileName}'")
            }
        }
        return 0
    }
}

@Parameters(commandNames = ["list"], commandDescription = "print the mod list to stdout")
internal object ListCommand : CommandBase() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().modlist.mods.forEach {
            println("${it.projectId}: '${it.projectName}' as '${it.fileName}'")
        }
        return 0
    }
}

@Parameters(commandNames = ["list-outdated"], commandDescription = "list the mods that can be upgraded")
internal object ListOutdatedCommand : CommandBase() {
    @FlowPreview
    override suspend fun run(jc: JCommander): Int {
        RootCommand.createModsman().use { modsman ->
            modsman.getOutdatedMods().collectPrintingFailures { (mod, newFileName) ->
                println("${mod.projectId}: '${mod.projectName}' can be updated to '$newFileName'")
            }
        }
        return 0
    }
}

@FlowPreview
suspend fun <T> Flow<Result<T>>.collectPrintingFailures(action: suspend (value: T) -> Unit) {
    collect { result ->
        result
            .onSuccess { action(it) }
            .onFailure { println("${it::class.java.simpleName}: ${it.message}") }
    }
}

@FlowPreview
fun main(args: Array<String>) {
    val jc = JCommander.newBuilder()
        .addObject(RootCommand)
        .addCommand(InitCommand)
        .addCommand(AddCommand)
        .addCommand(RemoveCommand)
        .addCommand(UpgradeCommand)
        .addCommand(ReinstallCommand)
        .addCommand(UpgradeAllCommand)
        .addCommand(RemoveAllCommand)
        .addCommand(ReinstallAllCommand)
        .addCommand(DiscoverCommand)
        .addCommand(ListCommand)
        .addCommand(ListOutdatedCommand)
        .build()
    jc.programName = "modsman"

    try {
        jc.parse(*args)
    } catch (e: ParameterException) {
        if (!RootCommand.help) {
            JCommander.getConsole().println(e.message)
            exitProcess(1)
        }
    }

    if (RootCommand.help) {
        jc.parsedCommand?.let { jc.usage(it) } ?: jc.usage()
        exitProcess(0)
    }

    val command = jc.commands[jc.parsedCommand]?.objects?.get(0) as CommandBase? ?: RootCommand
    exitProcess(runBlocking { command.run(jc) })
}
