package me.centralhardware.telegram

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.extensions.utils.asContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.update.*
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update

/**
 * A middleware for logging Telegram Bot API updates to stdout.
 * This library provides extension functions to format and log various Telegram update types.
 */

/**
 * Formats a User object to a readable string.
 *
 * @return A string in the format "userId(firstName lastName)" or empty string if the user is null
 */
fun User?.format(): String = this?.let {
    "${id.chatId.long}($firstName $lastName)"
} ?: ""

/**
 * Formats a Chat object to a readable string.
 *
 * @return A string containing the chat ID or empty string if the chat is null
 */
fun Chat?.format(): String = this?.let {
    "${id.chatId.long}"
} ?: ""

/**
 * Formats a ContentMessage to a readable string based on its content type.
 *
 * @return A string representation of the message content or null if the content type is unknown
 */
fun ContentMessage<MessageContent>.format(): String? = when (val messageContent = content) {
    is ContactContent -> "${messageContent.contact}"
    is DiceContent -> "${messageContent.dice}"
    is GameContent -> "${messageContent.game}"
    is GiveawayContent -> "$messageContent"
    is GiveawayPublicResultsContent -> "${messageContent.giveaway}"
    is InvoiceContent -> "${messageContent.invoice}"
    is LiveLocationContent -> "${messageContent.location}"
    is StaticLocationContent -> "${messageContent.location}"
    is PhotoContent -> "$messageContent"
    is AnimationContent -> "$messageContent"
    is VideoContent -> "$messageContent"
    is StickerContent -> "${messageContent.media}"
    is MediaGroupContent<*> -> "$messageContent"
    is AudioContent -> "$messageContent"
    is DocumentContent -> "$messageContent"
    is VoiceContent -> "$messageContent"
    is VideoNoteContent -> "${messageContent.media}"
    is PaidMediaInfoContent -> "$messageContent"
    is PollContent -> "${messageContent.poll}"
    is StoryContent -> "${messageContent.story}"
    is TextContent -> "$messageContent"
    is VenueContent -> "${messageContent.venue}"
    else -> {
        KSLog.info("Unknown content type: ${messageContent?.javaClass?.simpleName}")
        null
    }
}

/**
 * Formats an Update object to a readable string based on its type.
 *
 * @return A string representation of the update or null if the update type is unknown
 */
fun Update.format(): String? {
    var from = ""
    var text = ""

    when (this) {
        is EditMessageUpdate -> {
            from = data.from.format()
            text = "Edit: " + data.asContentMessage()?.format()
        }
        is MessageUpdate -> {
            from = data.from.format()
            text = "Receive: " + data.asContentMessage()?.format()
        }
        is InlineQueryUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is CallbackQueryUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is EditChannelPostUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is ChannelPostUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is ChosenInlineResultUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is ShippingQueryUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is PreCheckoutQueryUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is PollUpdate -> {
            text = data.toString()
        }
        is PollAnswerUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is MyChatMemberUpdatedUpdate -> {
            from = data.user.format()
            text = data.toString()
        }
        is CommonChatMemberUpdatedUpdate -> {
            from = data.user.format()
            text = data.toString()
        }
        is ChatJoinRequestUpdate -> {
            from = data.user.format()
            text = data.toString()
        }
        is ChatMessageReactionUpdatedUpdate -> {
            from = data.chat.format()
            text = data.toString()
        }
        is ChatMessageReactionsCountUpdatedUpdate -> {
            from = data.chat.format()
            text = data.toString()
        }
        is ChatBoostUpdatedUpdate -> {
            from = data.chat.format()
            text = data.toString()
        }
        is ChatBoostRemovedUpdate -> {
            from = data.chat.format()
            text = data.toString()
        }
        is BusinessConnectionUpdate -> {
            from = data.user.format()
            text = data.toString()
        }
        is BusinessMessageUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is EditBusinessMessageUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is DeletedBusinessMessageUpdate -> {
            from = data.chat.format()
            text = data.toString()
        }
        is PaidMediaPurchasedUpdate -> {
            from = data.from.format()
            text = data.toString()
        }
        is UnknownUpdate -> {}
        else -> return null
    }

    return "$from - $text"
}

/**
 * Extension function for TelegramBotMiddlewaresPipelinesHandler.Builder that adds
 * a middleware for logging all updates to stdout.
 *
 * Usage:
 * ```
 * telegramBotWithBehaviourAndLongPolling(token, scope) {
 *     includeMiddlewares {
 *         stdoutLogging()
 *     }
 * }
 * ```
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.stdoutLogging() {
    addMiddleware {
        doOnRequestReturnResult { result, _, _ ->
            kotlin
                .runCatching {
                    if (result.getOrNull() !is ArrayList<*>) return@doOnRequestReturnResult null

                    @Suppress("UNCHECKED_CAST")
                    (result.getOrNull() as ArrayList<Update>).forEach { update ->
                        update.format()?.let { message -> KSLog.info(message) }
                    }
                }
                .onFailure { error -> KSLog.error("Failed to log request", error) }
            null
        }
    }
    KSLog.info("Initialized stdout logging middleware")
}
