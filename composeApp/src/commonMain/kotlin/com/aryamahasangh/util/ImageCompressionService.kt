package com.aryamahasangh.util

import com.aryamahasangh.imgcompress.CompressionConfig
import com.aryamahasangh.imgcompress.ImageCompressor
import com.aryamahasangh.imgcompress.ImageData
import com.aryamahasangh.imgcompress.ResizeOptions
import com.aryamahasangh.isIos
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/**
 * Simple service for compressing images with configurable targets
 */
object ImageCompressionService {
  // Generic target-size compression (non-iOS or fallback)
  private suspend fun compressByTarget(bytes: ByteArray, mimeType: String, targetKb: Int, maxLongEdge: Int): ByteArray {
    return try {
      ImageCompressor.compress(
        input = ImageData(bytes, mimeType),
        config = CompressionConfig.ByTargetSize(targetKb),
        resize = ResizeOptions(maxLongEdgePx = maxLongEdge)
      ).bytes
    } catch (_: Exception) { bytes }
  }

  // iOS quality loop using kmpCompressImage (already suspending)
  private suspend fun compressIosQualityLoop(original: ByteArray, maxLongEdge: Int, targetKb: Int, minQuality: Int = 20): ByteArray {
    val targetBytes = targetKb * 1024
    val maxAcceptableBytes = (targetKb * 1.2 * 1024).toInt()  // Allow up to 30% above target
    
    // Progressive quality reduction with reasonable steps to preserve quality
    val qualities = listOf(75, 65, 60, 55, 50, 45, 40, 35, 30, 25)
    var last = original
    var bestMatch = original  // Track best result that's under maxAcceptable
    
    for (q in qualities) {
      val attempt = try { 
        kmpCompressImage(original, q, maxLongEdge, maxLongEdge) 
      } catch (_: Exception) { 
        continue 
      }
      last = attempt
      
      // If within acceptable range (target to target+30%), return immediately
      if (attempt.size in targetBytes..maxAcceptableBytes) {
        return attempt
      }
      
      // Track the best match that's under maxAcceptable
      if (attempt.size <= maxAcceptableBytes && 
          (bestMatch.contentEquals(original) || attempt.size > bestMatch.size)) {
        bestMatch = attempt
      }
      
      // If we went below target significantly, return previous attempt if it was closer
      if (attempt.size < targetBytes * 0.7 && !bestMatch.contentEquals(original)) {
        return bestMatch
      }
    }
    
    // Return best match if we found one, otherwise last attempt
    return if (!bestMatch.contentEquals(original)) bestMatch else last
  }

  /**
   * Compress a file synchronously and return the compressed bytes
   */
  suspend fun compressSync(
    file: PlatformFile,
    targetKb: Int,
    maxLongEdge: Int = 1024
  ): ByteArray {
    val bytes = compressGeneral(file, targetKb = targetKb, maxLongEdge = maxLongEdge)
    return if (bytes.isNotEmpty()) bytes else try { file.readBytes() } catch (_: Exception) { ByteArray(0) }
  }

  suspend fun compressGeneral(file: PlatformFile, targetKb: Int = 100, maxLongEdge: Int = 1024): ByteArray {
    val bytes = try { file.readBytes() } catch (_: Exception) { return ByteArray(0) }
    val mime = determineMimeType(file.name)
    return if (isIos()) {
      val iosCompressed = compressIosQualityLoop(bytes, maxLongEdge, targetKb)
      iosCompressed
    } else {
      compressByTarget(bytes, mime, targetKb, maxLongEdge)
    }
  }

  suspend fun compressThumbnail(file: PlatformFile, targetRangeKb: IntRange = 40..50, maxLongEdge: Int = 500): ByteArray {
    val target = targetRangeKb.last // use upper bound for stable quality
    val bytes = try { file.readBytes() } catch (_: Exception) { return ByteArray(0) }
    val mime = determineMimeType(file.name)
    return if (isIos()) compressIosQualityLoop(bytes, maxLongEdge, target) else compressByTarget(bytes, mime, target, maxLongEdge)
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
