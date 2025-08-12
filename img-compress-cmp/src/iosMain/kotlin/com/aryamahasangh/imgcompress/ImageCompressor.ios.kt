@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.aryamahasangh.imgcompress

import cocoapods.libwebp.WebPEncodeRGBA
import cocoapods.libwebp.WebPFree
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.dataWithBytes
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIImage
import kotlin.math.max

actual object ImageCompressor {
  actual suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions
  ): CompressedImage = withContext(Dispatchers.Default) {
    val startTime = NSDate().timeIntervalSince1970
    val originalSize = input.rawBytes.size

    // Optimized NSData creation from ByteArray
    fun nsDataFromBytes(bytes: ByteArray): NSData = bytes.usePinned {
      NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong())
    }

    // Decode UIImage only once
    fun decodeUIImageOnce(bytes: ByteArray): UIImage {
      val data = nsDataFromBytes(bytes)
      return UIImage(data = data) ?: throw IllegalArgumentException("Unable to decode input image: ${input.mimeType}")
    }

    // Optimized resizing with better memory management
    fun resizeIfNeeded(sourceImage: UIImage): UIImage {
      val maxEdge = resize.maxLongEdgePx ?: return sourceImage
      val originalSize = sourceImage.size
      val width = originalSize.useContents { width }
      val height = originalSize.useContents { height }
      val longEdge = max(width, height)
      
      if (resize.downscaleOnly && longEdge <= maxEdge.toDouble()) {
        return sourceImage
      }
      
      val scale = maxEdge.toDouble() / longEdge
      val newWidth = (width * scale).toInt()
      val newHeight = (height * scale).toInt()

      val colorSpace = CGColorSpaceCreateDeviceRGB()
      val bytesPerPixel = 4
      val bytesPerRow = newWidth * bytesPerPixel
      val data = nativeHeap.allocArray<UByteVar>(bytesPerRow * newHeight)
      
      val context = CGBitmapContextCreate(
        data,
        newWidth.convert(),
        newHeight.convert(),
        8u,
        bytesPerRow.convert(),
        colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
      ) ?: run {
        // Cleanup on error
        CGColorSpaceRelease(colorSpace)
        nativeHeap.free(data)
        throw RuntimeException("Failed to create bitmap context for resizing")
      }
      
      val cgImage = sourceImage.CGImage ?: run {
        CGColorSpaceRelease(colorSpace)
        CGContextRelease(context)
        nativeHeap.free(data)
        throw RuntimeException("No CGImage available for resizing")
      }
      
      CGContextDrawImage(context, CGRectMake(0.0, 0.0, newWidth.toDouble(), newHeight.toDouble()), cgImage)
      val scaledCGImage = CGBitmapContextCreateImage(context) ?: run {
        CGColorSpaceRelease(colorSpace)
        CGContextRelease(context)
        nativeHeap.free(data)
        throw RuntimeException("Failed to create resized CGImage")
      }
      
      val resizedImage = UIImage.imageWithCGImage(scaledCGImage)
      
      // Explicit cleanup
      CGColorSpaceRelease(colorSpace)
      CGContextRelease(context)
      CGImageRelease(scaledCGImage)
      nativeHeap.free(data)
      
      return resizedImage
    }

    // Optimized WebP encoding with proper error handling
    fun encodeWebP(image: UIImage, quality: Int): ByteArray {
      val cgImage = image.CGImage ?: throw RuntimeException("No CGImage available for encoding")
      val width = CGImageGetWidth(cgImage).toInt()
      val height = CGImageGetHeight(cgImage).toInt()
      val bytesPerPixel = 4
      val bytesPerRow = width * bytesPerPixel
      val colorSpace = CGColorSpaceCreateDeviceRGB()
      val data = nativeHeap.allocArray<UByteVar>(bytesPerRow * height)
      
      val context = CGBitmapContextCreate(
        data,
        width.convert(),
        height.convert(),
        8u,
        bytesPerRow.convert(),
        colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
      ) ?: run {
        CGColorSpaceRelease(colorSpace)
        nativeHeap.free(data)
        throw RuntimeException("Failed to create bitmap context for encoding")
      }
      
      CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cgImage)

      val resultBytes: ByteArray = memScoped {
        val outputPtr = alloc<CPointerVar<UByteVar>>()
        val sizeULong: ULong = WebPEncodeRGBA(
          data, width, height, bytesPerRow, quality.toFloat(), outputPtr.ptr
        )
        
        if (sizeULong == 0uL) {
          // Cleanup before error
          CGColorSpaceRelease(colorSpace)
          CGContextRelease(context)
          nativeHeap.free(data)
          throw RuntimeException("WebP encoding failed at quality $quality")
        }
        
        val size = sizeULong.toInt()
        val bytes = outputPtr.value!!.readBytes(size)
        WebPFree(outputPtr.value)
        bytes
      }

      // Explicit cleanup
      CGColorSpaceRelease(colorSpace)
      CGContextRelease(context)
      nativeHeap.free(data)
      
      return resultBytes
    }

    // Get image dimensions for analytics
    fun getImageDimensions(image: UIImage): Pair<Int, Int> {
      val size = image.size
      return Pair(size.useContents { width.toInt() }, size.useContents { height.toInt() })
    }

    // Decode and resize only once
    val sourceImage = decodeUIImageOnce(input.rawBytes)
    val originalDimensions = getImageDimensions(sourceImage)
    val workingImage = resizeIfNeeded(sourceImage)
    val finalDimensions = getImageDimensions(workingImage)

    val result = when (config) {
      is CompressionConfig.ByQuality -> {
        // Simple quality-based compression - single encoding
        val quality = config.qualityPercent.coerceIn(0f, 100f).toInt()
        val resultBytes = encodeWebP(workingImage, quality)

        val elapsedMs = ((NSDate().timeIntervalSince1970 - startTime) * 1000).toLong()
        CompressedImage(
          bytes = resultBytes,
          originalSize = originalSize,
          compressedSize = resultBytes.size,
          mimeType = "image/webp",
          metadata = CompressionMetadata(
            effectiveQualityPercent = quality.toFloat(),
            iterations = 1,
            elapsedMillis = elapsedMs,
            engineUsed = "iOS"
          )
        )
      }

      is CompressionConfig.ByTargetSize -> {
        val targetBytes = max(1, config.targetSizeKb) * 1024

        // Ultra-conservative quality prediction to ensure we NEVER go below target size
        val compressionRatio = targetBytes.toDouble() / originalSize
        val predictedQuality = when {
          compressionRatio > 0.7 -> 90   // Very light compression
          compressionRatio > 0.5 -> 82   // Light compression
          compressionRatio > 0.3 -> 72   // Moderate compression
          compressionRatio > 0.2 -> 62   // Good compression
          compressionRatio > 0.1 -> 52   // Heavy compression
          compressionRatio > 0.05 -> 35  // Very heavy compression
          else -> 25                      // Extreme compression
        }.let { baseQuality ->
          // Add very strong conservative bias for iOS
          val conservativeBias = when {
            compressionRatio < 0.05 -> +12  // Extreme compression - very strong bias
            compressionRatio < 0.15 -> +15  // Very aggressive compression - very strong bias
            compressionRatio < 0.3 -> +12   // Moderate compression - strong bias
            else -> +8                      // Light compression - good bias
          }
          (baseQuality + conservativeBias).coerceIn(20, 95)
        }

        // First attempt
        val firstBytes = encodeWebP(workingImage, predictedQuality)
        var iterations = 1
        var finalQuality = predictedQuality
        var finalBytes = firstBytes

        // Target size accuracy with wider tolerance
        val tolerance = max(35 * 1024, targetBytes / 15)
        val minAcceptable = targetBytes // Never go below target
        val maxAcceptable = targetBytes + tolerance

        // Triple-safety correction logic like Android
        if (firstBytes.size !in minAcceptable..maxAcceptable) {
          if (firstBytes.size > maxAcceptable) {
            // Too large - reduce quality carefully
            val sizeRatio = firstBytes.size.toDouble() / targetBytes
            val qualityReduction = when {
              sizeRatio > 2.0 -> 0.60  // Very large, cut by 40%
              sizeRatio > 1.5 -> 0.70  // Large, cut by 30%
              else -> 0.80             // Moderate, cut by 20%
            }
            val lowerQuality = (predictedQuality * qualityReduction).toInt().coerceAtLeast(8)
            val secondBytes = encodeWebP(workingImage, lowerQuality)
            iterations++

            if (secondBytes.size >= targetBytes) {
              finalQuality = lowerQuality
              finalBytes = secondBytes
            } else {
              // If went below target, use original (prefer overshooting)
              finalQuality = predictedQuality
              finalBytes = firstBytes
            }
          } else {
            // Too small - be very aggressive in quality increase
            val sizeRatio = targetBytes.toDouble() / firstBytes.size
            val qualityIncrease = when {
              sizeRatio > 2.0 -> 1.80  // Much too small, very big increase
              sizeRatio > 1.5 -> 1.60  // Too small, big increase
              sizeRatio > 1.2 -> 1.40  // Moderately small, good increase
              else -> 1.25             // Slightly small, moderate increase
            }
            val higherQuality = (predictedQuality * qualityIncrease).toInt().coerceAtMost(95)
            val secondBytes = encodeWebP(workingImage, higherQuality)
            iterations++

            // If still below target, make one final aggressive attempt
            if (secondBytes.size < targetBytes && iterations < 3) {
              val finalQuality3 = kotlin.math.min(95, higherQuality + 15)  // Add 15 more quality points
              val thirdBytes = encodeWebP(workingImage, finalQuality3)
              iterations++

              if (thirdBytes.size >= targetBytes) {
                finalQuality = finalQuality3
                finalBytes = thirdBytes
              } else {
                // Use the best result we have (closest to target)
                finalQuality = higherQuality
                finalBytes = secondBytes
              }
            } else {
              finalQuality = higherQuality
              finalBytes = secondBytes
            }
          }
        }

        val elapsedMs = ((NSDate().timeIntervalSince1970 - startTime) * 1000).toLong()

        CompressedImage(
          bytes = finalBytes,
          originalSize = originalSize,
          compressedSize = finalBytes.size,
          mimeType = "image/webp",
          metadata = CompressionMetadata(
            effectiveQualityPercent = finalQuality.toFloat(),
            iterations = iterations,
            elapsedMillis = elapsedMs,
            estimatedQuality = predictedQuality,
            searchRange = IntRange(
              kotlin.math.min(predictedQuality, finalQuality),
              kotlin.math.max(predictedQuality, finalQuality)
            ),
            engineUsed = "iOS"
          )
        )
      }
    }

    result
  }
}
