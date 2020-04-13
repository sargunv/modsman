import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val modsmanVersion: String by project

plugins {
    kotlin("jvm") version "1.3.31" apply false
    idea
    id("org.beryx.jlink") version "2.10.2" apply false
}

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    group = "modsman"
    version = modsmanVersion
}

configure<IdeaModel> {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
