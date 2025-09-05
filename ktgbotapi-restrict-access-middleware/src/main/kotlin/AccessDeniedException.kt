/**
 * Exception thrown when a user attempts to access the bot but is not authorized.
 */
open class AccessDeniedException : Exception("User access restricted")
