package com.aryamahasangh.features.activities.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.aryamahasangh.features.activities.ActivityFilterOption

/**
 * Combined search and filter row component
 */
@Composable
fun SearchAndFilterRow(
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  isSearching: Boolean,
  selectedFilters: Set<ActivityFilterOption>,
  isFilterDropdownOpen: Boolean,
  onFilterButtonClick: () -> Unit,
  onClearFilters: () -> Unit,
  onFilterToggle: (ActivityFilterOption) -> Unit,
  onDismissDropdown: () -> Unit,
  modifier: Modifier = Modifier
) {
  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  Row(
    modifier = modifier
      .fillMaxWidth()
      .semantics { contentDescription = "search_and_filter_row" }
      .testTag("search_and_filter_row"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Search field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier.weight(1f),
      placeholder = { Text("गतिविधि खोजें") },
      leadingIcon = if (searchQuery.isEmpty()) {
        { Icon(Icons.Default.Search, contentDescription = "Search") }
      } else null,
      trailingIcon = {
        if (isSearching) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
          )
        } else if (searchQuery.isNotEmpty()) {
          // Close icon that when clicked, clears the input field
          IconButton(onClick = { onSearchChange("") }) {
            Icon(
              Icons.Default.Close,
              contentDescription = "हटाएँ",
              modifier = Modifier.size(20.dp)
            )
          }
        } else {
          null
        }
      },
      singleLine = true
    )

    // Filter controls
    FilterRow(
      selectedFilters = selectedFilters,
      isDropdownOpen = isFilterDropdownOpen,
      onFilterButtonClick = onFilterButtonClick,
      onClearFilters = onClearFilters,
      onFilterToggle = onFilterToggle,
      onDismissDropdown = onDismissDropdown
    )
  }
}
