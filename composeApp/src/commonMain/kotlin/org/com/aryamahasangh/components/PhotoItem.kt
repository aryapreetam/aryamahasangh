package com.aryamahasangh.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.core.PlatformFile

@Composable
fun PhotoItem(
  file: PlatformFile,
  onRemoveFile: (PlatformFile) -> Unit
) {
  var bytes by remember(file) { mutableStateOf<ByteArray?>(null) }
  var showName by remember { mutableStateOf(false) }

  LaunchedEffect(file) {
    bytes =
      if (file.supportsStreams()) {
        val size = file.getSize()
        if (size != null && size > 0L) {
          val buffer = ByteArray(size.toInt())
          val tmpBuffer = ByteArray(1000)
          var totalBytesRead = 0
          file.getStream().use {
            while (it.hasBytesAvailable()) {
              val numRead = it.readInto(tmpBuffer, 1000)
              tmpBuffer.copyInto(
                buffer,
                destinationOffset = totalBytesRead,
                endIndex = numRead
              )
              totalBytesRead += numRead
            }
          }
          buffer
        } else {
          file.readBytes()
        }
      } else {
        file.readBytes()
      }
  }

  Surface(
    onClick = { showName = !showName },
    modifier =
      Modifier
        .aspectRatio(1f)
        .clip(shape = MaterialTheme.shapes.medium)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      bytes?.let {
        AsyncImage(
          bytes,
          contentDescription = file.name,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .fillMaxSize()
        )
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
      ) {
        IconButton(
          onClick = { onRemoveFile(file) },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            Icons.Default.Close,
            modifier = Modifier.size(22.dp),
            contentDescription = "Save",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      AnimatedVisibility(
        visible = showName,
        modifier = Modifier.padding(4.dp).align(Alignment.BottomStart)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
          shape = MaterialTheme.shapes.small
        ) {
          Text(
            file.name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }
  }
}
