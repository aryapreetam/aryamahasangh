package com.aryamahasangh.util

actual suspend fun kmpCompressImage(
    bytes: ByteArray,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int
): ByteArray = bytes // desktop no-op
