import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("org.beryx.jlink")
}

dependencies {
    api(project(":modsman-core"))
    api(group = "com.beust", name = "jcommander", version = "1.71")
}

val compileJava = tasks.getByName<JavaCompile>("compileJava") {
    doFirst {
        options.compilerArgs = listOf("--module-path", classpath.asPath)
        classpath = files()
    }
}

val compileKotlin = tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
    destinationDir = compileJava.destinationDir
}

val jar = tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

jlink {
    launcher {
        name = "modsman"
    }
}

application {
    mainClassName = "dev.sargunv.modsman.cli.MainKt"
}
