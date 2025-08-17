rootProject.name = "ktgbotapi-middlewars"

gradle.rootProject {
    extra["clickhouseVersion"] = "0.9.1"
}

include("ktgbotapi-clickhouse-logging-middleware")
include("ktgbotapi-stdout-logging-middleware")
include("ktgbotapi-restrict-access-middleware")