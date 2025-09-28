package com.aryamahasangh.features.gurukul.data

import com.aryamahasangh.utils.FileUploadUtils

class ImageUploadRepositoryImpl : ImageUploadRepository {
  override suspend fun uploadReceipt(imageBytes: ByteArray, filename: String): kotlin.Result<String> {
    val extension = filename.substringAfterLast('.', "webp")
    val result = FileUploadUtils.uploadCompressedImage(imageBytes, "course_receipts", extension)
    return when (result) {
      is com.aryamahasangh.util.Result.Success -> kotlin.Result.success(result.getOrNull()!!)
      is com.aryamahasangh.util.Result.Error -> kotlin.Result.failure(Exception("रसीद अपलोड विफल: ${result.errorOrNull()}"))
      else -> kotlin.Result.failure(Exception("रसीद अपलोड विफल: Unknown error"))
    }
  }
}
