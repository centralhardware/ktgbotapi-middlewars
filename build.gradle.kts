// build.gradle.kts (root)
plugins {
    kotlin("jvm") version "2.2.0" apply false
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

val ktgbotapiVersion = "28.0.0"

allprojects {
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    dependencies {
        add("implementation", "dev.inmo:tgbotapi:$ktgbotapiVersion")
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(24)
    }
}
