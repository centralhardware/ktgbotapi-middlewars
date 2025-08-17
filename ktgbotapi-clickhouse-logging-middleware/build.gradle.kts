plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
}

val clickhouseVersion = "0.9.1"

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.clickhouse:clickhouse-jdbc:$clickhouseVersion")
    implementation("com.clickhouse:clickhouse-http-client:$clickhouseVersion")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.github.seratch:kotliquery:1.9.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.centralhardware"
            artifactId = "ktgbotapi-clickhouse-logging-middleware"
            version = "1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}