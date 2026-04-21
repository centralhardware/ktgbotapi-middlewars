plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("dev.inmo:tgbotapi:33.0.0")
    implementation("dev.inmo:kslog:1.6.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = project.name
        }
    }
}
