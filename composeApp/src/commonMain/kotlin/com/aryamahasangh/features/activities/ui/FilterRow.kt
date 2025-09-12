package com.aryamahasangh.features.activities.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.activities.ActivityFilterOption

/**
 * Filter row component containing filter button and clear button
 */
@Composable
fun FilterRow(
  selectedFilters: Set<ActivityFilterOption>,
  isDropdownOpen: Boolean,
  onFilterButtonClick: () -> Unit,
  onClearFilters: () -> Unit,
  onFilterToggle: (ActivityFilterOption) -> Unit,
  onDismissDropdown: () -> Unit,
  modifier: Modifier = Modifier
) {
  val hasActiveFilters = selectedFilters.isNotEmpty() &&
    !selectedFilters.contains(ActivityFilterOption.ShowAll)
  val filterCount = if (hasActiveFilters) selectedFilters.size else 0

  Row(
    modifier = modifier
      .semantics { contentDescription = "filter_row" }
      .testTag("filter_row"),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Filter button with popup dropdown
    Box {
      FilterButton(
        filterCount = filterCount,
        isActive = hasActiveFilters || isDropdownOpen,
        onClick = onFilterButtonClick
      )

      // Filter dropdown as popup menu (anchored to the button)
      DropdownMenu(
        expanded = isDropdownOpen,
        onDismissRequest = onDismissDropdown,
        modifier = Modifier
          .semantics { contentDescription = "filter_dropdown_menu" }
          .testTag("filter_dropdown_menu")
      ) {
        // Filter options as menu items
        ActivityFilterOption.getAllOptions().forEach { filterOption ->
          val isSelected = selectedFilters.contains(filterOption)

          // No options should ever be disabled - only check state changes
          // All checkboxes are always enabled for user interaction

          DropdownMenuItem(
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .semantics { contentDescription = "filter_option_${filterOption.displayName}" }
                  .testTag("filter_option_${filterOption.displayName}")
              ) {
                Checkbox(
                  checked = isSelected,
                  onCheckedChange = { onFilterToggle(filterOption) },
                  enabled = true, // Always enabled
                  colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                  )
                )

                Text(
                  text = filterOption.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface, // Always full opacity
                  modifier = Modifier.padding(horizontal = 4.dp)
                )
              }
            },
            onClick = { onFilterToggle(filterOption) },
            enabled = true // Always enabled
          )
        }
      }
    }

    // Clear filters button (only visible when filters are active)
    ClearFiltersButton(
      isVisible = hasActiveFilters,
      onClick = onClearFilters
    )
  }
}
