package modsman.packutil

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import modsman.BuildConfig
import modsman.Modsman
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger
import kotlin.system.exitProcess

object Args {
    @Parameter(names = ["--minecraft-root", "-M"], required = true, order = 0)
    lateinit var minecraftRoot: String

    @Parameter(names = ["--invoke-in", "-i"], required = true, order = 1)
    lateinit var targetDirs: List<String>

    @Parameter(names = ["--force", "-f"], order = 2)
    var force: Boolean = false

    @Parameter(names = ["--version", "-v"], help = true, order = 3)
    var version: Boolean = false
}

@FlowPreview
fun main(args: Array<String>) {
    val jc = JCommander.newBuilder().addObject(Args).build()
    jc.programName = "modsman-packutil"

    try {
        jc.parse(*args)
    } catch (e: ParameterException) {
        JCommander.getConsole().println(e.message)
        exitProcess(1)
    }

    if (Args.version) {
        JCommander.getConsole().println(BuildConfig.VERSION)
        exitProcess(0)
    }

    System.setProperty(
        "java.util.logging.SimpleFormatter.format",
        "[%1\$tF %1\$tT] [%4\$s] %5\$s %n"
    )
    val log = Logger.getGlobal()

    runBlocking {
        val mcPath = Paths.get(Args.minecraftRoot)

        for (target in Args.targetDirs) {
            val targetPath = mcPath.resolve(target)
            val markerPath = targetPath.resolve(".MODSMAN_FIRST_RUN_COMPLETE")

            if (!Args.force && Files.exists(markerPath)) {
                log.info("Skipping $targetPath ...")
                continue
            }

            log.info("Invoking Modsman in $targetPath ...")
            Modsman(targetPath, 10).use { modsman ->
                val projectIds = modsman.modlist.mods.map { it.projectId }
                modsman.reinstallMods(projectIds).collect { result ->
                    result
                        .onSuccess { log.info("Downloaded '${it.projectName}' as '${it.fileName}'") }
                        .onFailure { log.severe("${it::class.java.simpleName}: ${it.message}") }
                }
            }

            log.info("Creating marker $markerPath ...")
            withContext(Dispatchers.IO) {
                try {
                    Files.createFile(markerPath)
                } catch (ignored: java.nio.file.FileAlreadyExistsException) {
                }
            }
        }
    }

    log.info("Done")
    exitProcess(0)
}
