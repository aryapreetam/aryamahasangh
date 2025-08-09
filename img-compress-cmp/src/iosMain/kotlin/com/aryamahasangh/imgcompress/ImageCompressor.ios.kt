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
    val start = NSDate().timeIntervalSince1970
    val originalSize = input.rawBytes.size

    fun nsDataFromBytes(bytes: ByteArray): NSData = bytes.usePinned {
      NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong())
    }

    fun decodeUIImage(bytes: ByteArray): UIImage {
      val data = nsDataFromBytes(bytes)
      return UIImage(data = data) ?: error("Unable to decode image data")
    }

    fun resizeIfNeeded(image: UIImage): UIImage {
      val maxEdge = resize.maxLongEdgePx ?: return image
      val w = image.size.useContents { width }
      val h = image.size.useContents { height }
      val longEdge = max(w, h)
      if (resize.downscaleOnly && longEdge <= maxEdge.toDouble()) return image
      val scale = maxEdge.toDouble() / longEdge
      val newW = (w * scale)
      val newH = (h * scale)

      val colorSpace = CGColorSpaceCreateDeviceRGB()
      val bytesPerPixel = 4
      val newWidth = newW.toInt()
      val newHeight = newH.toInt()
      val bytesPerRow = newWidth * bytesPerPixel
      val raw = nativeHeap.allocArray<UByteVar>(bytesPerRow * newHeight)
      val ctx = CGBitmapContextCreate(
        raw,
        newWidth.convert(),
        newHeight.convert(),
        8.convert(),
        bytesPerRow.convert(),
        colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
      )
        ?: error("Failed to create CG context")
      val cgImg = image.CGImage ?: error("No CGImage")
      CGContextDrawImage(ctx, CGRectMake(0.0, 0.0, newW, newH), cgImg)
      val scaledCg = CGBitmapContextCreateImage(ctx) ?: error("Failed to snapshot image")
      val scaled = UIImage.imageWithCGImage(scaledCg)
      // free context resources
      CGColorSpaceRelease(colorSpace)
      CGContextRelease(ctx)
      CGImageRelease(scaledCg)
      nativeHeap.free(raw)
      return scaled
    }

    fun encodeWebP(uiImage: UIImage, quality: Int): ByteArray {
      val cg = uiImage.CGImage ?: error("No CGImage for encoding")
      val width = CGImageGetWidth(cg).toInt()
      val height = CGImageGetHeight(cg).toInt()
      val bytesPerPixel = 4
      val bytesPerRow = width * bytesPerPixel
      val colorSpace = CGColorSpaceCreateDeviceRGB()
      val data = nativeHeap.allocArray<UByteVar>(bytesPerRow * height)
      val ctx = CGBitmapContextCreate(
        data,
        width.convert(),
        height.convert(),
        8.convert(),
        bytesPerRow.convert(),
        colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
      )
        ?: error("Failed to create context for encode")
      CGContextDrawImage(ctx, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cg)

      val outBytes: ByteArray = memScoped {
        val outputPtr = alloc<CPointerVar<UByteVar>>()
        val sizeULong: ULong = WebPEncodeRGBA(data, width, height, bytesPerRow, quality.toFloat(), outputPtr.ptr)
        if (sizeULong == 0uL) {
          // cleanup before error
          CGColorSpaceRelease(colorSpace)
          CGContextRelease(ctx)
          nativeHeap.free(data)
          error("WebP encoding failed")
        }
        val size = sizeULong.toInt()
        val bytes = outputPtr.value!!.readBytes(size)
        WebPFree(outputPtr.value)
        bytes
      }

      CGColorSpaceRelease(colorSpace)
      CGContextRelease(ctx)
      nativeHeap.free(data)
      return outBytes
    }

    val uiImage = decodeUIImage(input.rawBytes)
    val prepared = resizeIfNeeded(uiImage)

    val resultBytes: ByteArray
    var usedQuality: Float? = null
    var iterations = 0

    when (config) {
      is CompressionConfig.ByQuality -> {
        val q = config.qualityPercent.coerceIn(0f, 100f).toInt()
        resultBytes = encodeWebP(prepared, q)
        usedQuality = q.toFloat()
        iterations = 1
      }

      is CompressionConfig.ByTargetSize -> {
        val targetBytes = max(1, config.targetSizeKb) * 1024
        val tolerance = max((targetBytes * 0.05).toInt(), 10 * 1024)
        var lo = 5
        var hi = 95
        var bestQ = 95
        var bestBytes = encodeWebP(prepared, bestQ)
        iterations = 0
        while (lo <= hi) {
          iterations++
          val mid = (lo + hi) / 2
          val bytes = encodeWebP(prepared, mid)
          val size = bytes.size
          if (size <= targetBytes && size <= bestBytes.size) {
            bestQ = mid
            bestBytes = bytes
          }
          if (size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
            usedQuality = mid.toFloat()
            resultBytes = bytes
            val elapsedMs = ((NSDate().timeIntervalSince1970 - start) * 1000).toLong()
            return@withContext CompressedImage(
              bytes = resultBytes,
              originalSize = originalSize,
              compressedSize = resultBytes.size,
              metadata = CompressionMetadata(usedQuality, iterations, elapsedMs)
            )
          }
          if (size > targetBytes) hi = mid - 1 else lo = mid + 1
        }
        usedQuality = bestQ.toFloat()
        resultBytes = bestBytes
      }
    }

    val elapsedMs = ((NSDate().timeIntervalSince1970 - start) * 1000).toLong()
    CompressedImage(
      bytes = resultBytes,
      originalSize = originalSize,
      compressedSize = resultBytes.size,
      metadata = CompressionMetadata(usedQuality, iterations, elapsedMs)
    )
  }
}
