package org.aryamahasangh.utils

import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.datetime.Clock
import org.aryamahasangh.network.bucket
import org.aryamahasangh.util.Result

object FileUploadUtils {

  /**
   * Upload an image file to Supabase storage
   *
   * @param file The platform file to upload
   * @param folder The folder in storage to upload to (e.g., "activity_overview", "profile_images")
   * @return Result containing the public URL of the uploaded file
   */
  suspend fun uploadImage(file: PlatformFile, folder: String): Result<String> {
    return try {
      val timestamp = Clock.System.now().epochSeconds
      val fileExtension = file.name.substringAfterLast('.', "jpg")
      val fileName = "${folder}_${timestamp}.${fileExtension}"
      val path = if (folder.isNotEmpty()) "$folder/$fileName" else fileName

      val uploadResponse = bucket.upload(
        path = path,
        data = file.readBytes()
      )

      val publicUrl = bucket.publicUrl(uploadResponse.path)
      Result.Success(publicUrl)
    } catch (e: Exception) {
      Result.Error(e.message ?: "Image upload failed")
    }
  }
}
