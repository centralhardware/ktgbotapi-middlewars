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

    // Set group and version centrally (with an override for a specific module)
    group = when (name) {
        "ktgbotapi-restrict-access-middleware" -> "me.centralhardware.telegram"
        else -> "me.centralhardware"
    }
    version = "1.0-SNAPSHOT"

    dependencies {
        add("implementation", "dev.inmo:tgbotapi:28.0.0")
        add("implementation", "dev.inmo:kslog:1.5.0")
    }

    // Centralized publishing configuration for all subprojects
    extensions.configure<org.gradle.api.publish.PublishingExtension>(
    ) {
        publications {
            create<org.gradle.api.publish.maven.MavenPublication>("maven") {
                from(components["java"])
                // artifactId defaults to project.name, which is desired here
                artifactId = project.name
            }
        }
    }
}