/**
 * Interface for checking if a user has access to the bot.
 * Implementations of this interface determine whether a user with the given ID
 * is allowed to interact with the bot.
 */
package me.centralhardware.telegram.ktgbotapi.access.checker

/**
 * Interface for checking if a user has access to the bot.
 */
interface UserAccessChecker {

    /**
     * Checks if a user with the given ID has access to the bot.
     *
     * @param userId The Telegram user ID to check, or null if no user ID is available.
     * @return true if the user has access, false otherwise.
     */
    fun checkAccess(userId: Long?): Boolean
}