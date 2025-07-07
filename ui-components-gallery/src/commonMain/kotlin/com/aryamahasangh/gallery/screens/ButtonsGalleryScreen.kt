package com.aryamahasangh.gallery.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.aryamahasangh.ui.components.buttons.*
import kotlin.random.Random

/**
 * Gallery screen showcasing the perfect SubmitButton component
 * This is like Storybook for our SubmitButton - showcasing all states and configurations
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
        text = "SubmitButton Gallery",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Perfect submit button with bulletproof architecture",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Built-in Gallery from the SubmitButton component
    item {
      ComponentShowcase(
        title = "🎭 All States Showcase",
        description = "Demonstrates all possible button states in static display",
        modifier = Modifier,
        fillMaxWidth = true,
        content = {
          SubmitButtonGallery()
        }
      )
    }

    // Interactive examples in FlowRow
    item {
      Text(
        text = "Interactive Demos",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(16.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        ComponentShowcase(
          title = "✅ Always Successful",
          description = "Button that always succeeds",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            AlwaysSuccessfulDemo()
          }
        )

        ComponentShowcase(
          title = "❌ Always Fails",
          description = "Button that always fails",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            AlwaysFailsDemo()
          }
        )

        ComponentShowcase(
          title = "🎲 Random Success/Failure",
          description = "50/50 chance of success",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            RandomOutcomeDemo()
          }
        )

        ComponentShowcase(
          title = "⚡ Fast Operations",
          description = "Quick operations",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            FastOperationsDemo()
          }
        )

        ComponentShowcase(
          title = "🐌 Slow Operations",
          description = "Slow operations",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            SlowOperationsDemo()
          }
        )

        ComponentShowcase(
          title = "✋ Validation Testing",
          description = "Button with validation logic",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            ValidationDemo()
          }
        )

        ComponentShowcase(
          title = "🎨 Custom Styling",
          description = "Custom colors and texts",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            CustomStylingDemo()
          }
        )

        ComponentShowcase(
          title = "🌈 Beautiful Color Schemes",
          description = "Eye-pleasing color palettes",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            ColorSchemesDemo()
          }
        )

        ComponentShowcase(
          title = "🔄 Retry Functionality",
          description = "Error states with retry",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            RetryFunctionalityDemo()
          }
        )

        ComponentShowcase(
          title = "🌍 Internationalization",
          description = "Different languages",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            InternationalizationDemo()
          }
        )

        ComponentShowcase(
          title = "📊 Analytics Integration",
          description = "Comprehensive callback tracking",
          modifier = Modifier,
          fillMaxWidth = false,
          content = {
            AnalyticsDemo()
          }
        )
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
  fillMaxWidth: Boolean = false,
  content: @Composable () -> Unit
) {
  Card(
    modifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier,
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

// ================================================================================================
// DEMO IMPLEMENTATIONS
// ================================================================================================

@Composable
fun AlwaysSuccessfulDemo() {
  var successCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "डेटा संग्रहित करें",
      onSubmit = {
        delay(1000)
        successCount++
      },
      config = SubmitButtonConfig(fillMaxWidth = false),
      callbacks = object : SubmitCallbacks {
        override fun onSuccess() {
          println("Gallery: Success! Count: $successCount")
        }
      }
    )

    Text(
      text = "सफलताएं: $successCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun AlwaysFailsDemo() {
  var failureCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "त्रुटि परीक्षण",
      onSubmit = {
        delay(1000)
        failureCount++
        throw Exception("परीक्षण त्रुटि")
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        showRetryOnError = false
      ),
      callbacks = object : SubmitCallbacks {
        override fun onError(error: SubmissionError) {
          println("Gallery: Error - ${error.toUserMessage()}")
        }
      }
    )

    Text(
      text = "त्रुटियां: $failureCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun RandomOutcomeDemo() {
  var attemptCount by remember { mutableStateOf(0) }
  var successCount by remember { mutableStateOf(0) }
  var failureCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "रैंडम परीक्षण",
      onSubmit = {
        delay(1500)
        attemptCount++
        if (Random.nextBoolean()) {
          successCount++
        } else {
          failureCount++
          throw Exception("यादृच्छिक त्रुटि")
        }
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        texts = SubmitButtonTexts(
          successText = "उत्तम!",
          errorText = "पुनः प्रयास करें"
        )
      )
    )

    Text(
      text = "प्रयास: $attemptCount | सफल: $successCount | असफल: $failureCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun FastOperationsDemo() {
  var operationCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "तत्काल ऑपरेशन",
      onSubmit = {
        delay(200) // Very fast
        operationCount++
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        successDuration = 800L
      )
    )

    Text(
      text = "तेज़ ऑपरेशन: $operationCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun SlowOperationsDemo() {
  var operationCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "धीमा ऑपरेशन",
      onSubmit = {
        delay(3000) // Very slow
        operationCount++
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        texts = SubmitButtonTexts(
          submittingText = "कृपया प्रतीक्षा करें...",
          successText = "पूर्ण!"
        )
      )
    )

    Text(
      text = "धीमे ऑपरेशन: $operationCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun ValidationDemo() {
  var isValid by remember { mutableStateOf(true) }
  var validSubmissions by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
      Checkbox(
        checked = isValid,
        onCheckedChange = { isValid = it }
      )
      Text("डेटा वैध है")
    }

    SubmitButton(
      text = "सत्यापन सहित प्रस्तुत करें",
      onSubmit = {
        delay(1000)
        validSubmissions++
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        validator = {
          if (!isValid) SubmissionError.ValidationFailed else null
        }
      )
    )

    Text(
      text = "वैध प्रस्तुतियां: $validSubmissions",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun CustomStylingDemo() {
  var customCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "कस्टम स्टाइल",
      onSubmit = {
        delay(1000)
        customCount++
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false, // Natural button width
        texts = SubmitButtonTexts(
          submittingText = "जादू बन रहा है...",
          successText = "जादू पूरा!",
          errorText = "जादू असफल!"
        ),
        colors = SubmitButtonColors(
          idleContainer = Color(0xFF6A4C93),
          idleContent = Color.White,
          successContainer = Color(0xFF00C896),
          errorContainer = Color(0xFFFF6B6B)
        )
      )
    )

    Text(
      text = "कस्टम ऑपरेशन: $customCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun ColorSchemesDemo() {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // Default colors
    Text("Default Colors:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SubmitButton(
        text = "सफल",
        onSubmit = { delay(500) },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.default(),
          successDuration = 2000L
        )
      )
      SubmitButton(
        text = "त्रुटि",
        onSubmit = {
          delay(500)
          throw Exception("डेमो त्रुटि")
        },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.default(),
          showRetryOnError = false
        )
      )
    }

    // Vibrant colors
    Text("Vibrant Colors:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SubmitButton(
        text = "सफल",
        onSubmit = { delay(500) },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.vibrant(),
          successDuration = 2000L
        )
      )
      SubmitButton(
        text = "त्रुटि",
        onSubmit = {
          delay(500)
          throw Exception("डेमो त्रुटि")
        },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.vibrant(),
          showRetryOnError = false
        )
      )
    }

    // Soft colors
    Text("Soft Colors:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SubmitButton(
        text = "सफल",
        onSubmit = { delay(500) },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.soft(),
          successDuration = 2000L
        )
      )
      SubmitButton(
        text = "त्रुटि",
        onSubmit = {
          delay(500)
          throw Exception("डेमो त्रुटि")
        },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.soft(),
          showRetryOnError = false
        )
      )
    }

    // Professional colors
    Text("Professional Colors:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SubmitButton(
        text = "सफल",
        onSubmit = { delay(500) },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.professional(),
          successDuration = 2000L
        )
      )
      SubmitButton(
        text = "त्रुटि",
        onSubmit = {
          delay(500)
          throw Exception("डेमो त्रुटि")
        },
        config = SubmitButtonConfig(
          fillMaxWidth = false,
          colors = SubmitButtonColors.professional(),
          showRetryOnError = false
        )
      )
    }
  }
}

@Composable
fun RetryFunctionalityDemo() {
  var attemptCount by remember { mutableStateOf(0) }
  var retryCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "रिट्राई डेमो",
      onSubmit = {
        delay(1000)
        attemptCount++
        throw Exception("रिट्राई की आवश्यकता")
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        showRetryOnError = true,
        errorDuration = 0L // Keep error state for retry
      ),
      callbacks = object : SubmitCallbacks {
        override fun onRetry() {
          retryCount++
        }
      }
    )

    Text(
      text = "प्रयास: $attemptCount | रिट्राई: $retryCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun InternationalizationDemo() {
  var language by remember { mutableStateOf("hindi") }
  var submitCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
      Text("भाषा: ")
      Spacer(modifier = Modifier.width(8.dp))
      Row {
        Button(
          onClick = { language = "hindi" },
          colors = if (language == "hindi") ButtonDefaults.buttonColors()
          else ButtonDefaults.outlinedButtonColors()
        ) {
          Text("हिंदी", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = { language = "english" },
          colors = if (language == "english") ButtonDefaults.buttonColors()
          else ButtonDefaults.outlinedButtonColors()
        ) {
          Text("English", style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    SubmitButton(
      text = if (language == "hindi") "प्रस्तुत करें" else "Submit",
      onSubmit = {
        delay(1000)
        submitCount++
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        texts = if (language == "hindi")
          SubmitButtonTexts.default()
        else
          SubmitButtonTexts.english()
      )
    )

    Text(
      text = if (language == "hindi") "प्रस्तुतियां: $submitCount" else "Submissions: $submitCount",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun AnalyticsDemo() {
  var analytics by remember { mutableStateOf("") }
  var successCount by remember { mutableStateOf(0) }
  var errorCount by remember { mutableStateOf(0) }
  var retryCount by remember { mutableStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SubmitButton(
      text = "एनालिटिक्स ट्रैकिंग",
      onSubmit = {
        delay(1500)
        if (Random.nextInt(3) == 0) {
          errorCount++
          throw Exception("एनालिटिक्स त्रुटि")
        }
        successCount++
      },
      config = SubmitButtonConfig(fillMaxWidth = false),
      callbacks = object : SubmitCallbacks {
        override fun onSuccess() {
          analytics = "✅ Success tracked ($successCount successes)"
        }

        override fun onError(error: SubmissionError) {
          analytics = "❌ Error tracked: ${error.toUserMessage()} ($errorCount errors)"
        }

        override fun onRetry() {
          retryCount++
          analytics = "🔄 Retry tracked ($retryCount retries)"
        }
      }
    )

    if (analytics.isNotEmpty()) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Text(
          text = analytics,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
