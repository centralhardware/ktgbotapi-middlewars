package me.centralhardware.telegram.middleware.testbot

import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
        setMyCommands(
            BotCommand("start", "Show greeting"),
            BotCommand("ping", "Health check"),
            BotCommand("multi", "Three calls sharing one request_id"),
            BotCommand("dialog", "Two-step wait chain (name + age)"),
            BotCommand("server_fail", "Trigger a Telegram API error"),
            BotCommand("local_fail", "Trigger a local exception"),
        )
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
        onCommand("dialog") {
            // Two-step wait chain — verifies that incoming-update rows captured by
            // `wait*` calls reuse the originating handler's update_id (so all six rows
            // — 1 incoming /dialog + prompt + name reply + ack + age prompt-reply +
            // final ack — share one request_id in ClickHouse).
            reply(it, "What's your name?")
            val name = waitTextMessage().filter { msg -> msg.chat.id == it.chat.id }.first()
            reply(name, "Nice to meet you, ${name.content.text}. How old are you?")
            val age = waitTextMessage().filter { msg -> msg.chat.id == it.chat.id }.first()
            reply(age, "Got it: ${name.content.text}, ${age.content.text}.")
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
