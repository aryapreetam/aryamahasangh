package com.aryamahasangh.gallery.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.aryamahasangh.ui.components.buttons.SimpleSubmitButton
import com.aryamahasangh.ui.components.buttons.StatefulSubmitButton
import kotlin.random.Random

/**
 * Gallery screen showcasing different button components
 */
@Composable
fun ButtonsGalleryScreen(
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    contentPadding = PaddingValues(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    item {
      Text(
        text = "Button Components",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    }

    item {
      ComponentShowcase(
        title = "Stateful Submit Button",
        description = "A button that shows different states: initial, submitting, success, and error"
      ) {
        StatefulSubmitButtonDemo()
      }
    }

    item {
      ComponentShowcase(
        title = "Simple Submit Button",
        description = "A simpler version with automatic error handling"
      ) {
        SimpleSubmitButtonDemo()
      }
    }
  }
}

/**
 * Card container for showcasing a component with title and description
 */
@Composable
fun ComponentShowcase(
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      content()
    }
  }
}

/**
 * Demo showcasing the StatefulSubmitButton with different scenarios
 */
@Composable
fun StatefulSubmitButtonDemo() {
  var counter by remember { mutableStateOf(0) }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Always successful (green on success):",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface
    )

    StatefulSubmitButton(
      text = "डेटा संग्रहित करें", // "Save Data" in pure Hindi
      onSubmit = {
        delay(1000) // Shorter delay to see result faster
        Result.success(Unit)
      },
      fillMaxWidth = false
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Always fails (red on error):",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface
    )

    StatefulSubmitButton(
      text = "त्रुटि परीक्षण", // "Error Test" in pure Hindi
      onSubmit = {
        delay(1000)
        Result.failure(Exception("परीक्षण त्रुटि")) // "Test error" in pure Hindi
      },
      fillMaxWidth = false
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Random result (success/error):",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface
    )

    StatefulSubmitButton(
      text = "रैंडम परीक्षण", // "Random Test" in pure Hindi
      onSubmit = {
        delay(1500)
        counter++
        if (Random.nextBoolean()) {
          Result.success(Unit)
        } else {
          Result.failure(Exception("परीक्षण त्रुटि")) // "Test error" in pure Hindi
        }
      },
      successText = "उत्तम!", // "Excellent!" in pure Hindi/Sanskrit
      errorText = "पुनः प्रयास करें", // "Try again" in pure Hindi/Sanskrit
      fillMaxWidth = false
    )

    Text(
      text = "Clicked: $counter times",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

/**
 * Demo showcasing the SimpleSubmitButton
 */
@Composable
fun SimpleSubmitButtonDemo() {
  var submitCount by remember { mutableStateOf(0) }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Simple version:",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface
    )

    SimpleSubmitButton(
      text = "फॉर्म प्रस्तुत करें", // "Submit Form" in pure Hindi
      onSubmit = {
        delay(1000)
        submitCount++
      },
      fillMaxWidth = false
    )

    Text(
      text = "Submitted: $submitCount times",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
