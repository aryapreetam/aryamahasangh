package com.aryamahasangh.gallery.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Home screen of the UI components gallery showing list of all available components
 */
@Composable
fun GalleryHomeScreen(
  onNavigateToButtons: () -> Unit,
  modifier: Modifier = Modifier
) {
  val componentCategories = listOf(
    ComponentCategory(
      name = "Buttons", // "Buttons" in Hindi
      description = "Different types of buttons", // "Various types of button components" in Hindi
      icon = Icons.Default.SmartButton,
      onClick = onNavigateToButtons
    )
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    contentPadding = PaddingValues(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
          text = "UI Components Gallery", // Changed to English
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Collection of reusable UI components", // Changed to English
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }

    items(componentCategories) { category ->
      ComponentCategoryCard(
        category = category,
        onClick = category.onClick
      )
    }
  }
}

/**
 * Data class representing a component category
 */
data class ComponentCategory(
  val name: String,
  val description: String,
  val icon: ImageVector,
  val onClick: () -> Unit
)

/**
 * Card component for displaying a component category
 */
@Composable
fun ComponentCategoryCard(
  category: ComponentCategory,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 2.dp,
      pressedElevation = 4.dp
    )
  ) {
    ListItem(
      headlineContent = {
        Text(
          text = category.name,
          style = MaterialTheme.typography.titleMedium
        )
      },
      supportingContent = {
        Text(
          text = category.description,
          style = MaterialTheme.typography.bodyMedium
        )
      },
      leadingContent = {
        Icon(
          imageVector = category.icon,
          contentDescription = category.name,
          tint = MaterialTheme.colorScheme.primary
        )
      }
    )
  }
}
