package com.aryamahasangh.imgcompress

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Optimized JS function with better error handling
@JsFun(
  "function(b64, mime, quality, maxLongEdgePx, cb){\n" +
    "  try {\n" +
    "    const bin = atob(b64);\n" +
    "    const len = bin.length;\n" +
    "    const view = new Uint8Array(len);\n" +
    "    for (let i=0;i<len;i++) view[i] = bin.charCodeAt(i);\n" +
    "    const buf = view.buffer;\n" +
    "    \n" +
    "    (async () => {\n" +
    "      try {\n" +
    "        const blob = new Blob([buf], {type: mime});\n" +
    "        const bitmap = await createImageBitmap(blob);\n" +
    "        let w = bitmap.width; let h = bitmap.height;\n" +
    "        \n" +
    "        // Optional resizing\n" +
    "        if (maxLongEdgePx !== null && maxLongEdgePx !== undefined) {\n" +
    "          const longEdge = Math.max(w, h);\n" +
    "          if (longEdge > maxLongEdgePx) {\n" +
    "            const scale = maxLongEdgePx / longEdge;\n" +
    "            w = Math.max(1, Math.round(w * scale));\n" +
    "            h = Math.max(1, Math.round(h * scale));\n" +
    "          }\n" +
    "        }\n" +
    "        \n" +
    "        const canvas = new OffscreenCanvas(w, h);\n" +
    "        const ctx = canvas.getContext('2d');\n" +
    "        if (!ctx) throw new Error('Failed to get 2D context');\n" +
    "        \n" +
    "        ctx.drawImage(bitmap, 0, 0, w, h);\n" +
    "        const outBlob = await canvas.convertToBlob({\n" +
    "          type: 'image/webp',\n" +
    "          quality: (quality/100)\n" +
    "        });\n" +
    "        \n" +
    "        const outArr = new Uint8Array(await outBlob.arrayBuffer());\n" +
    "        let outBin = '';\n" +
    "        for (let i=0;i<outArr.length;i++) outBin += String.fromCharCode(outArr[i]);\n" +
    "        const outB64 = btoa(outBin);\n" +
    "        cb(outB64);\n" +
    "        \n" +
    "        // Cleanup\n" +
    "        bitmap.close();\n" +
    "      } catch (e) {\n" +
    "        cb('ERROR: ' + e.message);\n" +
    "      }\n" +
    "    })();\n" +
    "  } catch (e) {\n" +
    "    cb('ERROR: ' + e.message);\n" +
    "  }\n" +
    "}"
)
external fun jsEncodeWebPBase64(
  b64: String,
  mime: String,
  quality: Int,
  maxLongEdgePx: Int?,
  cb: (String) -> Unit
)

@JsFun("() => Date.now()")
external fun nowMs(): Double

actual object ImageCompressor {
  @OptIn(ExperimentalEncodingApi::class)
  actual suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions
  ): CompressedImage {
    val startTime = nowMs().toLong()
    val originalSize = input.rawBytes.size

    // Optimized encoding with error handling
    suspend fun encodeAtQuality(quality: Int): ByteArray = suspendCancellableCoroutine { cont ->
      val inputBase64 = Base64.encode(input.rawBytes)
      jsEncodeWebPBase64(
        b64 = inputBase64,
        mime = input.mimeType,
        quality = quality.coerceIn(0, 100),
        maxLongEdgePx = resize.maxLongEdgePx
      ) { resultBase64 ->
        if (resultBase64.startsWith("ERROR:")) {
          cont.resumeWith(Result.failure(RuntimeException(resultBase64.substring(7))))
        } else {
          val resultBytes = Base64.decode(resultBase64)
          cont.resume(resultBytes)
        }
      }
    }

    val result = when (config) {
      is CompressionConfig.ByQuality -> {
        // Simple quality-based compression - single encoding
        val quality = config.qualityPercent.coerceIn(0f, 100f).toInt()
        val resultBytes = encodeAtQuality(quality)

        CompressedImage(
          bytes = resultBytes,
          originalSize = originalSize,
          compressedSize = resultBytes.size,
          metadata = CompressionMetadata(
            effectiveQualityPercent = quality.toFloat(),
            iterations = 1,
            elapsedMillis = nowMs().toLong() - startTime
          )
        )
      }

      is CompressionConfig.ByTargetSize -> {
        // OPTIMIZED: Smart quality estimation + narrow binary search
        val targetBytes = max(1, config.targetSizeKb) * 1024
        val tolerance = max((targetBytes * 0.05).toInt(), 10 * 1024)

        // Smart quality prediction to reduce iterations from 6+ to 1-2
        val estimatedQuality = QualityPredictor.predictOptimalQuality(originalSize, config.targetSizeKb)
        val searchRange = QualityPredictor.getSearchRange(estimatedQuality)

        var lo = searchRange.first
        var hi = searchRange.last
        var bestQuality = estimatedQuality
        var bestBytes = encodeAtQuality(estimatedQuality) // Start with smart estimate
        var iterations = 1

        // Quick check if estimate is already good enough
        if (bestBytes.size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
          return CompressedImage(
            bytes = bestBytes,
            originalSize = originalSize,
            compressedSize = bestBytes.size,
            metadata = CompressionMetadata(
              effectiveQualityPercent = estimatedQuality.toFloat(),
              iterations = iterations,
              elapsedMillis = nowMs().toLong() - startTime,
              estimatedQuality = estimatedQuality,
              searchRange = searchRange
            )
          )
        }

        // Binary search in narrow range around estimate
        while (lo <= hi && iterations < 6) { // Limit max iterations for wasmJs performance
          iterations++
          val midQuality = (lo + hi) / 2
          val candidateBytes = encodeAtQuality(midQuality)
          val candidateSize = candidateBytes.size

          // Update best result if this is closer to target and within limit
          if (candidateSize <= targetBytes && candidateSize <= bestBytes.size) {
            bestQuality = midQuality
            bestBytes = candidateBytes
          }

          // Check if we hit our target tolerance
          if (candidateSize in (targetBytes - tolerance)..(targetBytes + tolerance)) {
            return CompressedImage(
              bytes = candidateBytes,
              originalSize = originalSize,
              compressedSize = candidateBytes.size,
              metadata = CompressionMetadata(
                effectiveQualityPercent = midQuality.toFloat(),
                iterations = iterations,
                elapsedMillis = nowMs().toLong() - startTime,
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
            elapsedMillis = nowMs().toLong() - startTime,
            estimatedQuality = estimatedQuality,
            searchRange = searchRange
          )
        )
      }
    }

    return result
  }
}
