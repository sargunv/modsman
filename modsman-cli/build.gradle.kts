plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":modsman-core"))
    implementation(group = "com.beust", name = "jcommander", version = "1.71")
}

application {
    mainClassName = "modsman.cli.MainKt"
}
