package com.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.getUserMessage

@Composable
fun ErrorContent(
  error: AppError?,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      val errorInfo = getErrorInfo(error)

      Icon(
        imageVector = errorInfo.icon,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(48.dp)
      )

      Text(
        text = errorInfo.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Text(
        text = errorInfo.description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      if (errorInfo.suggestions.isNotEmpty()) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "💡 आप यह कोशिश कर सकते हैं:",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            errorInfo.suggestions.forEach { suggestion ->
              Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (onDismiss != null) Arrangement.spacedBy(8.dp) else Arrangement.Center
      ) {
        if (onDismiss != null) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("कुछ बाद में")
          }
        }

        if (onRetry != null) {
          FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.weight(if (onDismiss != null) 1f else 2f)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("पुनः प्रयास करें")
          }
        }
      }
    }
  }
}

@Composable
fun InlineErrorMessage(
  error: AppError?,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null
) {
  if (error == null) return

  val errorInfo = getErrorInfo(error)

  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = errorInfo.icon,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = errorInfo.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = errorInfo.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (onRetry != null) {
        FilledTonalButton(
          onClick = onRetry,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("पुनः प्रयास करें", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
fun ErrorSnackbar(
  error: AppError?,
  snackbarHostState: SnackbarHostState,
  onRetry: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null
) {
  error?.let {
    val errorInfo = getErrorInfo(it)

    LaunchedEffect(error) {
      val result =
        snackbarHostState.showSnackbar(
          message = "${errorInfo.title}: ${errorInfo.description}",
          actionLabel = if (onRetry != null) "पुनः प्रयास करें" else null,
          duration = SnackbarDuration.Long
        )

      when (result) {
        SnackbarResult.ActionPerformed -> onRetry?.invoke()
        SnackbarResult.Dismissed -> onDismiss?.invoke()
      }
    }
  }
}

@Composable
fun LoadingErrorState(
  isLoading: Boolean,
  error: AppError?,
  onRetry: () -> Unit,
  loadingContent: @Composable () -> Unit = {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "लोड हो रहा है...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  },
  content: @Composable () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    when {
      isLoading -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          loadingContent()
        }
      }
      error != null -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          ErrorContent(
            error = error,
            onRetry = onRetry
          )
        }
      }
      else -> content()
    }
  }
}

private data class ErrorInfo(
  val title: String,
  val description: String,
  val suggestions: List<String>,
  val icon: ImageVector
)

