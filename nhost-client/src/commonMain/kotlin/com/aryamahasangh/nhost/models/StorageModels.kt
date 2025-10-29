package com.aryamahasangh.nhost.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Metadata about an uploaded file
 */
@Serializable
data class FileMetadata(
  val id: String,
  val name: String,
  val size: Long? = null,
  val bucketId: String,
  val etag: String? = null,
  val createdAt: Instant,
  val updatedAt: Instant,
  val isUploaded: Boolean,
  val mimeType: String? = null,
  val uploadedByUserId: String? = null,
  val metadata: Map<String, String>? = null
)

/**
 * Response from file upload endpoint
 */
@Serializable
data class FileUploadResponse(
  val id: String,
  val name: String,
  val size: Long? = null,
  val bucketId: String,
  val etag: String? = null,
  val createdAt: Instant,
  val updatedAt: Instant,
  val isUploaded: Boolean,
  val mimeType: String? = null,
  val uploadedByUserId: String? = null,
  val metadata: Map<String, String>? = null
)

/**
 * A presigned URL with expiration time
 */
@Serializable
data class PresignedUrl(
  val url: String,
  val expiration: Long // Unix timestamp in seconds
)

/**
 * Response from presigned URL endpoint
 */
@Serializable
data class PresignedUrlResponse(
  val presignedUrl: PresignedUrl
)

/**
 * Parameters for getting a presigned URL
 */
data class PresignedUrlParams(
  val fileId: String,
  val quality: Int? = null,
  val width: Int? = null,
  val height: Int? = null,
  val blur: Int? = null
)

