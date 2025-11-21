package com.aryamahasangh.util

actual suspend fun kmpCompressImage(
    bytes: ByteArray,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int
): ByteArray = bytes // no-op placeholder (Android not using this path currently)
