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

    // CRITICAL FIX: Decode and resize only once
    val sourceImage = decodeUIImageOnce(input.rawBytes)
    val workingImage = resizeIfNeeded(sourceImage)

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
          metadata = CompressionMetadata(
            effectiveQualityPercent = quality.toFloat(),
            iterations = 1,
            elapsedMillis = elapsedMs
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
        val elapsedMs = { ((NSDate().timeIntervalSince1970 - startTime) * 1000).toLong() }
        if (bestBytes.size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
          return@withContext CompressedImage(
            bytes = bestBytes,
            originalSize = originalSize,
            compressedSize = bestBytes.size,
            metadata = CompressionMetadata(
              effectiveQualityPercent = estimatedQuality.toFloat(),
              iterations = iterations,
              elapsedMillis = elapsedMs(),
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
                elapsedMillis = elapsedMs(),
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
            elapsedMillis = elapsedMs(),
            estimatedQuality = estimatedQuality,
            searchRange = searchRange
          )
        )
      }
    }

    result
  }
}
