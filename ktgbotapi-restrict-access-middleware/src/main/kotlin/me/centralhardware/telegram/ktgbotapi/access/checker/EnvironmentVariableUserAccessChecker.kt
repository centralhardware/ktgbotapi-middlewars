/**
 * Implementation of [UserAccessChecker] that uses environment variables to determine
 * which users have access to the bot.
 *
 * This implementation reads the "ALLOWED_USERS" environment variable, which should contain
 * a comma-separated list of Telegram user IDs that are allowed to access the bot.
 */
package me.centralhardware.telegram.ktgbotapi.access.checker

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning

/**
 * Implementation of [UserAccessChecker] that uses environment variables to determine
 * which users have access to the bot.
 */
open class EnvironmentVariableUserAccessChecker : UserAccessChecker {
    /**
     * Set of user IDs that are allowed to access the bot.
     * This is initialized from the "ALLOWED_USERS" environment variable.
     */
    private val allowedUsers: Set<Long> = parseAllowedUsers()

    /**
     * Parses the "ALLOWED_USERS" environment variable to get the set of allowed user IDs.
     * If the environment variable is not set or is empty, returns an empty set.
     * If any of the user IDs cannot be parsed as a Long, they are skipped and a warning is logged.
     *
     * @return Set of allowed user IDs
     */
    private fun parseAllowedUsers(): Set<Long> {
        val allowedUsersEnv = System.getenv("ALLOWED_USERS") ?: ""
        if (allowedUsersEnv.isBlank()) {
            return emptySet()
        }

        return allowedUsersEnv.split(",")
            .mapNotNull { userId ->
                try {
                    userId.trim().toLong()
                } catch (e: NumberFormatException) {
                    KSLog.warning("Invalid user ID in ALLOWED_USERS: $userId")
                    null
                }
            }
            .toSet()
    }

    /**
     * Checks if a user with the given ID has access to the bot.
     *
     * @param userId The Telegram user ID to check, or null if no user ID is available.
     * @return true if the user has access (either the user ID is in the allowed list or is null),
     *         false otherwise.
     */
    override fun checkAccess(userId: Long?): Boolean = userId?.let { allowedUsers.contains(it) } ?: false
}
