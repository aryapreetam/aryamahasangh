package com.aryamahasangh.features.activities.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aryamahasangh.utils.WithTooltip

/**
 * Clear filters button component
 */
@Composable
fun ClearFiltersButton(
  isVisible: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (isVisible) {
    WithTooltip(
      tooltip = "फिल्टर हटाएं"
    ) {
      IconButton(
        onClick = onClick,
        modifier = modifier
          .semantics { contentDescription = "clear_filters_button" }
          .testTag("clear_filters_button"),
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.error
        )
      ) {
        Icon(
          imageVector = Icons.Default.Clear,
          contentDescription = "फिल्टर हटाएं",
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
