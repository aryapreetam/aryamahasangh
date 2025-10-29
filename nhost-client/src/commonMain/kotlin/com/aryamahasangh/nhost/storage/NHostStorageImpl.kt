package com.aryamahasangh.nhost.storage

import com.aryamahasangh.nhost.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

/**
 * Implementation of NHost storage operations
 */
internal class NHostStorageImpl(
  private val httpClient: HttpClient,
  private val baseUrl: String
) : NHostStorage {

  override suspend fun upload(
    file: ByteArray,
    name: String,
    bucketId: String,
    mimeType: String?
  ): Result<FileUploadResponse> {
    return try {
      val response = httpClient.submitFormWithBinaryData(
        url = "$baseUrl/v1/files",
        formData = formData {
          append("bucket-id", bucketId)
          append("file[]", file, Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
            if (mimeType != null) {
              append(HttpHeaders.ContentType, mimeType)
            }
          })
        }
      )

      if (response.status.isSuccess()) {
        val uploadResponse = response.body<FileUploadResponse>()
        Result.success(uploadResponse)
      } else {
        val errorResponse = try {
          response.body<ErrorResponse>()
        } catch (e: Exception) {
          ErrorResponse("File upload failed", response.status.value)
        }
        Result.failure(Exception(errorResponse.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun delete(fileId: String): Result<Unit> {
    return try {
      val response = httpClient.delete("$baseUrl/v1/files/$fileId")

      if (response.status.isSuccess()) {
        Result.success(Unit)
      } else {
        val errorResponse = try {
          response.body<ErrorResponse>()
        } catch (e: Exception) {
          ErrorResponse("File deletion failed", response.status.value)
        }
        Result.failure(Exception(errorResponse.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun getPresignedUrl(params: PresignedUrlParams): Result<PresignedUrl> {
    return try {
      val response = httpClient.get("$baseUrl/v1/files/${params.fileId}/presignedUrl") {
        params.quality?.let { parameter("quality", it) }
        params.width?.let { parameter("width", it) }
        params.height?.let { parameter("height", it) }
        params.blur?.let { parameter("blur", it) }
      }

      if (response.status.isSuccess()) {
        val presignedResponse = response.body<PresignedUrlResponse>()
        Result.success(presignedResponse.presignedUrl)
      } else {
        val errorResponse = try {
          response.body<ErrorResponse>()
        } catch (e: Exception) {
          ErrorResponse("Failed to get presigned URL", response.status.value)
        }
        Result.failure(Exception(errorResponse.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override fun getPublicUrl(fileId: String): String {
    return "$baseUrl/v1/storage/files/$fileId"
  }
}

