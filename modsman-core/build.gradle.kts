import com.palantir.gradle.gitversion.VersionDetails

plugins {
    kotlin("jvm")
    `maven-publish`
    id("de.fuerstenau.buildconfig") version "1.1.8"
    id("com.palantir.git-version")
}

val versionDetails: groovy.lang.Closure<VersionDetails> by extra

dependencies {
    api(kotlin("stdlib-jdk8", "1.3.31"))
    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.2.1")
    implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.5.0")
    implementation(group = "com.squareup.retrofit2", name = "converter-gson", version = "2.5.0")
    implementation(group = "com.jakewharton.retrofit", name = "retrofit2-kotlin-coroutines-adapter", version = "0.9.2")
    implementation(group = "com.sangupta", name = "murmur", version = "1.0.0")
}

sourceSets {
    getByName("main") {
        java.srcDir("build/gen/buildconfig/src/main")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar.get()) {
                builtBy(sourcesJar)
            }
        }
    }

    repositories {
        mavenLocal()
        if (versionDetails().isCleanTag) {
            // TODO add prod maven repo here
        }
    }
}
