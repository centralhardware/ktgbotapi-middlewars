subprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            val ktgbotapiVersion: String = project.findProperty("ktgbotapiVersion") as? String
                ?: error("ktgbotapiVersion is not defined in gradle.properties")
            add("implementation", "dev.inmo:tgbotapi:$ktgbotapiVersion")
        }
    }
}
