plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
    implementation(project(":modsman-core"))
    implementation(group = "com.beust", name = "jcommander", version = "1.71")
}

application {
    mainClassName = "modsman.packutil.MainKt"
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
}
