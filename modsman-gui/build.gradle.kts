import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val modsmanVersion: String by project

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

enum class Os {
    WINDOWS, MACOS, LINUX
}

jlink {
    launcher {
        name = "modsman-gui"
    }

    jpackage {
        val os = System.getProperty("os.name").toLowerCase().let { osName ->
            when {
                osName.contains("windows") -> Os.WINDOWS
                osName.contains("mac") -> Os.MACOS
                osName.contains("linux") -> Os.LINUX
                else -> throw RuntimeException("Unsupported os: $osName")
            }
        }

        installerType = when (os) {
            Os.WINDOWS -> "msi"
            Os.MACOS -> "dmg"
            Os.LINUX -> "deb"
        }

        if (os == Os.MACOS)
            skipInstaller = true

        val icon = when (os) {
            Os.WINDOWS -> "icons/windows.ico"
            Os.MACOS -> "icons/macos.icns"
            Os.LINUX -> "icons/linux.png"
        }.let { iconPath ->
            projectDir.toPath()
                .resolve(iconPath)
                .toAbsolutePath()
        }

        val appVersion = modsmanVersion.substringBefore("-")

        val extraInstallerOptions = when (os) {
            Os.WINDOWS -> listOf(
                "--win-menu",
                "--win-shortcut",
                "--win-upgrade-uuid", "5dec2353-b238-4172-b00f-43c98b40bf08"
            )
            Os.MACOS -> emptyList()
            Os.LINUX -> listOf("--linux-bundle-name", "modsman-gui")
        }

        imageOptions = listOf(
            "--icon", "$icon"
        )

        installerOptions = listOf("--app-version", appVersion) + extraInstallerOptions
    }
}

application {
    mainClassName = "modsman.gui/modsman.gui.MainKt"
}
