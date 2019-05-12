plugins {
    kotlin("jvm") version "1.3.31"
    application
}

dependencies {
    api(project(":common"))
}

application {
    mainClassName = "dev.sargunv.modsman.app.MainKt"
}
