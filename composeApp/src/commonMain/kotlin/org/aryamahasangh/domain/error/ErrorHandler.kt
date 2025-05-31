package org.aryamahasangh.domain.error

import org.aryamahasangh.util.NetworkUtils
import org.aryamahasangh.util.Result

/**
 * Global error handler for the application
 */
object ErrorHandler {
  /**
   * Handles exceptions and converts them to appropriate AppError
   */
  fun handleException(exception: Throwable): AppError {
    return exception.toAppError()
  }

  /**
   * Executes a block of code and handles any exceptions with better network detection
   */
  suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
      Result.Success(block())
    } catch (exception: Exception) {
      // Enhanced network detection using NetworkUtils
      val appError = when {
        NetworkUtils.isNetworkException(exception) -> {
          when {
            exception.message?.contains("timeout", ignoreCase = true) == true ->
              AppError.NetworkError.Timeout

            else ->
              AppError.NetworkError.NoConnection
          }
        }
        NetworkUtils.isLikelyNetworkIssue(exception.message, exception) -> {
          AppError.NetworkError.NoConnection
        }
        else -> exception.toAppError()
      }

      Result.Error(appError.getUserMessage(), exception)
    }
  }

  /**
   * Logs errors for debugging purposes
   */
  fun logError(
    error: AppError,
    context: String = ""
  ) {
    val logMessage =
      buildString {
        append("Error in $context: ")
        append(error.message)
        error.cause?.let { cause ->
          append(" | Cause: ${cause.message}")
          append(" | Stack trace: ${cause.stackTraceToString()}")
        }
      }

    // In a real app, you would use a proper logging framework
    println("ERROR: $logMessage")
  }

  /**
   * Reports errors to analytics or crash reporting service
   */
  fun reportError(
    error: AppError,
    context: String = ""
  ) {
    // In a real app, you would integrate with services like:
    // - Firebase Crashlytics
    // - Sentry
    // - Bugsnag
    // etc.

    logError(error, context)

    // Example of what you might do:
    // crashlytics.recordException(error.cause ?: Exception(error.message))
    // analytics.track("error_occurred", mapOf(
    //     "error_type" to error::class.simpleName,
    //     "error_message" to error.message,
    //     "context" to context
    // ))
  }
}

/**
 * Extension functions for Result class to work with AppError
 */
fun <T> Result<T>.mapError(transform: (String) -> AppError): Result<T> {
  return when (this) {
    is Result.Success -> this
    is Result.Error -> {
      val appError = transform(this.message)
      Result.Error(appError.getUserMessage(), this.exception)
    }
    is Result.Loading -> this
  }
}

/**
 * Extension function to handle Result with proper error logging
 */
fun <T> Result<T>.onError(action: (AppError) -> Unit): Result<T> {
  if (this is Result.Error) {
    val appError = when {
      NetworkUtils.isNetworkException(this.exception) -> AppError.NetworkError.NoConnection
      else -> this.exception?.toAppError() ?: AppError.UnknownError(this.message)
    }
    action(appError)
  }
  return this
}

/**
 * Extension function to retry operations with exponential backoff
 */
suspend fun <T> retryWithBackoff(
  maxRetries: Int = 3,
  initialDelayMs: Long = 1000,
  maxDelayMs: Long = 10000,
  factor: Double = 2.0,
  block: suspend () -> Result<T>
): Result<T> {
  var currentDelay = initialDelayMs
  var lastResult: Result<T>? = null

  repeat(maxRetries) { attempt ->
    lastResult = block()

    when (val result = lastResult!!) {
      is Result.Success -> return result
      is Result.Error -> {
        if (attempt == maxRetries - 1) {
          // Last attempt, return the error
          return result
        }

        // Check if error is retryable
        val appError = when {
          NetworkUtils.isNetworkException(result.exception) -> AppError.NetworkError.NoConnection
          else -> result.exception?.toAppError() ?: AppError.UnknownError(result.message)
        }

        if (!isRetryableError(appError)) {
          return result
        }

        // Wait before retrying
        kotlinx.coroutines.delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
      }
      is Result.Loading -> {
        // Continue to next iteration
      }
    }
  }

  return lastResult ?: Result.Error("Max retries exceeded")
}

/**
 * Determines if an error is retryable
 */
private fun isRetryableError(error: AppError): Boolean {
  return when (error) {
    is AppError.NetworkError.Timeout,
    is AppError.NetworkError.ServerError,
    is AppError.NetworkError.UnknownNetworkError,
    is AppError.NetworkError.NoConnection -> true
    is AppError.NetworkError.HttpError -> error.code >= 500
    else -> false
  }
}
