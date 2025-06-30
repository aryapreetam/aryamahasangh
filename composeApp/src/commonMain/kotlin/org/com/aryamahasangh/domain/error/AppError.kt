package com.aryamahasangh.domain.error

import com.aryamahasangh.utils.LocalizationManager

/**
 * Sealed class representing different types of application errors
 */
sealed class AppError(
  open val message: String,
  open val cause: Throwable? = null
) {
  /**
   * Network-related errors
   */
  sealed class NetworkError(
    override val message: String,
    override val cause: Throwable? = null
  ) : AppError(message, cause) {
    object NoConnection : NetworkError("No internet connection available")

    object Timeout : NetworkError("Request timed out")

    object ServerError : NetworkError("Server error occurred")

    data class HttpError(val code: Int, override val message: String) : NetworkError(message)

    data class UnknownNetworkError(override val cause: Throwable) : NetworkError(
      "Unknown network error: ${cause.message}",
      cause
    )
  }

  /**
   * CRUD operation errors from Supabase functions
   * These errors contain message codes that get localized to Hindi
   */
  sealed class CrudError(
    val messageCode: String,
    override val message: String
  ) : AppError(message) {

    /**
     * Success response from CRUD operations
     */
    data class Success(
      private val code: String,
      val details: String? = null
    ) : CrudError(code, LocalizationManager.translateMessageCode(code))

    /**
     * Error response from CRUD operations
     */
    data class Error(
      private val code: String,
      val details: String? = null
    ) : CrudError(code, LocalizationManager.translateMessageCode(code))

    /**
     * Gets the localized message in Hindi
     */
    fun getLocalizedMessage(): String {
      return LocalizationManager.translateMessageCode(messageCode)
    }

    /**
     * Checks if this is a success response
     */
    fun isSuccess(): Boolean {
      return LocalizationManager.isSuccessMessage(messageCode)
    }

    /**
     * Checks if this is an error response
     */
    fun isError(): Boolean {
      return LocalizationManager.isErrorMessage(messageCode)
    }
  }

  /**
   * Validation-related errors
   */
  sealed class ValidationError(
    override val message: String
  ) : AppError(message) {
    data class RequiredField(val fieldName: String) : ValidationError("$fieldName is required")

    data class InvalidFormat(val fieldName: String) : ValidationError("$fieldName has invalid format")

    data class TooShort(val fieldName: String, val minLength: Int) : ValidationError(
      "$fieldName must be at least $minLength characters"
    )

    data class TooLong(val fieldName: String, val maxLength: Int) : ValidationError(
      "$fieldName must not exceed $maxLength characters"
    )

    data class InvalidEmail(val email: String) : ValidationError("Invalid email address: $email")

    data class InvalidPhone(val phone: String) : ValidationError("Invalid phone number: $phone")

    data class InvalidDate(val date: String) : ValidationError("Invalid date: $date")

    data class Custom(override val message: String) : ValidationError(message)
  }

  /**
   * Authentication and authorization errors
   */
  sealed class AuthError(
    override val message: String
  ) : AppError(message) {
    object NotAuthenticated : AuthError("User is not authenticated")

    object NotAuthorized : AuthError("User is not authorized to perform this action")

    object SessionExpired : AuthError("Session has expired")

    object InvalidCredentials : AuthError("Invalid credentials provided")
  }

  /**
   * Data-related errors
   */
  sealed class DataError(
    override val message: String,
    override val cause: Throwable? = null
  ) : AppError(message, cause) {
    object NotFound : DataError("Requested data not found")

    object AlreadyExists : DataError("Data already exists")

    data class ParseError(override val cause: Throwable) : DataError(
      "Failed to parse data: ${cause.message}",
      cause
    )

    data class DatabaseError(override val cause: Throwable) : DataError(
      "Database error: ${cause.message}",
      cause
    )
  }

  /**
   * Business logic errors
   */
  sealed class BusinessError(
    override val message: String
  ) : AppError(message) {
    object InsufficientPermissions : BusinessError("Insufficient permissions")

    object OperationNotAllowed : BusinessError("Operation not allowed")

    data class Custom(override val message: String) : BusinessError(message)
  }

  /**
   * Unknown or unexpected errors
   */
  data class UnknownError(
    override val message: String = "An unknown error occurred",
    override val cause: Throwable? = null
  ) : AppError(message, cause)
}

/**
 * Helper function to create CrudError from JSON response
 */
fun createCrudErrorFromResponse(
  success: Boolean?,
  messageCode: String?,
  errorCode: String?,
  errorDetails: String? = null
): AppError.CrudError {
  return when {
    success == true && messageCode != null ->
      AppError.CrudError.Success(messageCode, errorDetails)

    success == false && errorCode != null ->
      AppError.CrudError.Error(errorCode, errorDetails)

    messageCode != null ->
      AppError.CrudError.Success(messageCode, errorDetails)

    errorCode != null ->
      AppError.CrudError.Error(errorCode, errorDetails)

    else ->
      AppError.CrudError.Error("UNKNOWN_ERROR", errorDetails)
  }
}

/**
 * Extension function to convert exceptions to AppError with better network detection
 */
