import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    application
}

dependencies {
    api(project(":common"))
    api(group = "com.beust", name = "jcommander", version = "1.71")
}

application {
    mainClassName = "dev.sargunv.modsman.cli.MainKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
