package com.aryamahasangh.imgcompress

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
    val start = System.currentTimeMillis()
    val originalSize = input.rawBytes.size

    fun decodeBitmap(bytes: ByteArray): Bitmap {
      val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
      return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        ?: throw IllegalArgumentException("Unable to decode input image")
    }

    fun resizeIfNeeded(bm: Bitmap): Bitmap {
      val maxEdge = resize.maxLongEdgePx ?: return bm
      val w = bm.width
      val h = bm.height
      val longEdge = max(w, h)
      if (resize.downscaleOnly && longEdge <= maxEdge) return bm
      val scale = maxEdge.toFloat() / longEdge.toFloat()
      val newW = (w * scale).toInt().coerceAtLeast(1)
      val newH = (h * scale).toInt().coerceAtLeast(1)
      return Bitmap.createScaledBitmap(bm, newW, newH, true)
    }

    fun encodeWebP(bm: Bitmap, quality: Int): ByteArray {
      val stream = java.io.ByteArrayOutputStream()
      val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
      bm.compress(fmt, quality.coerceIn(0, 100), stream)
      return stream.toByteArray()
    }

    val decoded = decodeBitmap(input.rawBytes)
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
            return@withContext CompressedImage(
              bytes = resultBytes,
              originalSize = originalSize,
              compressedSize = resultBytes.size,
              metadata = CompressionMetadata(usedQuality, iterations, System.currentTimeMillis() - start)
            )
          }
          if (size > targetBytes) hi = mid - 1 else lo = mid + 1
        }
        usedQuality = bestQ.toFloat()
        resultBytes = bestBytes
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
