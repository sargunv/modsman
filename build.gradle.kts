import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31" apply false
    idea
    id("com.palantir.git-version") version "0.12.3"
    id("org.beryx.jlink") version "2.10.2" apply false
}

val gitVersion: groovy.lang.Closure<Any> by extra

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    group = "modsman"
    version = gitVersion()
}

configure<IdeaModel> {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
