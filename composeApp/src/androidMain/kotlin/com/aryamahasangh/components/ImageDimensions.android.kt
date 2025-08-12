package com.aryamahasangh.components

import android.graphics.BitmapFactory
import com.aryamahasangh.utils.logger

/**
 * Get actual image dimensions from byte array using Android BitmapFactory
 */
actual fun getImageDimensions(bytes: ByteArray): Pair<Int, Int>? {
  return try {
    val options = BitmapFactory.Options().apply {
      inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth > 0 && options.outHeight > 0) {
      Pair(options.outWidth, options.outHeight)
    } else {
      null
    }
  } catch (e: Exception) {
    logger.error(e) { "Failed to get image dimensions" }
    null
  }
}
