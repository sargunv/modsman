import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
}

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    group = "dev.sargunv.modsman"
    version = "1.0-SNAPSHOT"
}

configure<IdeaModel> {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
}