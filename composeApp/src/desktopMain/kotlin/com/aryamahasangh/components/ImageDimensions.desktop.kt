package com.aryamahasangh.components

import com.aryamahasangh.utils.logger
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Get image dimensions from byte array on desktop platform using Java ImageIO
 */
actual fun getImageDimensions(bytes: ByteArray): Pair<Int, Int>? {
  return try {
    val inputStream = ByteArrayInputStream(bytes)
    val image: BufferedImage = ImageIO.read(inputStream)
    inputStream.close()

    if (image != null) {
      Pair(image.width, image.height)
    } else {
      null
    }
  } catch (e: Exception) {
    logger.error(e) { "Failed to get image dimensions on desktop" }
    null
  }
}
