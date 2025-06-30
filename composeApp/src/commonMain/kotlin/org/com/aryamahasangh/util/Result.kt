package com.aryamahasangh.util

/**
 * A generic class that holds a value or an error.
 * @param T the type of the value
 */
sealed class Result<out T> {
  /**
   * Represents successful operation with data
   */
  data class Success<out T>(val data: T) : Result<T>()

  /**
   * Represents failed operation with error message
   */
  data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()

  /**
   * Represents loading state
   */
  object Loading : Result<Nothing>()

  /**
   * Returns true if this is a Success
   */
  val isSuccess: Boolean get() = this is Success

  /**
   * Returns true if this is an Error
   */
  val isError: Boolean get() = this is Error

  /**
   * Returns true if this is Loading
   */
  val isLoading: Boolean get() = this is Loading

  /**
   * Returns the data if this is a Success, null otherwise
   */
  fun getOrNull(): T? = if (this is Success) data else null

  /**
   * Returns the error message if this is an Error, null otherwise
   */
  fun errorOrNull(): String? = if (this is Error) message else null
}

/**
 * Executes the given [block] and wraps the result in a [Result].
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
  return try {
    Result.Success(block())
  } catch (e: Exception) {
    Result.Error(e.message ?: "Unknown error occurred", e)
  }
}
