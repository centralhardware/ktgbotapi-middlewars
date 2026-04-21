plugins {
    kotlin("jvm") version "2.3.20" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    group = "me.centralhardware.telegram.middleware"
    version = "1.0-SNAPSHOT"
}
