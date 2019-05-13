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
    launcher{
        name = "Modsman"
    }
}

application {
    mainClassName = "dev.sargunv.modsman.app.MainKt"
}

//val jar = tasks.getByName<Jar>("jar") {
//    manifest {
//        attributes(
//            "Main-Class" to application.mainClassName,
//            "Class-Path" to configurations.runtime.get().joinToString(" ") { it.name }
//        )
//    }
//}
//
//val libsDir = "${property("libsDir")}"
//
//val build by tasks.named("build")
//
//val copyDependencies by tasks.registering(Copy::class) {
//    dependsOn("build")
//    destinationDir = File(libsDir)
//    from(configurations.runtime.get())
//}
//
//val jpackager by tasks.registering(Exec::class) {
//    dependsOn("copyDependencies")
//    val os = System.getProperty("os.name").toLowerCase()
//    val nativeType = when {
//        os.contains("windows") -> "msi"
//        os.contains("mac") -> "dmg"
//        os.contains("linux") -> "deb"
//        else -> throw RuntimeException("Unsupported os: $os")
//    }
//    val dependencies = configurations.runtime.get().flatMap { listOf("-srcfiles", it.name) }
//    workingDir = projectDir
//    commandLine = listOf(
//        "jpackager",
//        "-deploy",
//        "-nosign",
//        "-native", nativeType,
//        "-outdir", "$buildDir/distribution",
//        "-outfile", project.name,
//        "-name", "Modsman",
//        "-appclass", application.mainClassName,
//        "-srcdir", libsDir,
//        "-srcmodulfiles", jar.archiveFileName,
//        "-Bruntime="
//    ) + dependencies
//}
