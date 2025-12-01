package com.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aryamahasangh.viewmodel.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginDialog(
  onDismiss: () -> Unit,
  onLoginSuccess: () -> Unit,
  viewModel: LoginViewModel = koinViewModel()
) {
  val state by viewModel.uiState.collectAsState()

  // Handle login success side effect
  LaunchedEffect(state.isLoginSuccessful) {
    if (state.isLoginSuccessful) {
      onLoginSuccess()
      onDismiss()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Login") },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        OutlinedTextField(
          value = state.phoneNumber,
          onValueChange = viewModel::onPhoneNumberChange,
          label = { Text("Username") },
          leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
          isError = state.phoneError != null,
          supportingText = { state.phoneError?.let { Text(it) } },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        OutlinedTextField(
          value = state.password,
          onValueChange = viewModel::onPasswordChange,
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
          visualTransformation = PasswordVisualTransformation(),
          isError = state.passwordError != null,
          supportingText = { state.passwordError?.let { Text(it) } },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        state.error?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = viewModel::login,
        enabled = !state.isLoading
      ) {
        if (state.isLoading) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logging in...")
          }
        } else {
          Text("Login")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    shape = MaterialTheme.shapes.medium
  )
}
