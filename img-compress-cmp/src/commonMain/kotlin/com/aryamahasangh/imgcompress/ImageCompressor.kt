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
)

data class CompressedImage(
  val bytes: ByteArray,
  val originalSize: Int,
  val compressedSize: Int,
  val mimeType: String = "image/webp",
  val metadata: CompressionMetadata? = null,
)

expect object ImageCompressor {
  suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions = ResizeOptions(maxLongEdgePx = 2560),
  ): CompressedImage
}
