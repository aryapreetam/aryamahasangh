package com.aryamahasangh.nhost.storage

import com.aryamahasangh.nhost.models.FileUploadResponse
import com.aryamahasangh.nhost.models.PresignedUrl
import com.aryamahasangh.nhost.models.PresignedUrlParams

/**
 * Storage module for NHost file operations
 */
interface NHostStorage {
  /**
   * Upload a file to NHost storage
   *
   * @param file The file content as ByteArray
   * @param name The name of the file
   * @param bucketId The bucket to upload to (defaults to "default")
   * @param mimeType The MIME type of the file (optional)
   * @return Result containing FileUploadResponse on success
   */
  suspend fun upload(
    file: ByteArray,
    name: String,
    bucketId: String = "default",
    mimeType: String? = null
  ): Result<FileUploadResponse>

  /**
   * Delete a file from NHost storage
   *
   * @param fileId The ID of the file to delete
   * @return Result with Unit on success
   */
  suspend fun delete(fileId: String): Result<Unit>

  /**
   * Get a presigned URL for a file
   *
   * @param params Parameters for presigned URL including fileId and optional image transformations
   * @return Result containing PresignedUrl on success
   */
  suspend fun getPresignedUrl(params: PresignedUrlParams): Result<PresignedUrl>

  /**
   * Get a public URL for a file
   * Note: This requires the file to have public read permissions
   *
   * @param fileId The ID of the file
   * @return The public URL for the file
   */
  fun getPublicUrl(fileId: String): String
}

