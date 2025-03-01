package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginDialog(onDismiss: () -> Unit, onLoginSuccess: () -> Unit) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var usernameError by remember { mutableStateOf(false) }
  var passwordError by remember { mutableStateOf(false) }
  var isLoggingIn by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Login") },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {

        OutlinedTextField(
          value = username,
          onValueChange = {
            if(it.length < 50){
              username = it
            }
            if (usernameError && it.isNotEmpty()) {
              usernameError = false // Clear error on input
            }
          },
          label = { Text("Username") },
          leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Username") },
          isError = usernameError,
          supportingText = { if (usernameError) Text("Username is required") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = password,
          onValueChange = {
            if(it.length <30)
              password = it
            if (passwordError && it.length >= 8 && it.matches(Regex(".*[!@#\$%^&*()].*"))) {
              passwordError = false // Clear error on valid input
            }
          },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
          visualTransformation = PasswordVisualTransformation(),
          isError = passwordError,
          supportingText = { if (passwordError) Text("Password is required (min 8 chars, 1 special char)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val isValidPassword = password.isNotEmpty() && password.length >= 8 && password.matches(Regex(".*[!@#\$%^&*()].*"))
          usernameError = username.isEmpty()
          passwordError = !isValidPassword

          if (!usernameError && !passwordError) {
            isLoggingIn = true
            coroutineScope.launch {
              delay(2000)
              isLoggingIn = false
              println("Login successful")
              onLoginSuccess()
              onDismiss()
            }
          }
        },
        enabled = !isLoggingIn,
      ) {
        if (isLoggingIn) {
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