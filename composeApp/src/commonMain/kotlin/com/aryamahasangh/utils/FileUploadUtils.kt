package com.aryamahasangh.utils

import com.aryamahasangh.network.bucket
import com.aryamahasangh.util.Result
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.datetime.Clock
import com.aryamahasangh.isIos
import com.aryamahasangh.auth.SessionManager

object FileUploadUtils {
  /**
   * Low-level helper: upload raw bytes to a specific storage path.
   * On iOS, prefer resumable (tus) to avoid Darwin transport errors; fallback once to regular upload.
   * Returns public URL on success, Result.Error on failure.
   */
  private suspend fun preRefreshSession() {
    try {
      SessionManager.isUserAuthenticated() // triggers refresh if needed
    } catch (_: Exception) {
      // ignore; upload might still be public
    }
  }

  suspend fun uploadBytes(
    path: String,
    data: ByteArray
  ): Result<String> {
    return try {
      preRefreshSession()
      println("[Upload] path=$path size=${data.size} platform=${if (isIos()) "iOS" else "Other"}")
      // NOTE: Resumable TUS API differs across supabase-kt versions. Using stable upload() for now.
      // If needed, switch to bucket.resumable.createOrContinueUpload(path).upload(...) once verified.
      bucket.upload(path = path, data = data)
      val publicUrl = bucket.publicUrl(path)
      println("[Upload] success url=$publicUrl")
      Result.Success(publicUrl)
    } catch (e: Exception) {
      println("[Upload] error: ${e.message}")
      Result.Error(e.message ?: "फ़ाइल अपलोड करने में त्रुटि")
    }
  }

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
      val fileName = "${folder}_${timestamp}_$randomSuffix.$extension"
      val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

      // Use unified helper
      uploadBytes(path = path, data = imageBytes)
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

      // Use unified helper
      uploadBytes(path = path, data = file.readBytes())
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

    // Use unified helper and throw on error to preserve legacy signature
    return when (val res = uploadBytes(path = fileName, data = file.readBytes())) {
      is Result.Success -> res.data
      is Result.Error -> throw Exception(res.message)
      else -> throw Exception("अज्ञात त्रुटि")
    }
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
        val bytes = file.readBytes()
        // Validate original file size (before any compression)
        val originalFileSize = bytes.size
        if (originalFileSize > maxSizeBytes) {
          return Result.Error("फ़ाइल ${file.name} का आकार ${maxFileSizeMB}MB से अधिक है")
        }

        val timestamp = Clock.System.now().epochSeconds
        val randomSuffix = (1000..9999).random()
        val fileExtension = "webp"
        val fileName = "${timestamp}_$randomSuffix.$fileExtension"
        val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

        when (val res = uploadBytes(path = path, data = bytes)) {
          is Result.Success -> uploadedUrls.add(res.data)
          is Result.Error -> return Result.Error(res.message)
          else -> return Result.Error("अज्ञात त्रुटि")
        }
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
