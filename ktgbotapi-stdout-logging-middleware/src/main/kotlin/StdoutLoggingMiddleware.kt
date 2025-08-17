import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewareBuilder
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.extensions.utils.asContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.update.*
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlin.collections.forEach
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.onFailure

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
    "${PrivateChat.id.chatId.long}(${PrivateChat.firstName} ${PrivateChat.lastName})"
} ?: ""

/**
 * Formats a Chat object to a readable string.
 *
 * @return A string containing the chat ID or empty string if the chat is null
 */
fun Chat?.format(): String = this?.let {
    "${Chat.id.chatId.long}"
} ?: ""

/**
 * Formats a ContentMessage to a readable string based on its content type.
 *
 * @return A string representation of the message content or null if the content type is unknown
 */
fun ContentMessage<MessageContent>.format(): String? = when (val messageContent = ContentMessage.content) {
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
            from = EditMessageUpdate.data.from.format()
            text = "Edit: " + EditMessageUpdate.data.asContentMessage()?.format()
        }
        is MessageUpdate -> {
            from = MessageUpdate.data.from.format()
            text = "Receive: " + MessageUpdate.data.asContentMessage()?.format()
        }
        is InlineQueryUpdate -> {
            from = InlineQueryUpdate.data.from.format()
            text = InlineQueryUpdate.data.toString()
        }
        is CallbackQueryUpdate -> {
            from = CallbackQueryUpdate.data.from.format()
            text = CallbackQueryUpdate.data.toString()
        }
        is EditChannelPostUpdate -> {
            from = EditChannelPostUpdate.data.from.format()
            text = EditChannelPostUpdate.data.toString()
        }
        is ChannelPostUpdate -> {
            from = ChannelPostUpdate.data.from.format()
            text = ChannelPostUpdate.data.toString()
        }
        is ChosenInlineResultUpdate -> {
            from = ChosenInlineResultUpdate.data.from.format()
            text = ChosenInlineResultUpdate.data.toString()
        }
        is ShippingQueryUpdate -> {
            from = ShippingQueryUpdate.data.from.format()
            text = ShippingQueryUpdate.data.toString()
        }
        is PreCheckoutQueryUpdate -> {
            from = PreCheckoutQueryUpdate.data.from.format()
            text = PreCheckoutQueryUpdate.data.toString()
        }
        is PollUpdate -> {
            text = PollUpdate.data.toString()
        }
        is PollAnswerUpdate -> {
            from = PollAnswerUpdate.data.from.format()
            text = PollAnswerUpdate.data.toString()
        }
        is MyChatMemberUpdatedUpdate -> {
            from = MyChatMemberUpdatedUpdate.data.user.format()
            text = MyChatMemberUpdatedUpdate.data.toString()
        }
        is CommonChatMemberUpdatedUpdate -> {
            from = CommonChatMemberUpdatedUpdate.data.user.format()
            text = CommonChatMemberUpdatedUpdate.data.toString()
        }
        is ChatJoinRequestUpdate -> {
            from = ChatJoinRequestUpdate.data.user.format()
            text = ChatJoinRequestUpdate.data.toString()
        }
        is ChatMessageReactionUpdatedUpdate -> {
            from = ChatMessageReactionUpdatedUpdate.data.chat.format()
            text = ChatMessageReactionUpdatedUpdate.data.toString()
        }
        is ChatMessageReactionsCountUpdatedUpdate -> {
            from = ChatMessageReactionsCountUpdatedUpdate.data.chat.format()
            text = ChatMessageReactionsCountUpdatedUpdate.data.toString()
        }
        is ChatBoostUpdatedUpdate -> {
            from = ChatBoostUpdatedUpdate.data.chat.format()
            text = ChatBoostUpdatedUpdate.data.toString()
        }
        is ChatBoostRemovedUpdate -> {
            from = ChatBoostRemovedUpdate.data.chat.format()
            text = ChatBoostRemovedUpdate.data.toString()
        }
        is BusinessConnectionUpdate -> {
            from = BusinessConnectionUpdate.data.user.format()
            text = BusinessConnectionUpdate.data.toString()
        }
        is BusinessMessageUpdate -> {
            from = BusinessMessageUpdate.data.from.format()
            text = BusinessMessageUpdate.data.toString()
        }
        is EditBusinessMessageUpdate -> {
            from = EditBusinessMessageUpdate.data.from.format()
            text = EditBusinessMessageUpdate.data.toString()
        }
        is DeletedBusinessMessageUpdate -> {
            from = DeletedBusinessMessageUpdate.data.chat.format()
            text = DeletedBusinessMessageUpdate.data.toString()
        }
        is PaidMediaPurchasedUpdate -> {
            from = PaidMediaPurchasedUpdate.data.from.format()
            text = PaidMediaPurchasedUpdate.data.toString()
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
    TelegramBotMiddlewaresPipelinesHandler.Builder.addMiddleware {
        TelegramBotMiddlewareBuilder.doOnRequestReturnResult { result, _, _ ->
            runCatching {
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
