package dev.sargunv.modsman.app

import dev.sargunv.modsman.common.Modsman
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.util.*
import kotlin.system.exitProcess


class MainApp : Application() {
    override fun start(primaryStage: Stage) {
        val location = javaClass.getResource("Main.fxml")
        val resources = ResourceBundle.getBundle("dev/sargunv/modsman/app/bundle")
        val fxmlLoader = FXMLLoader(location, resources)
        val scene = Scene(fxmlLoader.load())
        val controller = fxmlLoader.getController<MainController>()
        controller.modsman = Modsman(DirectoryChooser().showDialog(primaryStage).toPath(), 10)

        primaryStage.setOnCloseRequest { exitProcess(0) }
        primaryStage.title = "modsman"
//        primaryStage.icons.add(Image(javaClass.getResourceAsStream("icon.png")))
        primaryStage.scene = scene
        primaryStage.show()
    }
}