private fun getErrorInfo(error: AppError?): ErrorInfo {
  return when (error) {
    is AppError.CrudError.Success ->
      ErrorInfo(
        title = "सफल",
        description = error.getLocalizedMessage(),
        suggestions = emptyList(),
        icon = Icons.Default.Info
      )

    is AppError.CrudError.Error ->
      ErrorInfo(
        title = "त्रुटि",
        description = error.getLocalizedMessage(),
        suggestions = listOf(
          "🔄 कृपया पुनः प्रयास करें",
          "📞 यदि समस्या बनी रहे तो सहायता से संपर्क करें"
        ),
        icon = Icons.Default.Warning
      )

    is AppError.NetworkError.NoConnection ->
      ErrorInfo(
        title = "इंटरनेट कनेक्शन नहीं",
        description = "ऐसा लगता है कि आप इंटरनेट से जुड़े नहीं हैं।",
        suggestions =
          listOf(
            "📶 जांचें कि Wi-Fi या मोबाइल डेटा चालू है",
            "🔄 Wi-Fi और मोबाइल डेटा के बीच स्विच करने का प्रयास करें",
            "📍 बेहतर सिग्नल वाले क्षेत्र में जाएं",
            "🔌 अपना राउटर पुनः चालू करें या Wi-Fi से पुनः कनेक्ट करें"
          ),
        icon = Icons.Default.SignalWifiOff
      )

    is AppError.NetworkError.Timeout ->
      ErrorInfo(
        title = "अपेक्षा से अधिक समय लग रहा",
        description = "कनेक्शन धीमा है या सर्वर व्यस्त है।",
        suggestions =
          listOf(
            "⏱️ कुछ देर प्रतीक्षा करें और पुनः प्रयास करें",
            "📶 अपनी इंटरनेट स्पीड जांचें",
            "🔄 यदि उपलब्ध हो तो तेज़ नेटवर्क पर स्विच करें",
            "📱 इंटरनेट का उपयोग करने वाले अन्य ऐप्स बंद करें"
          ),
        icon = Icons.Default.CloudOff
      )

    is AppError.NetworkError.ServerError ->
      ErrorInfo(
        title = "सेवा अस्थायी रूप से अनुपलब्ध",
        description = "हमारे सर्वर में अभी कुछ समस्याएं आ रही हैं।",
        suggestions =
          listOf(
            "⏰ कृपया कुछ मिनट बाद पुनः प्रयास करें",
            "🔔 हम इसे जल्दी ठीक करने के लिए काम कर रहे हैं",
            "📞 यदि यह जारी रहे तो सहायता से संपर्क करें",
            "📱 अपडेट के लिए हमारे सोशल मीडिया देखें"
          ),
        icon = Icons.Default.CloudOff
      )

    is AppError.NetworkError.HttpError ->
      ErrorInfo(
        title =
          when (error.code) {
            404 -> "सामग्री नहीं मिली"
            500, 502, 503 -> "सेवा अस्थायी रूप से बंद"
            else -> "सेवा त्रुटि"
          },
        description =
          when (error.code) {
            404 -> "आपकी खोजी गई जानकारी उपलब्ध नहीं है।"
            500, 502, 503 -> "हमारे सर्वर में अभी समस्या आ रही है।"
            else -> "हमारी तरफ से कुछ गलत हुआ है।"
          },
        suggestions =
          when (error.code) {
            404 -> listOf("🔍 कुछ और खोजने का प्रयास करें", "🏠 मुख्य पेज पर वापस जाएं")
            500, 502, 503 ->
              listOf(
                "⏰ कृपया कुछ मिनट बाद पुनः प्रयास करें",
                "🔄 पेज को रिफ्रेश करें",
                "📞 यदि समस्या बनी रहे तो सहायता से संपर्क करें"
              )
            else -> listOf("🔄 कृपया पुनः प्रयास करें", "📞 यदि आवश्यक हो तो सहायता से संपर्क करें")
          },
        icon = Icons.Default.Info
      )

    is AppError.ValidationError ->
      ErrorInfo(
        title = "इनपुट समस्या",
        description = error.message,
        suggestions = listOf("✏️ कृपया अपना इनपुट जांचें और पुनः प्रयास करें"),
        icon = Icons.Default.Info
      )

    is AppError.AuthError ->
      ErrorInfo(
        title = "प्रमाणीकरण आवश्यक",
        description = error.getUserMessage(),
        suggestions =
          when (error) {
            is AppError.AuthError.NotAuthenticated -> listOf("🔑 जारी रखने के लिए कृपया लॉगिन करें")
            is AppError.AuthError.SessionExpired -> listOf("🔄 कृपया पुनः लॉगिन करें")
            else -> listOf("🔑 कृपया अपनी लॉगिन जानकारी जांचें")
          },
        icon = Icons.Default.Info
      )

    is AppError.DataError ->
      ErrorInfo(
        title = "डेटा समस्या",
        description = error.getUserMessage(),
        suggestions =
          listOf(
            "🔄 कृपया पुनः प्रयास करें",
            "📞 यदि यह जारी रहे तो सहायता से संपर्क करें"
          ),
        icon = Icons.Default.Warning
      )

    is AppError.BusinessError ->
      ErrorInfo(
        title = "कार्य अनुमतित नहीं",
        description = error.getUserMessage(),
        suggestions = listOf("ℹ️ कृपया अपनी अनुमतियां जांचें"),
        icon = Icons.Default.Info
      )

    else ->
      ErrorInfo(
        title = "कुछ गलत हुआ",
        description = error?.getUserMessage() ?: "एक अप्रत्याशित समस्या आई।",
        suggestions =
          listOf(
            "🔄 कृपया पुनः प्रयास करें",
            "📞 यदि यह जारी रहे तो सहायता से संपर्क करें"
          ),
        icon = Icons.Default.Info
      )
  }
}
