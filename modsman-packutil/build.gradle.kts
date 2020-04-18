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
        // defaults to project.name
//        archiveBaseName.set("${project.name}-fat")

        // defaults to all, so removing this overrides the normal, non-fat jar
        archiveClassifier.set("")
    }
}
