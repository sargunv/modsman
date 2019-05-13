module modsman.gui {
    requires modsman.core;
    requires kotlin.stdlib;
    requires kotlin.stdlib.jdk8;
    requires kotlinx.coroutines.core;
    requires javafx.controls;
    requires javafx.fxml;

    exports dev.sargunv.modsman.gui;
}
