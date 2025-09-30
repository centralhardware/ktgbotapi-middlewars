package me.centralhardware.telegram.middleware

import com.clickhouse.jdbc.DataSourceImpl
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.Properties
import javax.sql.DataSource

/**
 * Data class representing user interaction statistics
 */
private data class UserInteraction(
    val date: LocalDateTime,
    val userId: Long,
    val username: String,
    val firstName: String,
    val appName: String
)

/**
 * SQL query for inserting interaction statistics
 */
private const val INSERT_QUERY = """
    INSERT INTO bot_interactions
    (date, user_id, username, first_name,last_name, interactions, app_name)
    VALUES
    (:date, :user_id, :username, :first_name, :last_name, 1, :app_name)
"""

/**
 * Initialize ClickHouse data source
 */
private val dataSource: DataSource by lazy {
    try {
        DataSourceImpl(
            System.getenv("CLICKHOUSE_URL")
                ?: throw IllegalStateException("CLICKHOUSE_URL environment variable is not set"),
            Properties()
        )
    } catch (e: SQLException) {
        KSLog.info("Failed to initialize ClickHouse data source: ${e.message}")
        throw RuntimeException("Failed to initialize ClickHouse data source", e)
    }
}

private fun saveInteraction(date: LocalDateTime,
                            userId: Long,
                            username: String,
                            firstName: String,
                            lastName: String,
                            appName: String) {
    runCatching {
        sessionOf(dataSource).use { session ->
            session.execute(
                queryOf(
                    INSERT_QUERY,
                    mapOf(
                        "date" to date,
                        "user_id" to userId,
                        "username" to username,
                        "first_name" to firstName,
                        "last_name" to lastName,
                        "app_name" to appName
                    )
                )
            )
        }
    }.onFailure { e ->
        KSLog.info("Failed to save interaction for user ${userId}: ${e.message}")
    }
}

/**
 * Extract user information from an Update
 */
private fun Update.extractUserInfo(): User? {
    return when (this) {
        is dev.inmo.tgbotapi.types.update.MessageUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.EditMessageUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.CallbackQueryUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.InlineQueryUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.ChosenInlineResultUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.ShippingQueryUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.PreCheckoutQueryUpdate -> data.from
        is dev.inmo.tgbotapi.types.update.PollAnswerUpdate -> data.user
        is dev.inmo.tgbotapi.types.update.MyChatMemberUpdatedUpdate -> data.user
        is dev.inmo.tgbotapi.types.update.CommonChatMemberUpdatedUpdate -> data.user
        is dev.inmo.tgbotapi.types.update.ChatJoinRequestUpdate -> data.user
        else -> null
    }

}

/**
 * Extension function to add ClickHouse interaction statistics middleware to the Telegram bot
 *
 * @param appName The name of the application to be used in statistics
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.clickhouseLogging(appName: String) {
    addMiddleware {
        doOnRequestResultPresented { result, _, _, _ ->
            when (result) {
                is ArrayList<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as? ArrayList<Update>)?.forEach { update ->
                        runCatching {
                            update.extractUserInfo()?.let {
                                saveInteraction(
                                    LocalDateTime.now(),
                                    it.id.chatId.long,
                                    it.username?.username ?: "",
                                    it.firstName,
                                    it.lastName,
                                    appName
                                )
                            }
                        }.onFailure { e ->
                            KSLog.info("Failed to record interaction: ${e.message}")
                        }
                    }
                    result
                }
                else -> result
            }
        }
    }

    KSLog.info("Initialized ClickHouse statistics middleware for app: $appName")
}
