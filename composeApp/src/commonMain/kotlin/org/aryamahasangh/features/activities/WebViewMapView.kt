package org.aryamahasangh.features.activities

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Common interface for WebView-based map implementation
 */
@Composable
expect fun WebViewMap(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier = Modifier
)
