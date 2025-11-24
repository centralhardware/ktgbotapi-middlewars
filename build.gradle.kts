plugins {
    kotlin("jvm") version "2.2.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "me.centralhardware.telegram.middleware"
    version = "1.0-SNAPSHOT"

    dependencies {
        add("implementation", "dev.inmo:tgbotapi:30.0.2")
        add("implementation", "dev.inmo:kslog:1.5.0")
    }

    // Centralized publishing configuration for all subprojects
    extensions.configure<PublishingExtension>(
    ) {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifactId = project.name
            }
        }
    }
}