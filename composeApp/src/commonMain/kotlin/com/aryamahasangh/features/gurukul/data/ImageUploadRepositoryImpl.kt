package com.aryamahasangh.features.gurukul.data

import com.aryamahasangh.utils.FileUploadUtils

class ImageUploadRepositoryImpl(
  private val fileUploadUtils: FileUploadUtils
) : ImageUploadRepository {
  override suspend fun uploadReceipt(imageBytes: ByteArray, filename: String): Result<String> {
    val extension = filename.substringAfterLast('.', "webp")
    val result = fileUploadUtils.uploadCompressedImage(imageBytes, "course_receipts", extension)
    return when (result) {
      is com.aryamahasangh.util.Result.Success -> Result.success(result.data)
      is com.aryamahasangh.util.Result.Error -> Result.failure(Exception(result.message))
      else -> Result.failure(Exception("Unknown error"))
    }
  }
}
