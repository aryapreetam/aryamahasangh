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
    val start = System.currentTimeMillis()
    val originalSize = input.rawBytes.size

    fun loadImage(bytes: ByteArray): Image = Image.makeFromEncoded(bytes)

    fun resizeIfNeeded(img: Image): Image {
      val maxEdge = resize.maxLongEdgePx ?: return img
      val w = img.width
      val h = img.height
      val longEdge = max(w, h)
      if (resize.downscaleOnly && longEdge <= maxEdge) return img
      val scale = maxEdge.toFloat() / longEdge.toFloat()
      val newW = max(1, (w * scale).toInt())
      val newH = max(1, (h * scale).toInt())

      val surface = Surface.makeRasterN32Premul(newW, newH)
      val canvas = surface.canvas
      canvas.clear(Color.TRANSPARENT)
      val paint = Paint()
      paint.isAntiAlias = true
      val srcRect = Rect.makeWH(w.toFloat(), h.toFloat())
      val dstRect = Rect.makeWH(newW.toFloat(), newH.toFloat())
      canvas.drawImageRect(img, srcRect, dstRect, paint)
      return surface.makeImageSnapshot()
    }

    fun encodeWebP(img: Image, quality: Int): ByteArray {
      val data = img.encodeToData(EncodedImageFormat.WEBP, quality)
        ?: error("Skia failed to encode WebP")
      return data.bytes
    }

    val decoded = loadImage(input.rawBytes)
    val prepared = resizeIfNeeded(decoded)

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
        val tolerance = max((targetBytes * 0.05).toInt(), 10 * 1024) // ±5% or 10KB
        var lo = 5
        var hi = 95
        var best: Pair<Int, ByteArray> = 95 to encodeWebP(prepared, 95)
        iterations = 0

        while (lo <= hi) {
          iterations++
          val mid = (lo + hi) / 2
          val bytes = encodeWebP(prepared, mid)
          val size = bytes.size
          best = if (size <= targetBytes && size <= best.second.size) mid to bytes else best

          if (size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
            usedQuality = mid.toFloat()
            resultBytes = bytes
            return@withContext CompressedImage(
              bytes = resultBytes,
              originalSize = originalSize,
              compressedSize = resultBytes.size,
              metadata = CompressionMetadata(usedQuality, iterations, System.currentTimeMillis() - start)
            )
          }
          if (size > targetBytes) {
            // need more compression -> lower quality
            hi = mid - 1
          } else {
            // under target -> can increase quality
            lo = mid + 1
          }
        }
        usedQuality = best.first.toFloat()
        resultBytes = best.second
      }
    }

    CompressedImage(
      bytes = resultBytes,
      originalSize = originalSize,
      compressedSize = resultBytes.size,
      metadata = CompressionMetadata(usedQuality, iterations, System.currentTimeMillis() - start)
    )
  }
}
