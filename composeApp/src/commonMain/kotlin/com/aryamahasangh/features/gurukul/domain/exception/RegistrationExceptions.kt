package com.aryamahasangh.features.gurukul.domain.exception

sealed class RegistrationException(message: String, cause: Throwable? = null)
  : Exception(message, cause)

class InvalidInputException(message: String) : RegistrationException(message)

enum class UploadType { Receipt, Photo }

class UploadFailedException(
  val type: UploadType,
  message: String,
  cause: Throwable? = null
) : RegistrationException(message, cause)

class RegistrationSubmissionException(
  message: String,
  cause: Throwable? = null
) : RegistrationException(message, cause)
