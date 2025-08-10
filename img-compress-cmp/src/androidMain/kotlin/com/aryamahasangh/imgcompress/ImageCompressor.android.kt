package com.aryamahasangh.imgcompress

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

actual object ImageCompressor {
  actual suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions
  ): CompressedImage = withContext(Dispatchers.Default) {
    val startTime = System.currentTimeMillis()
    val originalSize = input.rawBytes.size

    fun decodeBitmapOnce(bytes: ByteArray): Bitmap {
      val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inMutable = false
        inDither = false
        inScaled = false
      }
      return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: throw IllegalArgumentException("Unable to decode input image: ${input.mimeType}")
    }

    fun resizeIfNeeded(sourceBitmap: Bitmap): Bitmap {
      val maxEdge = resize.maxLongEdgePx ?: return sourceBitmap
      val width = sourceBitmap.width
      val height = sourceBitmap.height
      val longEdge = max(width, height)

      if (resize.downscaleOnly && longEdge <= maxEdge) {
        return sourceBitmap
      }

      val scale = maxEdge.toFloat() / longEdge.toFloat()
      val newWidth = (width * scale).toInt().coerceAtLeast(1)
      val newHeight = (height * scale).toInt().coerceAtLeast(1)

      val resized = Bitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true)

      if (resized !== sourceBitmap) {
      }

      return resized
    }

    fun encodeWebP(bitmap: Bitmap, quality: Int): ByteArray {
      val outputStream = java.io.ByteArrayOutputStream()

      val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
      } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
      }

      val success = bitmap.compress(format, quality.coerceIn(0, 100), outputStream)
      if (!success) {
        throw RuntimeException("Failed to encode WebP at quality $quality")
      }

      return outputStream.toByteArray()
    }

    val sourceBitmap = decodeBitmapOnce(input.rawBytes)
    val workingBitmap = resizeIfNeeded(sourceBitmap)

    if (workingBitmap !== sourceBitmap) {
      sourceBitmap.recycle()
    }

    try {
      val result = when (config) {
        is CompressionConfig.ByQuality -> {
          val quality = config.qualityPercent.coerceIn(0f, 100f).toInt()
          val resultBytes = encodeWebP(workingBitmap, quality)

          CompressedImage(
            bytes = resultBytes,
            originalSize = originalSize,
            compressedSize = resultBytes.size,
            metadata = CompressionMetadata(
              effectiveQualityPercent = quality.toFloat(),
              iterations = 1,
              elapsedMillis = System.currentTimeMillis() - startTime
            )
          )
        }

        is CompressionConfig.ByTargetSize -> {
          // OPTIMIZED: Smart quality estimation + narrow binary search
          val targetBytes = max(1, config.targetSizeKb) * 1024
          val tolerance = max((targetBytes * 0.05).toInt(), 10 * 1024)

          // Smart quality prediction to reduce iterations
          val estimatedQuality = QualityPredictor.predictOptimalQuality(originalSize, config.targetSizeKb)
          val searchRange = QualityPredictor.getSearchRange(estimatedQuality)

          var lo = searchRange.first
          var hi = searchRange.last
          var bestQuality = estimatedQuality
          var bestBytes = encodeWebP(workingBitmap, estimatedQuality) // Start with estimate
          var iterations = 1

          // Quick check if estimate is already good enough
          if (bestBytes.size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
            return@withContext CompressedImage(
              bytes = bestBytes,
              originalSize = originalSize,
              compressedSize = bestBytes.size,
              metadata = CompressionMetadata(
                effectiveQualityPercent = estimatedQuality.toFloat(),
                iterations = iterations,
                elapsedMillis = System.currentTimeMillis() - startTime,
                estimatedQuality = estimatedQuality,
                searchRange = searchRange
              )
            )
          }

          // Binary search in narrow range around estimate
          while (lo <= hi && iterations < 6) { // Limit max iterations
            iterations++
            val midQuality = (lo + hi) / 2
            val candidateBytes = encodeWebP(workingBitmap, midQuality)
            val candidateSize = candidateBytes.size

            // Update best result if this is closer to target and within limit
            if (candidateSize <= targetBytes && candidateSize <= bestBytes.size) {
              bestQuality = midQuality
              bestBytes = candidateBytes
            }

            // Check if we hit our target tolerance
            if (candidateSize in (targetBytes - tolerance)..(targetBytes + tolerance)) {
              return@withContext CompressedImage(
                bytes = candidateBytes,
                originalSize = originalSize,
                compressedSize = candidateBytes.size,
                metadata = CompressionMetadata(
                  effectiveQualityPercent = midQuality.toFloat(),
                  iterations = iterations,
                  elapsedMillis = System.currentTimeMillis() - startTime,
                  estimatedQuality = estimatedQuality,
                  searchRange = searchRange
                )
              )
            }

            // Adjust search range
            if (candidateSize > targetBytes) {
              hi = midQuality - 1 // Need more compression (lower quality)
            } else {
              lo = midQuality + 1 // Can use less compression (higher quality)
            }
          }

          // Return best result found
          CompressedImage(
            bytes = bestBytes,
            originalSize = originalSize,
            compressedSize = bestBytes.size,
            metadata = CompressionMetadata(
              effectiveQualityPercent = bestQuality.toFloat(),
              iterations = iterations,
              elapsedMillis = System.currentTimeMillis() - startTime,
              estimatedQuality = estimatedQuality,
              searchRange = searchRange
            )
          )
        }
      }

      result
    } finally {
      workingBitmap.recycle()
    }
  }
}
