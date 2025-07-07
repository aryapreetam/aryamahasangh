package com.aryamahasangh.ui.components.buttons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ================================================================================================
// PERFECT TYPE-SAFE ERROR HANDLING
// ================================================================================================

/**
 * Type-safe submission errors - no more string-based error handling
 */
sealed class SubmissionError {
  object ValidationFailed : SubmissionError()
  object NetworkError : SubmissionError()
  object UnknownError : SubmissionError()
  data class BusinessLogic(val code: String, val details: String) : SubmissionError()
  data class Custom(val message: String) : SubmissionError()

  fun toUserMessage(): String = when (this) {
    is ValidationFailed -> "कृपया सभी आवश्यक फील्ड भरें"
    is NetworkError -> "नेटवर्क त्रुटि। कृपया पुनः प्रयास करें"
    is UnknownError -> "अज्ञात त्रुटि हुई"
    is BusinessLogic -> details
    is Custom -> message
  }
}

// ================================================================================================
// PERFECT STATE MACHINE
// ================================================================================================

/**
 * Perfect state machine with exhaustive states and computed properties
 */
internal sealed class SubmissionState {
  object Idle : SubmissionState()
  object Validating : SubmissionState()
  object Processing : SubmissionState()
  object Success : SubmissionState()
  data class Error(val error: SubmissionError, val retryable: Boolean = true) : SubmissionState()

  // Computed properties for perfect UX
  val isInteractionAllowed: Boolean get() = this is SubmissionState.Idle
  val isLoading: Boolean get() = this is SubmissionState.Validating || this is SubmissionState.Processing
  val canRetry: Boolean get() = this is SubmissionState.Error && retryable
}

/**
 * Bulletproof state machine that prevents invalid transitions
 */
private class SubmissionStateMachine {
  private var _state: SubmissionState = SubmissionState.Idle
  val state: SubmissionState get() = _state

  fun transition(newState: SubmissionState): Boolean {
    val isValidTransition = when (_state) {
      is SubmissionState.Idle -> newState is SubmissionState.Validating
      is SubmissionState.Validating ->
        newState is SubmissionState.Processing || newState is SubmissionState.Error

      is SubmissionState.Processing ->
        newState is SubmissionState.Success || newState is SubmissionState.Error

      is SubmissionState.Success -> newState is SubmissionState.Idle
      is SubmissionState.Error ->
        newState is SubmissionState.Idle || newState is SubmissionState.Validating
    }

    if (isValidTransition) {
      _state = newState
      return true
    }
    return false
  }

  fun reset() {
    _state = SubmissionState.Idle
  }
}

// ================================================================================================
// PERFECT CONFIGURATION SYSTEM
// ================================================================================================

/**
 * Perfect configuration with intelligent defaults and builder pattern
 */
data class SubmitButtonTexts(
  val submittingText: String = "प्रेषित किया जा रहा है...",
  val successText: String = "सफल!",
  val errorText: String = "त्रुटि हुई",
  val validatingText: String = "जांच की जा रही है...",
  val retryText: String = "पुनः प्रयास करें"
) {
  companion object {
    fun default() = SubmitButtonTexts()
    fun english() = SubmitButtonTexts(
      submittingText = "Submitting...",
      successText = "Success!",
      errorText = "Error",
      validatingText = "Validating...",
      retryText = "Retry"
    )
  }
}

/**
 * Perfect color system with state-aware colors
 */
data class SubmitButtonColors(
  val idleContainer: Color = Color.Unspecified,
  val idleContent: Color = Color.Unspecified,
  val processingContainer: Color = Color.Unspecified,
  val processingContent: Color = Color.Unspecified,
  val successContainer: Color = Color(0xFF059669), // Professional dark emerald as default
  val successContent: Color = Color.White,
  val errorContainer: Color = Color(0xFFDC2626), // Professional dark red as default
  val errorContent: Color = Color.White
) {
  companion object {
    fun default() = SubmitButtonColors.professional() // Use professional colors as default

    fun vibrant() = SubmitButtonColors(
      successContainer = Color(0xFF10B981),
      errorContainer = Color(0xFFE11D48)
    )

    fun soft() = SubmitButtonColors(
      successContainer = Color(0xFF34D399),
      errorContainer = Color(0xFFF87171)
    )

    fun professional() = SubmitButtonColors(
      successContainer = Color(0xFF059669),
      errorContainer = Color(0xFFDC2626)
    )
  }

  @Composable
  internal fun getContainerColor(state: SubmissionState): Color = when (state) {
    is SubmissionState.Idle ->
      if (idleContainer == Color.Unspecified) MaterialTheme.colorScheme.primary else idleContainer

    is SubmissionState.Validating, is SubmissionState.Processing ->
      if (processingContainer == Color.Unspecified) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
      } else processingContainer

    is SubmissionState.Success -> successContainer
    is SubmissionState.Error -> errorContainer
  }

  @Composable
  internal fun getContentColor(state: SubmissionState): Color = when (state) {
    is SubmissionState.Idle ->
      if (idleContent == Color.Unspecified) MaterialTheme.colorScheme.onPrimary else idleContent

    is SubmissionState.Validating, is SubmissionState.Processing ->
      if (processingContent == Color.Unspecified) MaterialTheme.colorScheme.onPrimary else processingContent

    is SubmissionState.Success -> successContent
    is SubmissionState.Error -> errorContent
  }
}

