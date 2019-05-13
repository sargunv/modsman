package dev.sargunv.modsman.app

import dev.sargunv.modsman.BuildConfig
import dev.sargunv.modsman.common.Modlist
import dev.sargunv.modsman.common.ModlistManager
import dev.sargunv.modsman.common.Modsman
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TextInputDialog
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.system.exitProcess


class MainApp : Application() {
    private val title = "modsman ${BuildConfig.VERSION}"

    private fun chooseDirectory(stage: Stage): Path {
        val chooser = DirectoryChooser()
        chooser.title = title
        return chooser.showDialog(stage)?.toPath() ?: exitProcess(0)
    }

    private fun chooseGameVersion(): String {
        val dialog = TextInputDialog("1.14.1")
        dialog.headerText = "Mod list not found. Enter a game version to initialize a new mod list."
        dialog.title = title
        return dialog.showAndWait().orElse(null) ?: exitProcess(0)
    }

    private fun createModsman(stage: Stage): Modsman {
        val path = chooseDirectory(stage)
        if (!Files.exists(path.resolve(Modlist.fileName))) {
            ModlistManager.init(path, chooseGameVersion()).close()
        }
        return Modsman(path, 10)
    }

    override fun start(stage: Stage) {
        stage.setOnCloseRequest { exitProcess(0) }
        stage.title = title
//        primaryStage.icons.add(Image(javaClass.getResourceAsStream("icon.png")))

        val fxmlLoader = FXMLLoader(
            javaClass.getResource("Main.fxml"),
            ResourceBundle.getBundle("dev/sargunv/modsman/app/bundle")
        )

        val scene = Scene(fxmlLoader.load())
        stage.scene = scene
        stage.show()

        val controller = fxmlLoader.getController<MainController>()
        controller.modsman = createModsman(stage)
    }
}
