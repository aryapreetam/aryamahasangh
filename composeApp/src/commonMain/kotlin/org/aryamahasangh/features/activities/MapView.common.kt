package org.aryamahasangh.features.activities

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier = Modifier
)