/**
 * Public interface for submit button callbacks
 */
interface SubmitCallbacks {
  fun onSuccess() {}
  fun onError(error: SubmissionError) {}
  fun onRetry() {}
}

/**
 * Internal callbacks that include state change notifications
 */
internal data class SubmitButtonCallbacks(
  val onSuccess: () -> Unit = {},
  val onError: (SubmissionError) -> Unit = {},
  val onStateChange: (SubmissionState) -> Unit = {},
  val onRetry: () -> Unit = {}
) {
  companion object {
    fun empty() = SubmitButtonCallbacks()

    fun fromPublic(callbacks: SubmitCallbacks) = SubmitButtonCallbacks(
      onSuccess = callbacks::onSuccess,
      onError = callbacks::onError,
      onRetry = callbacks::onRetry
    )
  }
}

/**
 * Master configuration class - the command center
 */
data class SubmitButtonConfig(
  val enabled: Boolean = true,
  val fillMaxWidth: Boolean = false,
  val successDuration: Long = 1500L,
  val errorDuration: Long = 3000L,
  val validator: (suspend () -> SubmissionError?)? = null,
  val errorMapper: (Exception) -> SubmissionError = {
    when {
      it.message?.contains("network", ignoreCase = true) == true -> SubmissionError.NetworkError
      it.message?.contains("validation", ignoreCase = true) == true -> SubmissionError.ValidationFailed
      else -> SubmissionError.UnknownError
    }
  },
  val texts: SubmitButtonTexts = SubmitButtonTexts.default(),
  val colors: SubmitButtonColors = SubmitButtonColors.default(),
  val showRetryOnError: Boolean = true
) {
  companion object {
    fun default() = SubmitButtonConfig()
  }
}

// ================================================================================================
// PERFECT BUTTON CONTENT
// ================================================================================================

@Composable
private fun PerfectButtonContent(
  state: SubmissionState,
  text: String,
  config: SubmitButtonConfig
) {
  // Use Box with generous padding to match the visual appearance from the example
  Box(
    modifier = Modifier
      .widthIn(min = 160.dp) // Increased minimum width for better proportion
      .padding(horizontal = 32.dp), // Increased to 32.dp for more generous, visible padding
    contentAlignment = Alignment.Center
  ) {
    AnimatedContent(
      targetState = state,
      transitionSpec = {
        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
      },
      label = "ButtonContent"
    ) { currentState ->
      when (currentState) {
        is SubmissionState.Idle -> {
          Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
          )
        }

        is SubmissionState.Validating -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = config.colors.getContentColor(currentState),
              strokeWidth = 2.dp
            )
            Text(
              text = config.texts.validatingText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        is SubmissionState.Processing -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = config.colors.getContentColor(currentState),
              strokeWidth = 2.dp
            )
            Text(
              text = config.texts.submittingText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        is SubmissionState.Success -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Success",
              modifier = Modifier.size(16.dp),
              tint = config.colors.getContentColor(currentState)
            )
            Text(
              text = config.texts.successText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        is SubmissionState.Error -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (config.showRetryOnError) Icons.Default.Refresh else Icons.Default.Close,
              contentDescription = "Error",
              modifier = Modifier.size(16.dp),
              tint = config.colors.getContentColor(currentState)
            )
            Text(
              text = if (config.showRetryOnError && currentState.retryable)
                config.texts.retryText else config.texts.errorText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }
    }
  }
}

// ================================================================================================
// THE PERFECT SUBMIT BUTTON
// ================================================================================================

/**
 * A perfect submit button that eliminates entire categories of bugs.
 *
 * Design Principles:
 * - Impossible to misuse
 * - Self-contained complexity
 * - Type-safe error handling
 * - Perfect accessibility
 * - Comprehensive instrumentation
 * - Zero-config defaults
 * - Gallery-testable states
 */
