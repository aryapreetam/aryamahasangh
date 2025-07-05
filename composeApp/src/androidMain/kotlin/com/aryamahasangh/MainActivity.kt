package com.aryamahasangh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinceglb.filekit.core.FileKit
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      navigationBarStyle =
        SystemBarStyle.light(
          AndroidColor.TRANSPARENT,
          AndroidColor.TRANSPARENT
        )
    )

    // Initialize FileKit
    FileKit.init(this)

    setContent {
      App()
    }
  }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}
