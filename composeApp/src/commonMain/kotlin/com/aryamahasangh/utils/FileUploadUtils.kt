package com.aryamahasangh.utils

import com.aryamahasangh.network.bucket
import com.aryamahasangh.util.Result
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.datetime.Clock

object FileUploadUtils {
  /**
   * Upload compressed image ByteArray to Supabase storage
   *
   * @param imageBytes The compressed image data (e.g., WebP bytes)
   * @param folder The folder in storage to upload to (e.g., "test")
   * @param extension The file extension (default: "webp")
   * @return Result containing the public URL of the uploaded file
   */
  suspend fun uploadCompressedImage(
    imageBytes: ByteArray,
    folder: String,
    extension: String = "webp"
  ): Result<String> {
    return try {
      val timestamp = Clock.System.now().epochSeconds
      val randomSuffix = (1000..9999).random()
      val fileName = "compressed_${timestamp}_$randomSuffix.$extension"
      val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

      val uploadResponse = bucket.upload(
        path = path,
        data = imageBytes
      )

      val publicUrl = bucket.publicUrl(path)
      Result.Success(publicUrl)
    } catch (e: Exception) {
      Result.Error(e.message ?: "Compressed image upload failed")
    }
  }

  /**
   * Upload an image file to Supabase storage
   *
   * @param file The platform file to upload
   * @param folder The folder in storage to upload to (e.g., "activity_overview", "profile_images")
   * @return Result containing the public URL of the uploaded file
   */
  suspend fun uploadImage(
    file: PlatformFile,
    folder: String
  ): Result<String> {
    return try {
      val timestamp = Clock.System.now().epochSeconds
      val fileExtension = "webp"
      val fileName = "${folder}_$timestamp.$fileExtension"
      val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

      val uploadResponse =
        bucket.upload(
          path = path,
          data = file.readBytes()
        )

      val publicUrl = bucket.publicUrl(path)
      Result.Success(publicUrl)
    } catch (e: Exception) {
      Result.Error(e.message ?: "Image upload failed")
    }
  }

  /**
   * Upload a single file to Supabase storage (legacy method for compatibility)
   *
   * @param file The platform file to upload
   * @return The public URL of the uploaded file
   * @throws Exception if upload fails
   */
  suspend fun uploadFile(file: PlatformFile): String {
    val timestamp = Clock.System.now().epochSeconds
    val fileExtension = "webp"
    val fileName = "$timestamp.$fileExtension"

    val uploadResponse =
      bucket.upload(
        path = fileName,
        data = file.readBytes()
      )

    return bucket.publicUrl(uploadResponse.path)
  }

  /**
   * Upload multiple files to Supabase storage with size validation
   *
   * @param files List of platform files to upload
   * @param folder The folder in storage to upload to (e.g., "family_photos", "member_images")
   * @param maxFileSizeMB Maximum file size in megabytes (default: 2MB)
   * @return Result containing list of public URLs of uploaded files
   */
  suspend fun uploadFiles(
    files: List<PlatformFile>,
    folder: String = "",
    maxFileSizeMB: Int = 2
  ): Result<List<String>> {
    return try {
      val maxSizeBytes = maxFileSizeMB * 1024 * 1024
      val uploadedUrls = mutableListOf<String>()

      for (file in files) {
        // Validate original file size (before any compression)
        val originalFileSize = file.readBytes().size
        if (originalFileSize > maxSizeBytes) {
          return Result.Error("फ़ाइल ${file.name} का आकार ${maxFileSizeMB}MB से अधिक है")
        }

        // Upload file
        val timestamp = Clock.System.now().epochSeconds
        val randomSuffix = (1000..9999).random()
        val fileExtension = "webp"
        val fileName = "${timestamp}_$randomSuffix.$fileExtension"
        val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

        val uploadResponse =
          bucket.upload(
            path = path,
            data = file.readBytes()
          )
        val publicUrl = bucket.publicUrl(path)
        uploadedUrls.add(publicUrl)
      }

      Result.Success(uploadedUrls)
    } catch (e: Exception) {
      Result.Error("फ़ाइल अपलोड करने में त्रुटि: ${e.message}")
    }
  }

  /**
   * Delete files from Supabase storage
   *
   * @param urls List of file URLs to delete
   * @return Result indicating success or failure
   */
  suspend fun deleteFiles(urls: List<String>): Result<Unit> {
    return try {
      if (urls.isEmpty()) return Result.Success(Unit)

      val filePaths =
        urls.map { url ->
          url.substringAfterLast("/")
        }

      bucket.delete(filePaths)
      Result.Success(Unit)
    } catch (e: Exception) {
      Result.Error("फ़ाइल हटाने में त्रुटि: ${e.message}")
    }
  }
}
