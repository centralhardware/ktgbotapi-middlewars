plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":ktgbotapi-clickhouse-logging-middleware"))
    implementation("dev.inmo:tgbotapi:33.1.0")
    implementation("dev.inmo:kslog:1.6.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.12")
}

application {
    mainClass.set("me.centralhardware.telegram.middleware.testbot.MainKt")
}
