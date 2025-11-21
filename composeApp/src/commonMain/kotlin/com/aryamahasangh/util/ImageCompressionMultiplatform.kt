package com.aryamahasangh.util

/**
 * Multiplatform wrapper for image compression used only on iOS in current usage.
 * Other platforms provide a no-op implementation to satisfy expect/actual without pulling in unsupported APIs (e.g. WASM).
 */
expect suspend fun kmpCompressImage(
    bytes: ByteArray,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int
): ByteArray
