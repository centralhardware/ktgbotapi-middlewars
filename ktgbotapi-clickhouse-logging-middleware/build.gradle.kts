plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("dev.inmo:tgbotapi:33.1.0")
    implementation("dev.inmo:kslog:1.6.0")
    implementation("com.clickhouse:clickhouse-jdbc:0.8.6")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = project.name
        }
    }
}
