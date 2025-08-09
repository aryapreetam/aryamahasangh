package com.aryamahasangh.imgcompress

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@JsFun(
  "function(b64, mime, quality, maxLongEdgePx, cb){\n" +
    "  const bin = atob(b64);\n" +
    "  const len = bin.length;\n" +
    "  const view = new Uint8Array(len);\n" +
    "  for (let i=0;i<len;i++) view[i] = bin.charCodeAt(i);\n" +
    "  const buf = view.buffer;\n" +
    "  (async () => {\n" +
    "    const blob = new Blob([buf], {type: mime});\n" +
    "    const bitmap = await createImageBitmap(blob);\n" +
    "    let w = bitmap.width; let h = bitmap.height;\n" +
    "    if (maxLongEdgePx !== null && maxLongEdgePx !== undefined) {\n" +
    "      const longEdge = Math.max(w, h);\n" +
    "      if (longEdge > maxLongEdgePx) {\n" +
    "        const scale = maxLongEdgePx / longEdge;\n" +
    "        w = Math.max(1, Math.round(w * scale));\n" +
    "        h = Math.max(1, Math.round(h * scale));\n" +
    "      }\n" +
    "    }\n" +
    "    const canvas = new OffscreenCanvas(w, h);\n" +
    "    const ctx = canvas.getContext('2d');\n" +
    "    ctx.drawImage(bitmap, 0, 0, w, h);\n" +
    "    const outBlob = await canvas.convertToBlob({type: 'image/webp', quality: (quality/100)});\n" +
    "    const outArr = new Uint8Array(await outBlob.arrayBuffer());\n" +
    "    let outBin = '';\n" +
    "    for (let i=0;i<outArr.length;i++) outBin += String.fromCharCode(outArr[i]);\n" +
    "    const outB64 = btoa(outBin);\n" +
    "    cb(outB64);\n" +
    "  })();\n" +
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
    val start = nowMs().toLong()
    val originalSize = input.rawBytes.size

    suspend fun encodeAtQuality(q: Int): ByteArray = suspendCancellableCoroutine { cont ->
      val b64 = Base64.encode(input.rawBytes)
      jsEncodeWebPBase64(
        b64 = b64,
        mime = input.mimeType,
        quality = q.coerceIn(0, 100),
        maxLongEdgePx = resize.maxLongEdgePx
      ) { outB64 ->
        val out = Base64.decode(outB64)
        cont.resume(out)
      }
    }

    val result: ByteArray
    var usedQ: Float? = null
    var iterations = 0

    when (config) {
      is CompressionConfig.ByQuality -> {
        val q = config.qualityPercent.coerceIn(0f, 100f).toInt()
        result = encodeAtQuality(q)
        usedQ = q.toFloat()
        iterations = 1
      }

      is CompressionConfig.ByTargetSize -> {
        val targetBytes = max(1, config.targetSizeKb) * 1024
        val tolerance = max((targetBytes * 0.05).toInt(), 10 * 1024)
        var lo = 5
        var hi = 95
        var bestQ = 95
        var best = encodeAtQuality(bestQ)
        iterations = 0
        while (lo <= hi) {
          iterations++
          val mid = (lo + hi) / 2
          val bytes = encodeAtQuality(mid)
          val size = bytes.size
          if (size <= targetBytes && size <= best.size) {
            bestQ = mid; best = bytes
          }
          if (size in (targetBytes - tolerance)..(targetBytes + tolerance)) {
            usedQ = mid.toFloat()
            result = bytes
            val elapsed = nowMs().toLong() - start
            return CompressedImage(
              bytes = result,
              originalSize = originalSize,
              compressedSize = result.size,
              metadata = CompressionMetadata(usedQ, iterations, elapsed)
            )
          }
          if (size > targetBytes) hi = mid - 1 else lo = mid + 1
        }
        usedQ = bestQ.toFloat()
        result = best
      }
    }

    val elapsed = nowMs().toLong() - start
    return CompressedImage(
      bytes = result,
      originalSize = originalSize,
      compressedSize = result.size,
      metadata = CompressionMetadata(usedQ, iterations, elapsed)
    )
  }
}
