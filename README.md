# ktgbotapi-middlewars

A set of middlewares for [dev.inmo/tgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) that simplify logging and restricting access to your Telegram bot. The repository contains several independent modules that can be used separately.

Modules:
- ktgbotapi-restrict-access-middleware — restrict access to the bot by user IDs.
- ktgbotapi-stdout-logging-middleware — log incoming updates to stdout.
- ktgbotapi-clickhouse-logging-middleware — log bot requests/responses to ClickHouse.

## Installation

Artifacts are published via JitPack. Add the JitPack repository and the dependencies you need.

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Restrict access
    implementation("me.centralhardware.telegram:ktgbotapi-restrict-access-middleware:1.0-SNAPSHOT")

    // Logging to stdout
    implementation("me.centralhardware:ktgbotapi-stdout-logging-middleware:1.0-SNAPSHOT")

    // Logging to ClickHouse
    implementation("me.centralhardware:ktgbotapi-clickhouse-logging-middleware:1.0-SNAPSHOT")
}
```

Note: if you use a specific tag/commit on JitPack, specify the corresponding version. Artifact coordinates are taken from Gradle publications (see groupId/artifactId above).

## Usage

All examples below assume you are using tgbotapi with middleware pipelines. Basic tgbotapi setup with middleware pipelines is assumed.

### 1) Restrict access

Core parts:
- `UserAccessChecker` — an interface for access checking.
- `EnvironmentVariableUserAccessChecker` — implementation that reads allowed IDs from the `ALLOWED_USERS` environment variable.
- `restrictAccess(accessChecker: UserAccessChecker)` — an extension to enable the middleware.

Environment variables:
- `ALLOWED_USERS` — a comma-separated list of user IDs (Long). Example: `ALLOWED_USERS=123456789,987654321`.

Example:

```kotlin
val bot = telegramBot(token)
bot.buildBehaviourWithLongPolling(scope) {
    includeMiddlewares {
        restrictAccess(EnvironmentVariableUserAccessChecker())
    }
    // ... your behaviour
}
```

Behavior:
- For each incoming `Update`, the middleware attempts to extract the sender's user ID.
- If the ID is not in the allowed list, the update is replaced with `UnknownUpdate` containing `AccessDeniedException()`, preventing further processing.

### 2) Logging to stdout

Core parts:
- `stdoutLogging()` — an extension that adds middleware formatting and printing incoming updates via KSLog.

Example:

```kotlin
includeMiddlewares {
    stdoutLogging()
}
```

Most update types are logged. For each, a human-readable message like "<who> - <what>" is produced.

### 3) Logging to ClickHouse

Core parts:
- `clickhouseLogging(appName: String)` — an extension that logs outgoing requests and incoming responses.

Environment variables:
- `CLICKHOUSE_URL` — JDBC URL for ClickHouse, e.g. `jdbc:ch://clickhouse-host:8123/default`.
- `HOST` — optional, host name for logs; if not set, the system host name is used.

Expected table schema: `bot_log.bot_log` with columns:
- `date_time` DateTime
- `appName` String
- `type` String — "IN" or "OUT"
- `data` String — JSON
- `className` String
- `host` String

Example:

```kotlin
includeMiddlewares {
    clickhouseLogging(appName = "my-telegram-bot")
}
```

Notes:
- Requests of types `GetUpdates`, `DeleteWebhook`, `GetMe` are not logged.
- For `GetUpdates`, the result (list of updates) is logged item by item.
- Binary responses (`ByteArray`) are not stored.
