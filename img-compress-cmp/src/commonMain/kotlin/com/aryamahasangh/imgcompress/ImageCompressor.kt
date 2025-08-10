package com.aryamahasangh.imgcompress

sealed class CompressionConfig {
  data class ByQuality(val qualityPercent: Float) : CompressionConfig()
  data class ByTargetSize(val targetSizeKb: Int) : CompressionConfig()
}

/** Optional resizing prior to compression to improve ratios and UX across desktop and mobile. */
data class ResizeOptions(
  val maxLongEdgePx: Int? = 2560,
  val downscaleOnly: Boolean = true,
  val maintainAspectRatio: Boolean = true,
)

data class ImageData(
  val rawBytes: ByteArray,
  val mimeType: String,
)

data class CompressionMetadata(
  val effectiveQualityPercent: Float?,
  val iterations: Int,
  val elapsedMillis: Long,
  val estimatedQuality: Int? = null,  // Quality predictor estimate
  val searchRange: IntRange? = null,  // Binary search range used
)

data class CompressedImage(
  val bytes: ByteArray,
  val originalSize: Int,
  val compressedSize: Int,
  val mimeType: String = "image/webp",
  val metadata: CompressionMetadata? = null,
)

/**
 * Smart quality estimation to reduce binary search iterations from 6+ to 1-2.
 * Based on compression ratios and image characteristics.
 */
object QualityPredictor {
  /**
   * Predicts optimal quality based on target compression ratio.
   * @param originalSizeBytes Original image size in bytes
   * @param targetSizeKb Target size in KB
   * @return Estimated quality (5-95 range)
   */
  fun predictOptimalQuality(originalSizeBytes: Int, targetSizeKb: Int): Int {
    val targetBytes = targetSizeKb * 1024
    val compressionRatio = targetBytes.toDouble() / originalSizeBytes

    return when {
      compressionRatio > 0.8 -> 95  // Light compression needed
      compressionRatio > 0.6 -> 88  // Moderate compression  
      compressionRatio > 0.4 -> 78  // Good compression
      compressionRatio > 0.25 -> 65 // Heavy compression
      compressionRatio > 0.15 -> 50 // Aggressive compression
      compressionRatio > 0.10 -> 35 // Very aggressive
      else -> 25                     // Maximum compression
    }.coerceIn(5, 95)
  }

  /**
   * Calculate optimal binary search range around predicted quality.
   * @param predictedQuality The predicted optimal quality
   * @return IntRange for binary search (typically ±15 from prediction)
   */
  fun getSearchRange(predictedQuality: Int): IntRange {
    val range = 15 // ±15 quality points around prediction
    val lo = (predictedQuality - range).coerceAtLeast(5)
    val hi = (predictedQuality + range).coerceAtMost(95)
    return lo..hi
  }
}

expect object ImageCompressor {
  suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions = ResizeOptions(maxLongEdgePx = 2560),
  ): CompressedImage
}
