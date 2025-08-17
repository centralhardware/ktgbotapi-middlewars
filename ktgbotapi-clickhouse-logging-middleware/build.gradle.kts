
val clickhouseVersion: String by rootProject.extra

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.clickhouse:clickhouse-jdbc:$clickhouseVersion")
    implementation("com.clickhouse:clickhouse-http-client:$clickhouseVersion")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Explicit dependencies used in source code
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("dev.inmo:kslog:1.5.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.centralhardware.telegram"
            artifactId = "ktgbotapi-clickhouse-logging-middleware"
            version = "1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}