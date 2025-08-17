/**
 * Middleware for restricting access to the bot based on user IDs.
 */
package me.centralhardware.telegram.ktgbotapi.access.middleware

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.serialization.json.JsonObject
import me.centralhardware.telegram.ktgbotapi.access.checker.UserAccessChecker
import me.centralhardware.telegram.ktgbotapi.access.exception.AccessDeniedException
import me.centralhardware.telegram.ktgbotapi.access.util.chatId

/**
 * Adds a middleware to the bot that restricts access based on user IDs.
 *
 * This middleware intercepts all updates from Telegram and checks if the user who sent the update
 * is allowed to access the bot. If the user is not allowed, the update is replaced with an
 * [UnknownUpdate] containing an [AccessDeniedException], effectively preventing the bot from
 * processing the update.
 *
 * @param accessChecker The [UserAccessChecker] implementation to use for checking user access.
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.restrictAccess(
    accessChecker: UserAccessChecker
) {
    addMiddleware {
        doOnRequestResultPresented { result, _, _, _ ->
            when {
                result !is ArrayList<*> -> result
                result is ArrayList<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as? ArrayList<Update>)?.map { update ->
                        val userId = update.chatId()
                        val permitted = accessChecker.checkAccess(userId)
                        
                        if (permitted) {
                            update
                        } else {
                            KSLog.info(
                                "Filtering out ${update::class.simpleName} from unauthorized user $userId"
                            )
                            UnknownUpdate(update.updateId, JsonObject(mapOf()), AccessDeniedException())
                        }
                    } ?: result
                }
                else -> result
            }
        }
    }
    KSLog.info("Initialize restrict access middleware with access checker: ${accessChecker::class.simpleName}")
}