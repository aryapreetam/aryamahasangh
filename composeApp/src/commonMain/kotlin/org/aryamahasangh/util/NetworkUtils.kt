package org.aryamahasangh.util

/**
 * Network utility functions for detecting connectivity issues
 */
object NetworkUtils {

  /**
   * Check if an error message indicates a network connectivity issue
   */
  fun isNetworkError(error: String?): Boolean {
    if (error == null) return false
    val message = error.lowercase()

    return message.contains("network") ||
      message.contains("internet") ||
      message.contains("connection") ||
      message.contains("unreachable") ||
      message.contains("timeout") ||
      message.contains("refused") ||
      message.contains("failed to connect") ||
      message.contains("unable to resolve host") ||
      message.contains("host is unresolved") ||
      message.contains("no route to host") ||
      message.contains("failed to resolve") ||
      message.contains("name resolution failed") ||
      message.contains("connection reset")
  }

  /**
   * Check if an exception indicates a network connectivity issue
   */
  fun isNetworkException(exception: Throwable?): Boolean {
    if (exception == null) return false

    val className = exception::class.simpleName?.lowercase() ?: ""
    val message = exception.message?.lowercase() ?: ""

    // Apollo/GraphQL specific exceptions
    if (className.contains("apollo")) return true

    // Standard network exceptions
    if (className.contains("unknownhostexception") ||
      className.contains("socketexception") ||
      className.contains("connectexception") ||
      className.contains("networkunreachableexception") ||
      className.contains("sockettimeoutexception") ||
      className.contains("ioexception")
    ) {
      return true
    }

    // Check the message for network-related terms
    return isNetworkError(message)
  }

  /**
   * Get a user-friendly network error message
   */
  fun getNetworkErrorMessage(error: String?, exception: Throwable?): String {
    return when {
      isNetworkException(exception) || isNetworkError(error) -> {
        "Please check your internet connection and try again"
      }
      else -> error ?: "Something went wrong"
    }
  }

  /**
   * Check if error is likely a network issue disguised as something else
   */
  fun isLikelyNetworkIssue(error: String?, exception: Throwable?): Boolean {
    // Even if the error message says "not found" or similar,
    // if we have network indicators, it's probably a network issue
    val message = error?.lowercase() ?: ""

    return isNetworkException(exception) ||
      isNetworkError(error) ||
      // Common patterns when network issues are disguised
      (message.contains("not found") &&
        (message.contains("organisation") || message.contains("data") || message.contains("resource")))
  }
}
