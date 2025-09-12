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

    fun nsDataFromBytes(bytes: ByteArray): NSData = bytes.usePinned {
      NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong())
    }

    fun decodeUIImageOnce(bytes: ByteArray): UIImage {
      val data = nsDataFromBytes(bytes)
      return UIImage(data = data) ?: throw IllegalArgumentException("Unable to decode input image: ${input.mimeType}")
    }

    fun resizeIfNeeded(sourceImage: UIImage): UIImage {
      val maxEdge = resize.maxLongEdgePx ?: return sourceImage
      val originalSize = sourceImage.size
      val width = originalSize.useContents { width }
      val height = originalSize.useContents { height }
      val longEdge = max(width, height)
      if (resize.downscaleOnly && longEdge <= maxEdge.toDouble()) return sourceImage

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

      CGColorSpaceRelease(colorSpace)
      CGContextRelease(context)
      CGImageRelease(scaledCGImage)
      nativeHeap.free(data)

      return resizedImage
    }

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

      CGColorSpaceRelease(colorSpace)
      CGContextRelease(context)
      nativeHeap.free(data)
      return resultBytes
    }

    fun getImageDimensions(image: UIImage): Pair<Int, Int> {
      val size = image.size
      return Pair(size.useContents { width.toInt() }, size.useContents { height.toInt() })
    }

    val sourceImage = decodeUIImageOnce(input.rawBytes)
    val originalDimensions = getImageDimensions(sourceImage)
    val workingImage = resizeIfNeeded(sourceImage)
    val finalDimensions = getImageDimensions(workingImage)

    val result = when (config) {
      is CompressionConfig.ByQuality -> {
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
        val compressionRatio = targetBytes.toDouble() / originalSize
        val predictedQuality = when {
          compressionRatio > 0.7 -> 90
          compressionRatio > 0.5 -> 82
          compressionRatio > 0.3 -> 72
          compressionRatio > 0.2 -> 62
          compressionRatio > 0.1 -> 52
          compressionRatio > 0.05 -> 35
          else -> 25
        }
        val firstBytes = encodeWebP(workingImage, predictedQuality)
        var iterations = 1
        var finalQuality = predictedQuality
        var finalBytes = firstBytes
        val tolerance = max(35 * 1024, targetBytes / 15)
        val minAcceptable = targetBytes
        val maxAcceptable = targetBytes + tolerance
        if (firstBytes.size !in minAcceptable..maxAcceptable) {
          if (firstBytes.size > maxAcceptable) {
            val sizeRatio = firstBytes.size.toDouble() / targetBytes
            val qualityReduction = when {
              sizeRatio > 2.0 -> 0.60
              sizeRatio > 1.5 -> 0.70
              else -> 0.80
            }
            val lowerQuality = (predictedQuality * qualityReduction).toInt().coerceAtLeast(8)
            val secondBytes = encodeWebP(workingImage, lowerQuality)
            iterations++
            if (secondBytes.size >= targetBytes) {
              finalQuality = lowerQuality
              finalBytes = secondBytes
            } else {
              finalQuality = predictedQuality
              finalBytes = firstBytes
            }
          } else {
            val sizeRatio = targetBytes.toDouble() / firstBytes.size
            val qualityIncrease = when {
              sizeRatio > 2.0 -> 1.80
              sizeRatio > 1.5 -> 1.60
              sizeRatio > 1.2 -> 1.40
              else -> 1.25
            }
            val higherQuality = (predictedQuality * qualityIncrease).toInt().coerceAtMost(95)
            val secondBytes = encodeWebP(workingImage, higherQuality)
            iterations++
            if (secondBytes.size < targetBytes && iterations < 3) {
              val finalQuality3 = kotlin.math.min(95, higherQuality + 15)
              val thirdBytes = encodeWebP(workingImage, finalQuality3)
              iterations++
              if (thirdBytes.size >= targetBytes) {
                finalQuality = finalQuality3
                finalBytes = thirdBytes
              } else {
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