@Composable
fun SubmitButton(
  text: String,
  onSubmit: suspend () -> Unit,
  modifier: Modifier = Modifier,
  config: SubmitButtonConfig = SubmitButtonConfig.default(),
  callbacks: SubmitCallbacks = object : SubmitCallbacks {}
) {
  val stateMachine = remember { SubmissionStateMachine() }
  var currentState by remember { mutableStateOf<SubmissionState>(SubmissionState.Idle) }
  val coroutineScope = rememberCoroutineScope()
  val internalCallbacks = remember { SubmitButtonCallbacks.fromPublic(callbacks) }

  // Perfect submission logic with comprehensive error handling
  val handleSubmission = remember {
    {
      coroutineScope.launch {
        try {
          // Validation phase
          if (!stateMachine.transition(SubmissionState.Validating)) return@launch
          currentState = SubmissionState.Validating
          internalCallbacks.onStateChange(currentState)

          config.validator?.invoke()?.let { validationError ->
            stateMachine.transition(SubmissionState.Error(validationError))
            currentState = stateMachine.state
            internalCallbacks.onStateChange(currentState)
            internalCallbacks.onError(validationError)
            return@launch
          }

          // Processing phase
          if (!stateMachine.transition(SubmissionState.Processing)) return@launch
          currentState = SubmissionState.Processing
          internalCallbacks.onStateChange(currentState)

          // Execute user logic
          onSubmit()

          // Success
          stateMachine.transition(SubmissionState.Success)
          currentState = SubmissionState.Success
          internalCallbacks.onStateChange(currentState)
          internalCallbacks.onSuccess()

        } catch (e: Exception) {
          val error = config.errorMapper(e)
          stateMachine.transition(SubmissionState.Error(error))
          currentState = stateMachine.state
          internalCallbacks.onStateChange(currentState)
          internalCallbacks.onError(error)
        }
      }
    }
  }

  // Handle retry logic for errors
  val handleRetry = remember {
    {
      if (currentState is SubmissionState.Error && (currentState as SubmissionState.Error).retryable) {
        internalCallbacks.onRetry()
        handleSubmission()
      }
    }
  }

  // Auto-reset with perfect timing
  LaunchedEffect(currentState) {
    when (val state = currentState) {
      is SubmissionState.Success -> {
        delay(config.successDuration)
        stateMachine.reset()
        currentState = SubmissionState.Idle
        internalCallbacks.onStateChange(currentState)
      }

      is SubmissionState.Error -> {
        if (!config.showRetryOnError) {
          delay(config.errorDuration)
          if (stateMachine.state is SubmissionState.Error) {
            stateMachine.reset()
            currentState = SubmissionState.Idle
            internalCallbacks.onStateChange(currentState)
          }
        }
      }

      else -> {}
    }
  }

  Button(
    onClick = {
      // Smart click prevention - only process clicks when appropriate
      // This avoids ugly disabled styling while preventing unwanted clicks
      when (currentState) {
        is SubmissionState.Idle -> {
          if (config.enabled) handleSubmission()
        }
        is SubmissionState.Error -> {
          if (config.showRetryOnError && config.enabled) handleRetry()
        }
        // Ignore clicks during Validating, Processing, and Success states
        // This provides the "smart prevention" without disabled styling
        else -> { /* No action - click is ignored gracefully */
        }
      }
    },
    modifier = modifier.then(
      if (config.fillMaxWidth) Modifier.fillMaxWidth().height(56.dp)
      else Modifier.height(56.dp)
    ).semantics {
      // Perfect accessibility
      when (currentState) {
        is SubmissionState.Validating -> {
          contentDescription = "${config.texts.validatingText} - $text"
          stateDescription = "जांच प्रक्रिया में"
        }

        is SubmissionState.Processing -> {
          contentDescription = "${config.texts.submittingText} - $text"
          stateDescription = "प्रक्रिया में"
        }

        is SubmissionState.Success -> {
          contentDescription = "${config.texts.successText} - $text"
          stateDescription = "सफल"
        }

        is SubmissionState.Error -> {
          contentDescription = "${config.texts.errorText} - $text"
          stateDescription = "त्रुटि"
        }

        else -> {
          contentDescription = text
          stateDescription = "तैयार"
        }
      }
    },
    // ALWAYS ENABLED - This prevents ugly disabled styling!
    // Click prevention is handled in onClick logic above
    enabled = true,
    colors = ButtonDefaults.buttonColors(
      containerColor = config.colors.getContainerColor(currentState),
      contentColor = config.colors.getContentColor(currentState)
    ),
    // Remove default Material3 content padding to use our custom 24.dp padding
    contentPadding = PaddingValues(0.dp)
  ) {
    PerfectButtonContent(
      state = currentState,
      text = text,
      config = config
    )
  }
}

