/**
 * Exception thrown when a user attempts to access the bot but is not authorized.
 */
package me.centralhardware.telegram.ktgbotapi.access.exception

/**
 * Exception thrown when a user attempts to access the bot but is not authorized.
 * This exception is used by the restrict access middleware to indicate that
 * a user does not have permission to interact with the bot.
 */
open class AccessDeniedException : Exception("User access restricted")
