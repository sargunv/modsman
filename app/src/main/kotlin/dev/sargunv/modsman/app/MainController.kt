package dev.sargunv.modsman.app

import dev.sargunv.modsman.common.ModEntry
import dev.sargunv.modsman.common.Modsman
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.file.Files
import java.util.*
import kotlin.streams.toList

class MainController : Initializable {
    var modsman: Modsman? = null
        set(value) {
            field = value
            refresh()
        }

    @FXML
    private lateinit var root: BorderPane

    @FXML
    private lateinit var path: Label

    @FXML
    private lateinit var version: Label

    @FXML
    private lateinit var tableView: TableView<ModEntry>

    @FXML
    private lateinit var idColumn: TableColumn<ModEntry, Int>

    @FXML
    private lateinit var nameColumn: TableColumn<ModEntry, String>

    @FXML
    private lateinit var fileColumn: TableColumn<ModEntry, String>

    @FXML
    private lateinit var status: Label

    @FXML
    @FlowPreview
    private fun onClickDiscover() {
        val modlist = modsman!!.modlist
        val installedJars = modlist.mods
            .map { modlist.modsPath.resolve(it.fileName).toAbsolutePath() }
            .toSet()
        val jars = Files.list(modlist.modsPath)
            .filter { Files.isRegularFile(it) && Files.isReadable(it) && it.toString().endsWith(".jar") }
            .map { it.toAbsolutePath() }
            .filter { it !in installedJars }
            .map { it.toString() }
            .toList()
        processWithStatus(
            getFlow = { matchMods(jars) },
            getStatus = { mod -> "Matched '${mod.projectName}' to '${mod.fileName}'" },
            countVerb = "Matched"
        )
    }

    @FXML
    @FlowPreview
    private fun onClickReinstall() {
        processProjects(
            getFlow = { ids -> reinstallMods(ids) },
            getStatus = { mod -> "Downloaded '${mod.projectName}' to '${mod.fileName}'" },
            countVerb = "Downloaded"
        )
    }

    @FXML
    @FlowPreview
    private fun onClickRemove() {
        processProjects(
            getFlow = { ids -> removeMods(ids) },
            getStatus = { mod -> "Deleted '${mod.fileName}'" },
            countVerb = "Deleted"
        )
    }

    @FXML
    @FlowPreview
    private fun onClickUpgrade() {
        processProjects(
            getFlow = { ids -> upgradeMods(ids) },
            getStatus = { (_, new) -> "Upgraded '${new.projectName}' to '${new.fileName}'" },
            countVerb = "Upgraded"
        )
    }

    @FlowPreview
    private fun <T> processProjects(getFlow: suspend Modsman.(List<Int>) -> Flow<T>, getStatus: (T) -> String, countVerb: String) {
        val projectIds = tableView.selectionModel.selectedItems.map { mod -> mod.projectId }
        processWithStatus({ getFlow(projectIds) }, getStatus, countVerb)
    }

    @FlowPreview
    private fun <T> processWithStatus(getFlow: suspend Modsman.() -> Flow<T>, getStatus: (T) -> String, countVerb: String) {
        root.isDisable = true
        GlobalScope.launch {
            modsman!!.use { modsman ->
                val count = modsman.getFlow()
                    .map { t ->
                        Platform.runLater {
                            status.text = getStatus(t)
                        }
                    }.count()
                Platform.runLater {
                    status.text = "$countVerb $count mods"
                    refresh()
                    root.isDisable = false
                }
            }
        }
    }

    @FXML
    private fun reloadModlist() {
        modsman?.let {
            modsman = Modsman(it.modlist.modsPath, it.numConcurrent)
        }
    }

    private fun refresh() {
        path.text = ""
        version.text = ""
        tableView.items.clear()

        modsman?.let {
            path.text = "${it.modlist.modsPath}"
            version.text = "MC ${it.modlist.config.gameVersion}"
            tableView.items.addAll(it.modlist.mods)
        }
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        status.text = ""
        tableView.selectionModel.selectionMode = SelectionMode.MULTIPLE
        idColumn.setCellValueFactory { ReadOnlyObjectWrapper(it.value.projectId) }
        nameColumn.setCellValueFactory { ReadOnlyObjectWrapper(it.value.projectName) }
        fileColumn.setCellValueFactory { ReadOnlyObjectWrapper(it.value.fileName) }
    }
}
