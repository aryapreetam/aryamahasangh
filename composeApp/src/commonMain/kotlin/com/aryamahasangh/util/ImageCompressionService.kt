package com.aryamahasangh.util

import com.aryamahasangh.imgcompress.CompressionConfig
import com.aryamahasangh.imgcompress.ImageCompressor
import com.aryamahasangh.imgcompress.ImageData
import com.aryamahasangh.imgcompress.ResizeOptions
import io.github.vinceglb.filekit.core.PlatformFile

/**
 * Simple service for compressing images with configurable targets
 */
object ImageCompressionService {

  /**
   * Compress a file synchronously and return the compressed bytes
   */
  suspend fun compressSync(
    file: PlatformFile,
    targetKb: Int,
    maxLongEdge: Int = 2560
  ): ByteArray {
    return try {
      val originalBytes = file.readBytes()
      val mimeType = determineMimeType(file.name)

      val compressed = ImageCompressor.compress(
        input = ImageData(originalBytes, mimeType),
        config = CompressionConfig.ByTargetSize(targetKb),
        resize = ResizeOptions(maxLongEdgePx = maxLongEdge)
      )

      compressed.bytes
    } catch (e: Exception) {
      // Fallback to original bytes on compression failure
      file.readBytes()
    }
  }

  /**
   * Determine MIME type from file extension
   */
  private fun determineMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
      "jpg", "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "webp" -> "image/webp"
      else -> "image/jpeg"
    }
  }
}
