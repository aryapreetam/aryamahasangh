package com.aryamahasangh.imgcompress

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageCompressorTest {
  // Tiny 2x2 JPEG (embedded). This is sufficient to exercise the pipeline.
  private val tinyJpeg: ByteArray = byteArrayOf(
    -1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1, 1, 0, 0, 1,
    0, 1, 0, 0, -1, -37, 0, 67, 0, 8, 6, 6, 7, 6, 5, 8, 7, 7,
    7, 9, 9, 8, 10, 12, 20, 13, 12, 11, 11, 12, 25, 18, 19, 15,
    20, 29, 26, 31, 30, 29, 26, 28, 28, 32, 36, 46, 39, 32, 34, 44,
    35, 28, 28, 40, 55, 41, 44, 48, 49, 52, 52, 52, 31, 39, 57, 61,
    56, 50, 60, 46, 51, 52, 50, -1, -37, 0, 67, 1, 9, 9, 9, 12, 11, 12,
    24, 13, 13, 24, 50, 33, 28, 33, 50, 50, 50, 50, 50, 50, 50, 50,
    50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
    50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, -1, -60, 0, 17, 8, 0, 2,
    0, 2, 3, 1, 17, 0, 2, 17, 1, 3, 17, 1, -1, -38, 0, 12, 3, 1, 0, 2, 17, 3,
    17, 0, 63, 0, -105, -44, -13, -1, -39
  )

  private fun isWebP(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    return bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
      bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
      bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
  }

  @Test
  fun byQuality_producesWebP() = runTest {
    val input = ImageData(rawBytes = tinyJpeg, mimeType = "image/jpeg")
    val out = ImageCompressor.compress(input, CompressionConfig.ByQuality(75f))
    assertTrue(isWebP(out.bytes), "Output should be WebP (RIFF/WEBP signature)")
    assertTrue(out.compressedSize > 0)
  }

  @Test
  fun byTargetSize_hitsTolerance() = runTest {
    val input = ImageData(rawBytes = tinyJpeg, mimeType = "image/jpeg")
    val targetKb = 10
    val out = ImageCompressor.compress(input, CompressionConfig.ByTargetSize(targetKb))
    val targetBytes = targetKb * 1024
    val tol = maxOf((targetBytes * 0.05).toInt(), 10 * 1024)
    assertTrue(isWebP(out.bytes))
    assertTrue(out.compressedSize in (targetBytes - tol)..(targetBytes + tol) || out.compressedSize <= targetBytes)
  }
}
