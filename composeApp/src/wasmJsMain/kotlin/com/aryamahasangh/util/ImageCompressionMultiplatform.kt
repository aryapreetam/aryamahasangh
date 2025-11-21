package com.aryamahasangh.util

// WASM target: compression lib not available. Return original bytes.
actual suspend fun kmpCompressImage(
    bytes: ByteArray,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int
): ByteArray = bytes
