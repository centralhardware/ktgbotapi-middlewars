package me.centralhardware.telegram.middleware

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.CustomBehaviourContextAndTypeReceiver
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

private const val PENDING_TTL_MS = 60_000L
private const val MAX_PENDING = 10_000

private val pendingRequestIds = ConcurrentHashMap<Long, String>()
private val jobToRequestId = ConcurrentHashMap<Job, String>()
private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun rememberPending(updateId: Long, requestId: String) {
    if (pendingRequestIds.size >= MAX_PENDING) {
        pendingRequestIds.keys.firstOrNull()?.let { pendingRequestIds.remove(it) }
    }
    pendingRequestIds[updateId] = requestId
    cleanupScope.launch {
        delay(PENDING_TTL_MS)
        pendingRequestIds.remove(updateId, requestId)
    }
}

private fun Job.containsDescendant(target: Job): Boolean {
    if (this === target) return true
    for (child in children) {
        if (child.containsDescendant(target)) return true
    }
    return false
}

private fun currentRequestId(currentJob: Job?): String {
    if (currentJob == null) return ""
    for ((parent, requestId) in jobToRequestId) {
        if (parent.containsDescendant(currentJob)) return requestId
    }
    return ""
}

private fun loadDriver() {
    runCatching { Class.forName("com.clickhouse.jdbc.ClickHouseDriver") }
}

private fun writeRow(
    jdbcUrl: String,
    botName: String,
    requestId: String,
    method: String,
    request: String,
    response: String,
    success: Boolean,
    error: String,
    durationMs: Long,
) {
    runCatching {
        DriverManager.getConnection(jdbcUrl).use { conn ->
            conn.prepareStatement(
                "INSERT INTO bot_requests (timestamp, bot, request_id, method, request, response, success, error, duration_ms) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            ).use { ps ->
                ps.setTimestamp(1, Timestamp.from(Instant.now()))
                ps.setString(2, botName)
                ps.setString(3, requestId)
                ps.setString(4, method)
                ps.setString(5, request)
                ps.setString(6, response)
                ps.setBoolean(7, success)
                ps.setString(8, error)
                ps.setLong(9, durationMs)
                ps.executeUpdate()
            }
        }
    }.onFailure { KSLog.error("ClickHouse insert failed", it) }
}

/**
 * Install via `telegramBotWithBehaviourAndLongPolling(subcontextInitialAction = ...)` to auto-tag
 * every Telegram API call performed while handling an incoming `Update` with that update's
 * `request_id`, so handler code doesn't need to know about it.
 */
fun clickHouseRequestIdContext(): CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> =
    { update: Update ->
        // get(), not remove(): one Update can match several triggers (e.g. onCommand + onText) and
        // each invokes subcontextInitialAction independently — they must all see the same id.
        val requestId = pendingRequestIds[update.updateId.long]
            ?: UUID.randomUUID().toString()
        val job = coroutineContext[Job]
        if (job != null) {
            jobToRequestId[job] = requestId
            job.invokeOnCompletion { jobToRequestId.remove(job) }
        }
    }

/**
 * Adds a middleware that logs each Telegram API call to ClickHouse (`bot_requests` table). One row
 * per call, written synchronously.
 *
 * `GetUpdates` itself is never logged; on success its result is decomposed into one row per
 * incoming `Update`. Pair with [clickHouseRequestIdContext] for incoming-to-outgoing correlation
 * and [clickHouseExceptionsHandler] for exceptions thrown before the HTTP layer.
 *
 * Schema lives in `src/main/resources/db/migration/V1__create_bot_requests.sql`.
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.clickHouseLogging(
    botName: String,
    jdbcUrl: String = System.getenv("CLICKHOUSE_JDBC_URL")
        ?: error("CLICKHOUSE_JDBC_URL env var is not set"),
) {
    loadDriver()
    val starts = ConcurrentHashMap<Request<*>, Long>()

    addMiddleware {
        doOnBeforeCallFactoryMakeCall { request, _ ->
            starts[request] = System.nanoTime()
        }
        doOnRequestReturnResult { result, request, _ ->
            val startedNs = starts.remove(request)
            val durationMs = startedNs?.let { (System.nanoTime() - it) / 1_000_000 } ?: 0L
            val response = result.getOrNull()

            when {
                request is GetUpdates -> {
                    if (response is List<*>) {
                        response.filterIsInstance<Update>().forEach { update ->
                            val requestId = UUID.randomUUID().toString()
                            rememberPending(update.updateId.long, requestId)
                            writeRow(
                                jdbcUrl,
                                botName = botName,
                                requestId = requestId,
                                method = update::class.simpleName.orEmpty(),
                                request = update.updateId.toString(),
                                response = update.toString(),
                                success = true,
                                error = "",
                                durationMs = 0L,
                            )
                        }
                    }
                }

                else -> {
                    val throwable = result.exceptionOrNull()
                    writeRow(
                        jdbcUrl,
                        botName = botName,
                        requestId = currentRequestId(coroutineContext[Job]),
                        method = request::class.simpleName.orEmpty(),
                        request = request.toString(),
                        response = response?.toString().orEmpty(),
                        success = result.isSuccess,
                        error = throwable?.toString().orEmpty(),
                        durationMs = durationMs,
                    )
                }
            }
            null
        }
    }
    KSLog.info("Initialized ClickHouse logging middleware for bot '$botName'")
}

/**
 * `defaultExceptionsHandler` that writes one row per exception thrown inside behaviour handlers.
 * Catches things the HTTP middleware can't see (e.g. local validation in request constructors).
 */
fun clickHouseExceptionsHandler(
    botName: String,
    jdbcUrl: String = System.getenv("CLICKHOUSE_JDBC_URL")
        ?: error("CLICKHOUSE_JDBC_URL env var is not set"),
): suspend (Throwable) -> Unit {
    loadDriver()
    return { throwable ->
        writeRow(
            jdbcUrl,
            botName = botName,
            requestId = currentRequestId(coroutineContext[Job]),
            method = throwable::class.simpleName.orEmpty(),
            request = "",
            response = "",
            success = false,
            error = throwable.stackTraceToString(),
            durationMs = 0L,
        )
    }
}