fun Throwable.toAppError(): AppError {
  val message = this.message?.lowercase() ?: ""
  val className = this::class.simpleName?.lowercase() ?: ""

  // Debug logging to see what exceptions we're getting
  println("DEBUG - Exception: $className, Message: $message")

  return when {
    // Apollo GraphQL specific exceptions (check these first)
    className.contains("apolloexception") -> AppError.NetworkError.UnknownNetworkError(this)
    className.contains("apollonetworkexception") -> AppError.NetworkError.NoConnection
    className.contains("apollohttpexception") -> AppError.NetworkError.ServerError

    // Network exceptions (highest priority)
    className.contains("unknownhostexception") -> AppError.NetworkError.NoConnection
    className.contains("socketexception") -> AppError.NetworkError.NoConnection
    className.contains("connectexception") -> AppError.NetworkError.NoConnection
    className.contains("networkunreachableexception") -> AppError.NetworkError.NoConnection
    className.contains("sockettimeoutexception") -> AppError.NetworkError.Timeout

    // IO exceptions - check message for network context
    className.contains("ioexception") -> {
      AppError.NetworkError.UnknownNetworkError(this) // Default to network error for IO exceptions
    }

    // Message-based network detection (check BEFORE "organisation not found")
    message.contains("network is unreachable") -> AppError.NetworkError.NoConnection
    message.contains("no network") -> AppError.NetworkError.NoConnection
    message.contains("no internet") -> AppError.NetworkError.NoConnection
    message.contains("connection refused") -> AppError.NetworkError.NoConnection
    message.contains("host is unresolved") -> AppError.NetworkError.NoConnection
    message.contains("unable to resolve host") -> AppError.NetworkError.NoConnection
    message.contains("failed to connect") -> AppError.NetworkError.NoConnection
    message.contains("failed to resolve") -> AppError.NetworkError.NoConnection
    message.contains("no route to host") -> AppError.NetworkError.NoConnection
    message.contains("connection reset") -> AppError.NetworkError.NoConnection
    message.contains("name resolution failed") -> AppError.NetworkError.NoConnection
    message.contains("connection timed out") -> AppError.NetworkError.Timeout
    message.contains("read timeout") -> AppError.NetworkError.Timeout
    message.contains("timeout") -> AppError.NetworkError.Timeout

    // Check for any generic connection/network terms in the message
    message.contains("network") -> AppError.NetworkError.NoConnection
    message.contains("connection") -> AppError.NetworkError.NoConnection
    message.contains("internet") -> AppError.NetworkError.NoConnection
    message.contains("host") -> AppError.NetworkError.NoConnection

    // Server errors
    message.contains("server error") -> AppError.NetworkError.ServerError
    message.contains("503") || message.contains("502") || message.contains("500") -> AppError.NetworkError.ServerError
    message.contains("404") -> AppError.NetworkError.HttpError(404, "Resource not found")

    // Business logic errors (only after network checks)
    message.contains("organisation not found") -> {
      // This could be a network issue disguised as "not found"
      // If we reach here, it's likely a legitimate "not found" after successful network call
      AppError.DataError.NotFound
    }

    // Validation exceptions
    this is IllegalArgumentException ->
      AppError.ValidationError.Custom(
        this.message ?: "Invalid argument"
      )
    this is IllegalStateException ->
      AppError.BusinessError.Custom(
        this.message ?: "Invalid state"
      )

    // Default fallback - assume network issues for most exceptions
    else -> {
      // For any exception we can't specifically identify, check if it seems network-related
      if (className.contains("exception")) {
        // Most exceptions in mobile/network apps are network-related
        AppError.NetworkError.UnknownNetworkError(this)
      } else {
        AppError.UnknownError(
          message = this.message ?: "Unknown error occurred",
          cause = this
        )
      }
    }
  }
}

/**
 * Extension function to get user-friendly error messages with Hindi support
 */
fun AppError.getUserMessage(): String {
  return when (this) {
    is AppError.CrudError -> getLocalizedMessage()
    is AppError.NetworkError.NoConnection -> "कृपया अपना इंटरनेट कनेक्शन जांचें और पुनः प्रयास करें"
    is AppError.NetworkError.Timeout -> "अनुरोध का समय समाप्त हो गया। कृपया पुनः प्रयास करें"
    is AppError.NetworkError.ServerError -> "सर्वर अस्थायी रूप से अनुपलब्ध है। कृपया बाद में पुनः प्रयास करें"
    is AppError.NetworkError.HttpError ->
      when (code) {
        400 -> "अमान्य अनुरोध। कृपया अपना इनपुट जांचें"
        401 -> "प्रमाणीकरण आवश्यक। कृपया लॉगिन करें"
        403 -> "आपको यह कार्य करने की अनुमति नहीं है"
        404 -> "अनुरोधित संसाधन नहीं मिला"
        500 -> "सर्वर त्रुटि। कृपया बाद में पुनः प्रयास करें"
        else -> "नेटवर्क त्रुटि हुई (कोड: $code)"
      }
    is AppError.ValidationError -> message
    is AppError.AuthError.NotAuthenticated -> "कृपया जारी रखने के लिए लॉगिन करें"
    is AppError.AuthError.NotAuthorized -> "आपको यह कार्य करने की अनुमति नहीं है"
    is AppError.AuthError.SessionExpired -> "आपका सत्र समाप्त हो गया है। कृपया पुनः लॉगिन करें"
    is AppError.AuthError.InvalidCredentials -> "अमान्य उपयोगकर्ता नाम या पासवर्ड"
    is AppError.DataError.NotFound -> "अनुरोधित जानकारी नहीं मिली"
    is AppError.DataError.AlreadyExists -> "यह जानकारी पहले से मौजूद है"
    is AppError.BusinessError.InsufficientPermissions -> "आपके पास पर्याप्त अनुमतियां नहीं हैं"
    is AppError.BusinessError.OperationNotAllowed -> "यह कार्य अनुमतित नहीं है"
    else -> message
  }
}
