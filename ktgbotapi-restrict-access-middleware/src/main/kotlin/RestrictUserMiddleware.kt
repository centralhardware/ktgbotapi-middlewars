import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.serialization.json.JsonObject

/**
 * Adds a middleware to the bot that restricts user access.
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
            when (result) {
                !is ArrayList<*> -> result

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as? ArrayList<Update>)?.map { update ->
                        val userId = update.chatId()
                        if (userId == null) {
                            KSLog.info("Update type ${update::class.simpleName} does not support")
                            return@map update
                        }

                        if (accessChecker.checkAccess(userId)) {
                            update
                        } else {
                            KSLog.info(
                                "Filtering out ${update::class.simpleName} from unauthorized user $userId"
                            )
                            UnknownUpdate(update.updateId, JsonObject(mapOf()), AccessDeniedException())
                        }
                    } ?: result
                }
            }
        }
    }
    KSLog.info("Initialize restrict access middleware with access checker: ${accessChecker::class.simpleName}")
}