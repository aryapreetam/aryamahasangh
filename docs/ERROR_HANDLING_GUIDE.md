# Enhanced Error Handling Guide

This document explains how to use the enhanced error handling system in the AryaMahasangh app.

## Overview

The app now uses a robust error handling system that provides contextual error messages to users, helping them
understand what went wrong and how to fix it.

## Key Components

### 1. AppError Types

The `AppError` sealed class provides specific error types:

- `NetworkError.NoConnection` - No internet connection
- `NetworkError.Timeout` - Request timeout
- `NetworkError.ServerError` - Server issues
- `NetworkError.HttpError` - HTTP errors with codes
- `ValidationError` - Input validation errors
- `AuthError` - Authentication/authorization errors
- `DataError` - Data parsing/database errors
- `BusinessError` - Business logic errors

### 2. Error Components

#### ErrorContent

Full-screen error display with contextual suggestions:

```kotlin
ErrorContent(
  error = appError,
  onRetry = { /* retry action */ },
  onDismiss = { /* dismiss action */ }
)
```

#### InlineErrorMessage

Compact error display for forms/cards:

```kotlin
InlineErrorMessage(
  error = appError,
  onRetry = { /* retry action */ }
)
```

#### ErrorSnackbar

Shows errors as snackbars:

```kotlin
ErrorSnackbar(
  error = appError,
  snackbarHostState = snackbarHostState,
  onRetry = { /* retry action */ },
  onDismiss = { /* dismiss action */ }
)
```

#### LoadingErrorState

Handles loading/error/content states:

```kotlin
LoadingErrorState(
  isLoading = uiState.isLoading,
  error = uiState.appError,
  onRetry = { /* retry action */ }
) {
  // Content when loaded successfully
}
```

### 3. ViewModel Error Handling

#### UI State Interface

Implement `ErrorState` in your UI state:

```kotlin
data class MyUiState(
  val data: List<Item>? = null,
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null
) : ErrorState
```

#### Using handleResult Extension

```kotlin
fun loadData() {
  launch {
    repository.getData().collect { result ->
      result.handleResult(
        onLoading = {
          updateState { it.copy(isLoading = true, error = null, appError = null) }
        },
        onSuccess = { data ->
          updateState {
            it.copy(
              data = data,
              isLoading = false,
              error = null,
              appError = null
            )
          }
        },
        onError = { appError ->
          ErrorHandler.logError(appError, "MyViewModel.loadData")
          updateState {
            it.copy(
              isLoading = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        }
      )
    }
  }
}
```

## Error Messages Provided to Users

### Network Errors

#### No Connection

- **Title**: "No Internet Connection"
- **Description**: "Please check your internet connection and try again."
- **Suggestions**:
    - Check if Wi-Fi or mobile data is enabled
    - Try connecting to a different network
    - Restart your router if using Wi-Fi

#### Timeout

- **Title**: "Connection Timeout"
- **Description**: "The request took too long to complete."
- **Suggestions**:
    - Check your internet speed
    - Try again in a few moments
    - Switch to a more stable network

#### Server Error

- **Title**: "Server Error"
- **Description**: "Our servers are temporarily unavailable."
- **Suggestions**:
    - Please try again in a few minutes
    - Check our social media for updates
    - Contact support if the issue persists

## Migration Guide

### For existing ViewModels:

1. Add AppError support to UI state:

```kotlin
data class MyUiState(
  // existing fields...
  override val appError: AppError? = null
) : ErrorState
```

2. Replace manual error handling with `handleResult`:

```kotlin
// Before
when (result) {
  is Result.Error -> {
    updateState { it.copy(error = result.message) }
  }
}

// After  
result.handleResult(
  onError = { appError ->
    ErrorHandler.logError(appError, "Context")
    updateState {
      it.copy(
        error = appError.getUserMessage(),
        appError = appError
      )
    }
  }
)
```

3. Add error clearing methods:

```kotlin
fun clearError() {
  updateState { it.copy(error = null, appError = null) }
}
```

### For existing Screens:

Replace manual error handling with error components:

```kotlin
// Before
if (uiState.error != null) {
  Text("Error: ${uiState.error}")
}

// After
LoadingErrorState(
  isLoading = uiState.isLoading,
  error = uiState.appError,
  onRetry = { viewModel.retry() }
) {
  // Main content
}
```

## Best Practices

1. **Always log errors** with context using `ErrorHandler.logError()`
2. **Provide clear retry actions** for recoverable errors
3. **Use appropriate error components** based on UI needs
4. **Clear errors** when appropriate (on retry, dismiss, etc.)
5. **Test error scenarios** to ensure good user experience
