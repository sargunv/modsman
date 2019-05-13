import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("org.openjfx.javafxplugin").version("0.0.7")
}

dependencies {
    api(project(":common"))
}

application {
    mainClassName = "dev.sargunv.modsman.app.MainKt"
}

javafx {
    version = "12.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
