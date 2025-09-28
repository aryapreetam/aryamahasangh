// ... existing code ...
package com.aryamahasangh.features.gurukul.data

interface ImageUploadRepository {
  suspend fun uploadReceipt(imageBytes: ByteArray, filename: String): Result<String>
}
// ... existing code ...
