package com.aryamahasangh.features.activities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryamahasangh.utils.WithTooltip

/**
 * Filter button with count badge component
 */
@Composable
fun FilterButton(
  filterCount: Int,
  isActive: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  WithTooltip(
    tooltip = if (filterCount > 0) "फिल्टर ($filterCount लागू)" else "फिल्टर"
  ) {
    Box(
      modifier = modifier
        .semantics { contentDescription = "filter_button" }
        .testTag("filter_button")
    ) {
      IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = if (isActive)
            MaterialTheme.colorScheme.primary
          else
            MaterialTheme.colorScheme.onSurface
        )
      ) {
        Icon(
          imageVector = Icons.Default.FilterList,
          contentDescription = "फिल्टर",
          modifier = Modifier.size(24.dp)
        )
      }

      // Count badge
      if (filterCount > 0) {
        Badge(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = 4.dp)
            .semantics { contentDescription = "filter_count_badge" }
            .testTag("filter_count_badge"),
          containerColor = MaterialTheme.colorScheme.primary
        ) {
          Text(
            text = if (filterCount > 99) "99+" else filterCount.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimary
          )
        }
      }
    }
  }
}
