plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.inmo:tgbotapi:28.0.0")
    implementation("dev.inmo:kslog:1.5.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.centralhardware.telegram"
            artifactId = "ktgbotapi-restrict-access-middleware"
            version = "1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}
