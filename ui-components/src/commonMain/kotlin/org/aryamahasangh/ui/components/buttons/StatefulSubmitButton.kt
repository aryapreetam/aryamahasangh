package org.aryamahasangh.ui.components.buttons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A stateful submit button that handles different states: initial, submitting, success, and error.
 * The button automatically resets to initial state after showing success/error.
 *
 * @param text The text to display on the button in initial state
 * @param onSubmit Callback triggered when button is clicked in initial state
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled in initial state
 * @param submittingText Text to show during submitting state
 * @param successText Text to show during success state
 * @param errorText Text to show during error state
 * @param successDuration How long to show success state before resetting (ms)
 * @param errorDuration How long to show error state before resetting (ms)
 * @param fillMaxWidth Whether button should fill maximum width
 */
@Composable
fun StatefulSubmitButton(
  text: String,
  onSubmit: suspend () -> Result<Unit>,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  submittingText: String = "प्रेषित किया जा रहा है...", // "Submitting..." in pure Hindi
  successText: String = "उत्तम!", // "Success!" in Hindi
  errorText: String = "त्रुटि हुई", // "Error occurred" in Hindi
  successDuration: Long = 2000L,
  errorDuration: Long = 3000L,
  fillMaxWidth: Boolean = true
) {
  var buttonState by remember { mutableStateOf(SubmitButtonState.INITIAL) }
  val coroutineScope = rememberCoroutineScope()

  // Auto reset after success/error
  LaunchedEffect(buttonState) {
    when (buttonState) {
      SubmitButtonState.SUCCESS -> {
        delay(successDuration)
        buttonState = SubmitButtonState.INITIAL
      }

      SubmitButtonState.ERROR -> {
        delay(errorDuration)
        buttonState = SubmitButtonState.INITIAL
      }

      else -> {}
    }
  }

  val buttonColors = ButtonDefaults.buttonColors(
    containerColor = when (buttonState) {
      SubmitButtonState.INITIAL -> MaterialTheme.colorScheme.primary
      SubmitButtonState.SUBMITTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
      SubmitButtonState.SUCCESS -> MaterialTheme.colorScheme.tertiary
      SubmitButtonState.ERROR -> MaterialTheme.colorScheme.error
    },
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
  )

  val isClickable = buttonState == SubmitButtonState.INITIAL && enabled

  Button(
    onClick = {
      if (buttonState == SubmitButtonState.INITIAL) {
        buttonState = SubmitButtonState.SUBMITTING
        coroutineScope.launch {
          try {
            onSubmit().fold(
              onSuccess = { buttonState = SubmitButtonState.SUCCESS },
              onFailure = { buttonState = SubmitButtonState.ERROR }
            )
          } catch (e: Exception) {
            buttonState = SubmitButtonState.ERROR
          }
        }
      }
    },
    modifier = if (fillMaxWidth) {
      modifier.fillMaxWidth().height(56.dp)
    } else {
      modifier.height(56.dp)
    },
    enabled = isClickable,
    colors = buttonColors
  ) {
    AnimatedContent(
      targetState = buttonState,
      transitionSpec = {
        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
      },
      label = "ButtonContent"
    ) { state ->
      when (state) {
        SubmitButtonState.INITIAL -> {
          Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
          )
        }

        SubmitButtonState.SUBMITTING -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
            Text(
              text = submittingText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        SubmitButtonState.SUCCESS -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Success",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onTertiary
            )
            Text(
              text = successText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        SubmitButtonState.ERROR -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Error",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onError
            )
            Text(
              text = errorText,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }
    }
  }
}

/**
 * A simpler version that automatically handles the submit operation
 */
@Composable
fun SimpleSubmitButton(
  text: String,
  onSubmit: suspend () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  submittingText: String = "प्रेषित किया जा रहा है...",
  successText: String = "उत्तम!",
  errorText: String = "त्रुटि हुई",
  fillMaxWidth: Boolean = true
) {
  StatefulSubmitButton(
    text = text,
    onSubmit = {
      try {
        onSubmit()
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    },
    modifier = modifier,
    enabled = enabled,
    submittingText = submittingText,
    successText = successText,
    errorText = errorText,
    fillMaxWidth = fillMaxWidth
  )
}
