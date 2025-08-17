subprojects {
    // Ensure repositories available for all submodules (safe even if modules define their own)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    // Provide a common dependency on ktgbotapi only when Kotlin JVM plugin is applied
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            val ktgbotapiVersion: String = project.findProperty("ktgbotapiVersion") as? String
                ?: error("ktgbotapiVersion is not defined in gradle.properties")
            add("implementation", "dev.inmo:tgbotapi:$ktgbotapiVersion")
        }
    }
}
