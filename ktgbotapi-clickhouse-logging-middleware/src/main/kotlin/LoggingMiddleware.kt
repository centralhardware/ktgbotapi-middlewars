package me.centralhardware.telegram

import com.clickhouse.jdbc.ClickHouseDataSource
import com.clickhouse.jdbc.DataSourceImpl
import com.google.gson.Gson
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.requests.webhook.DeleteWebhook
import java.net.InetAddress
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.ArrayList
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import kotliquery.sessionOf
import java.util.Properties

/**
 * List of request types that should be excluded from logging
 */
private val EXCLUDED_REQUEST_TYPES = setOf(
    GetUpdates::class,
    DeleteWebhook::class,
    GetMe::class
)

/**
 * SQL query for inserting log entries
 */
private const val INSERT_QUERY = """
    INSERT INTO bot_log.bot_log
    (
        date_time,
        appName,
        type,
        data,
        className,
        host
    )
    VALUES
    (
        :date_time,
        :appName,
        :type,
        :data,
        :className,
        :host
    )
"""

/**
 * Initialize ClickHouse data source
 */
private val dataSource: DataSource by lazy {
    try {
        DataSourceImpl(System.getenv("CLICKHOUSE_URL") ?:
            throw IllegalStateException("CLICKHOUSE_URL environment variable is not set"), Properties())
    } catch (e: SQLException) {
        KSLog.info("Failed to initialize ClickHouse data source: ${e.message}")
        throw RuntimeException("Failed to initialize ClickHouse data source", e)
    }
}

/**
 * Save log entry to ClickHouse
 */
private fun save(data: String, clazz: KClass<*>, income: Boolean, appName: String) {
    sessionOf(dataSource)
        .execute(
            queryOf(
                INSERT_QUERY,
                mapOf(
                    "date_time" to LocalDateTime.now(),
                    "appName" to appName,
                    "type" to if (income) "IN" else "OUT",
                    "data" to data,
                    "className" to clazz.simpleName,
                    "host" to (System.getenv("HOST") ?: InetAddress.getLocalHost().hostName),
                ),
            )
        )
}

/**
 * Get serializer for the given data object
 */
fun <T : Any> getSerializer(data: T): SerializationStrategy<T> {
    val property = data::class.declaredMemberProperties.find { it.name == "requestSerializer" }
        ?: throw IllegalArgumentException("No requestSerializer property found in ${data::class.simpleName}")

    @Suppress("UNCHECKED_CAST")
    return (property.call(data)) as SerializationStrategy<T>
}

/**
 * Extension function to add ClickHouse logging middleware to the Telegram bot
 * 
 * @param appName The name of the application to be used in logs
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.clickhouseLogging(appName: String) {
    addMiddleware {
        val gson = Gson()
        val nonstrictJsonFormat = Json {
            isLenient = true
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = true
            encodeDefaults = true
        }

        doOnRequestReturnResult { result, request, _ ->
            // Log outgoing request if it's not in excluded types
            if (request::class !in EXCLUDED_REQUEST_TYPES) {
                runCatching {
                    save(
                        nonstrictJsonFormat
                            .encodeToJsonElement(getSerializer(request), request)
                            .toString(),
                        request::class,
                        false,
                        appName,
                    )
                }.onFailure { 
                    KSLog.info("Failed to save request ${request::class.simpleName}: ${it.message}")
                }
            }

            // Early return if result is null
            val resultValue = result.getOrNull() ?: return@doOnRequestReturnResult null

            // Log incoming response
            runCatching {
                when {
                    // Handle GetUpdates response specially (it's an array)
                    request is GetUpdates -> {
                        (resultValue as ArrayList<Any>).forEach {
                            save(gson.toJson(it), it::class, true, appName)
                        }
                    }
                    // Log other responses if they're not from excluded types and not binary data
                    request::class !in EXCLUDED_REQUEST_TYPES && resultValue !is ByteArray -> {
                        save(gson.toJson(result), resultValue::class, true, appName)
                    }
                }
            }.onFailure {
                KSLog.info("Failed to save response for ${request::class.simpleName}: ${it.message}")
            }

            null
        }
    }
    KSLog.info("Initialized ClickHouse logging middleware")
}
