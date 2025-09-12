package com.aryamahasangh.imgcompress

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object ImageCompressor {
  actual suspend fun compress(
    input: ImageData,
    config: CompressionConfig,
    resize: ResizeOptions
  ): CompressedImage = withContext(Dispatchers.Default) {
    // Stubbed encoder for tests when native interop is disabled.
    // Returns original bytes and marks mime as webp to satisfy API contracts.
    CompressedImage(
      bytes = input.rawBytes,
      originalSize = input.rawBytes.size,
      compressedSize = input.rawBytes.size,
      mimeType = "image/webp",
      metadata = CompressionMetadata(
        effectiveQualityPercent = when (config) {
          is CompressionConfig.ByQuality -> config.qualityPercent
          is CompressionConfig.ByTargetSize -> 75f
        },
        iterations = 0,
        elapsedMillis = 0,
        engineUsed = "iOS-Stub"
      )
    )
  }
}
