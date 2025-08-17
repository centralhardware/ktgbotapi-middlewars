plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.centralhardware"
            artifactId = "ktgbotapi-stdout-logging-middleware"
            version = "1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}
