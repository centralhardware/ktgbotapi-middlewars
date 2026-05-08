package me.centralhardware.telegram.middleware.testbot

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.coroutines.runBlocking
import me.centralhardware.telegram.middleware.clickHouseExceptionsHandler
import me.centralhardware.telegram.middleware.clickHouseLogging
import me.centralhardware.telegram.middleware.clickHouseRequestIdContext

/**
 * Required env vars: TELEGRAM_TOKEN, CLICKHOUSE_JDBC_URL.
 * Apply migration once: ktgbotapi-clickhouse-logging-middleware/db/migration/V1__create_bot_requests.sql
 */
@OptIn(Warning::class)
fun main(): Unit = runBlocking {
    val token = System.getenv("TELEGRAM_TOKEN")
        ?: error("TELEGRAM_TOKEN env var is not set")

    val (_, job) = telegramBotWithBehaviourAndLongPolling(
        token = token,
        builder = {
            includeMiddlewares {
                clickHouseLogging(botName = "test_bot")
            }
        },
        subcontextInitialAction = clickHouseRequestIdContext(),
        defaultExceptionsHandler = clickHouseExceptionsHandler(botName = "test_bot"),
    ) {
        onCommand("start") {
            reply(it, "Hi! Send me anything and I'll echo it. Each interaction is logged to ClickHouse.")
        }
        onCommand("ping") {
            reply(it, "pong")
        }
        onCommand("multi") {
            // Three independent API calls — all four resulting rows (1 incoming + 3 outgoing)
            // share one request_id thanks to clickHouseRequestIdContext.
            reply(it, "step 1/3")
            sendMessage(it.chat, "step 2/3 (separate call, same request_id)")
            sendMessage(it.chat, "step 3/3 (third call, same request_id)")
        }
        onCommand("server_fail") {
            deleteMessage(it.chat.id, MessageId(1L))
        }
        onCommand("local_fail") {
            throw RuntimeException("sdf")
        }
        onText { message ->
            if (message.content.text.startsWith("/")) return@onText
            reply(message, "echo: ${message.content.text}")
        }
    }
    job.join()
}