// ================================================================================================
// GALLERY TESTING UTILITIES - FOR AMAZING DEMOS
// ================================================================================================

/**
 * Gallery-friendly test harness for showcasing all button states
 */
@Composable
fun SubmitButtonGallery(
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      "SubmitButton Gallery - All States",
      style = MaterialTheme.typography.headlineSmall
    )

    // All States in FlowRow - much better layout!
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      // Idle State
      SubmitButtonDemo(
        title = "Idle State",
        initialState = SubmissionState.Idle,
        text = "Submit Form",
        colors = SubmitButtonColors.default()
      )

      // Validating State
      SubmitButtonDemo(
        title = "Validating State",
        initialState = SubmissionState.Validating,
        text = "Validating...",
        colors = SubmitButtonColors.default()
      )

      // Processing State
      SubmitButtonDemo(
        title = "Processing State",
        initialState = SubmissionState.Processing,
        text = "Processing...",
        colors = SubmitButtonColors.default()
      )

      // Success State
      SubmitButtonDemo(
        title = "Success State",
        initialState = SubmissionState.Success,
        text = "Success!",
        colors = SubmitButtonColors.default()
      )

      // Error State
      SubmitButtonDemo(
        title = "Error State",
        initialState = SubmissionState.Error(SubmissionError.NetworkError),
        text = "Retry",
        colors = SubmitButtonColors.default()
      )
    }

    // Custom Configuration Demo
    Card(
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Custom Configuration Demo", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SubmitButton(
          text = "Custom Button",
          onSubmit = {
            delay(2000)
            if (Random.nextInt(2) == 0) {
              throw Exception("Random error for demo")
            }
          },
          config = SubmitButtonConfig(
            texts = SubmitButtonTexts(
              submittingText = "Creating magic...",
              successText = "Magic created!",
              errorText = "Magic failed!"
            ),
            colors = SubmitButtonColors.vibrant(), // Use beautiful vibrant colors
            successDuration = 3000L,
            showRetryOnError = true
          ),
          callbacks = object : SubmitCallbacks {
            override fun onSuccess() {
              println("Gallery: Success callback")
            }

            override fun onError(error: SubmissionError) {
              println("Gallery: Error - ${error.toUserMessage()}")
            }
          }
        )
      }
    }

    // Beautiful Color Schemes Demo
    Card(
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Beautiful Color Schemes", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Vibrant Colors:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Success,
            text = "Success",
            colors = SubmitButtonColors.vibrant()
          )
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Error(SubmissionError.NetworkError),
            text = "Error",
            colors = SubmitButtonColors.vibrant()
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Soft Colors:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Success,
            text = "Success",
            colors = SubmitButtonColors.soft()
          )
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Error(SubmissionError.NetworkError),
            text = "Error",
            colors = SubmitButtonColors.soft()
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Professional Colors:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Success,
            text = "Success",
            colors = SubmitButtonColors.professional()
          )
          SubmitButtonDemo(
            title = "",
            initialState = SubmissionState.Error(SubmissionError.NetworkError),
            text = "Error",
            colors = SubmitButtonColors.professional()
          )
        }
      }
    }
  }
}

/**
 * Individual demo button for gallery
 */
@Composable
private fun SubmitButtonDemo(
  title: String,
  initialState: SubmissionState,
  text: String,
  colors: SubmitButtonColors = SubmitButtonColors.default()
) {
  Card(
    modifier = Modifier
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))

      // Mock button that stays in the demo state
      var currentState by remember { mutableStateOf(initialState) }

      Button(
        onClick = { /* Demo - no action */ },
        modifier = Modifier.height(56.dp),
        enabled = true, // Always enabled like our main button - no ugly disabled styling
        colors = ButtonDefaults.buttonColors(
          containerColor = colors.getContainerColor(currentState),
          contentColor = colors.getContentColor(currentState)
        ),
        // Remove default Material3 content padding to use our custom 24.dp padding
        contentPadding = PaddingValues(0.dp)
      ) {
        PerfectButtonContent(
          state = currentState,
          text = text,
          config = SubmitButtonConfig.default().copy(colors = colors)
        )
      }
    }
  }
}
