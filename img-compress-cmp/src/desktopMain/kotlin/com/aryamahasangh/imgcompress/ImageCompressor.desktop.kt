package com.aryamahasangh.imgcompress

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import kotlin.math.max

actual object ImageCompressor {
  actual suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions
  ): CompressedImage = withContext(Dispatchers.Default) {
    val startTime = System.currentTimeMillis()
    val originalSize = input.rawBytes.size

    // Optimized image loading - happens ONCE
    fun loadImageOnce(bytes: ByteArray): Image {
      return Image.makeFromEncoded(bytes)
        ?: throw IllegalArgumentException("Unable to decode input image: ${input.mimeType}")
    }

    // Fast resizing with performance-optimized Skia settings
    fun resizeIfNeeded(sourceImage: Image): Image {
      val maxEdge = resize.maxLongEdgePx ?: return sourceImage
      val width = sourceImage.width
      val height = sourceImage.height
      val longEdge = max(width, height)

      if (resize.downscaleOnly && longEdge <= maxEdge) {
        return sourceImage
      }

      val scale = maxEdge.toFloat() / longEdge.toFloat()
      val newWidth = max(1, (width * scale).toInt())
      val newHeight = max(1, (height * scale).toInt())

      val surface = Surface.makeRasterN32Premul(newWidth, newHeight)
      val canvas = surface.canvas
      canvas.clear(Color.TRANSPARENT)

      // OPTIMIZED: Performance-focused paint settings
      val paint = Paint().apply {
        isAntiAlias = false        // Faster for large downscales
      }

      val srcRect = Rect.makeWH(width.toFloat(), height.toFloat())
      val dstRect = Rect.makeWH(newWidth.toFloat(), newHeight.toFloat())
      canvas.drawImageRect(sourceImage, srcRect, dstRect, paint)

      return surface.makeImageSnapshot()
    }

    // Optimized WebP encoding with error handling
    fun encodeWebP(image: Image, quality: Int): ByteArray {
      val data = image.encodeToData(EncodedImageFormat.WEBP, quality.coerceIn(0, 100))
        ?: throw RuntimeException("Skia failed to encode WebP at quality $quality")
      return data.bytes
    }

    // CRITICAL FIX: Load image only once
    val sourceImage = loadImageOnce(input.rawBytes)
    val workingImage = resizeIfNeeded(sourceImage)

    val result = when (config) {
      is CompressionConfig.ByQuality -> {
        // Simple quality-based compression - single encoding
        val quality = config.qualityPercent.coerceIn(0f, 100f).toInt()
        val resultBytes = encodeWebP(workingImage, quality)

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
        var bestBytes = encodeWebP(workingImage, estimatedQuality) // Start with estimate
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
          val candidateBytes = encodeWebP(workingImage, midQuality)
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
  }
}
