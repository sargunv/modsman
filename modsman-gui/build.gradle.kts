import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("org.openjfx.javafxplugin") version "0.0.7"
    id("org.beryx.jlink")
}

dependencies {
    implementation(project(":modsman-core"))
    runtime(project(":modsman-core"))
}

javafx {
    version = "12.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

val compileKotlin = tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

val jar = tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

jlink {
    launcher {
        name = "modsman-gui"
    }

    jpackage {
        val os = System.getProperty("os.name").toLowerCase()
        installerType = when {
            os.contains("windows") -> "msi"
            os.contains("mac") -> "dmg"
            os.contains("linux") -> "deb"
            else -> throw RuntimeException("Unsupported os: $os")
        }
        installerOptions = listOf("--win-console", "--win-menu", "--win-shortcut")
    }
}

application {
    mainClassName = "modsman.gui/modsman.gui.MainKt"
}
