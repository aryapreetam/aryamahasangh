package org.aryamahasangh.components

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
import org.aryamahasangh.domain.error.AppError

@Composable
fun ErrorContent(
  error: AppError?,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
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
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          )
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "💡 What you can try:",
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
            Text("Maybe Later")
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
            Text("Try Again")
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
    colors = CardDefaults.cardColors(
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
          Text("Retry", style = MaterialTheme.typography.labelMedium)
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
      val result = snackbarHostState.showSnackbar(
        message = "${errorInfo.title}: ${errorInfo.description}",
        actionLabel = if (onRetry != null) "Retry" else null,
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
        text = "Loading...",
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
    is AppError.NetworkError.NoConnection -> ErrorInfo(
      title = "No Internet Connection",
      description = "It looks like you're not connected to the internet.",
      suggestions = listOf(
        "📶 Check if Wi-Fi or mobile data is turned on",
        "🔄 Try switching between Wi-Fi and mobile data",
        "📍 Move to an area with better signal",
        "🔌 Restart your router or reconnect to Wi-Fi"
      ),
      icon = Icons.Default.SignalWifiOff
    )

    is AppError.NetworkError.Timeout -> ErrorInfo(
      title = "Taking Longer Than Expected",
      description = "The connection is slow or the server is busy.",
      suggestions = listOf(
        "⏱️ Wait a moment and try again",
        "📶 Check your internet speed",
        "🔄 Switch to a faster network if available",
        "📱 Close other apps using the internet"
      ),
      icon = Icons.Default.CloudOff
    )

    is AppError.NetworkError.ServerError -> ErrorInfo(
      title = "Service Temporarily Unavailable",
      description = "Our servers are experiencing some issues right now.",
      suggestions = listOf(
        "⏰ Please try again in a few minutes",
        "🔔 We're working to fix this quickly",
        "📞 Contact support if this continues",
        "📱 Check our social media for updates"
      ),
      icon = Icons.Default.CloudOff
    )

    is AppError.NetworkError.HttpError -> ErrorInfo(
      title = when (error.code) {
        404 -> "Content Not Found"
        500, 502, 503 -> "Service Temporarily Down"
        else -> "Service Error"
      },
      description = when (error.code) {
        404 -> "The information you're looking for is not available."
        500, 502, 503 -> "Our servers are having trouble right now."
        else -> "Something went wrong on our end."
      },
      suggestions = when (error.code) {
        404 -> listOf("🔍 Try searching for something else", "🏠 Go back to the main page")
        500, 502, 503 -> listOf(
          "⏰ Please try again in a few minutes",
          "🔄 Refresh the page",
          "📞 Contact support if this persists"
        )
        else -> listOf("🔄 Please try again", "📞 Contact support if needed")
      },
      icon = Icons.Default.Info
    )

    is AppError.ValidationError -> ErrorInfo(
      title = "Input Issue",
      description = error.message,
      suggestions = listOf("✏️ Please check your input and try again"),
      icon = Icons.Default.Info
    )

    is AppError.AuthError -> ErrorInfo(
      title = "Authentication Required",
      description = error.message,
      suggestions = when (error) {
        is AppError.AuthError.NotAuthenticated -> listOf("🔑 Please log in to continue")
        is AppError.AuthError.SessionExpired -> listOf("🔄 Please log in again")
        else -> listOf("🔑 Please check your login details")
      },
      icon = Icons.Default.Info
    )

    is AppError.DataError -> ErrorInfo(
      title = "Data Issue",
      description = error.message,
      suggestions = listOf(
        "🔄 Please try again",
        "📞 Contact support if this continues"
      ),
      icon = Icons.Default.Warning
    )

    is AppError.BusinessError -> ErrorInfo(
      title = "Action Not Allowed",
      description = error.message,
      suggestions = listOf("ℹ️ Please check your permissions"),
      icon = Icons.Default.Info
    )

    else -> ErrorInfo(
      title = "Something Went Wrong",
      description = error?.message ?: "An unexpected issue occurred.",
      suggestions = listOf(
        "🔄 Please try again",
        "📞 Contact support if this continues"
      ),
      icon = Icons.Default.Info
    )
  }
}
