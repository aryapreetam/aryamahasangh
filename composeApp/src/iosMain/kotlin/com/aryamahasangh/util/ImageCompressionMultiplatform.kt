package com.aryamahasangh.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.compressImage

actual suspend fun kmpCompressImage(
    bytes: ByteArray,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int
): ByteArray = FileKit.compressImage(
    bytes = bytes,
    quality = quality,
    maxWidth = maxWidth,
    maxHeight = maxHeight,
    imageFormat = ImageFormat.JPEG
)
