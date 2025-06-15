package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import org.aryamahasangh.auth.SessionManager
import org.aryamahasangh.network.supabaseClient

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var phoneErrorMessage by remember { mutableStateOf("Phone number is required") }
    var passwordError by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf("Password is required (min 6 chars)") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }
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
                    value = phoneNumber,
                    onValueChange = { input ->
                        // Only allow digits and common phone number separators
                        if (input.length <= 15 && input.matches(Regex("^[0-9+\\-() ]*$"))) {
                            phoneNumber = input
                        }
                        if (phoneError && input.isNotEmpty()) {
                            phoneError = false
                        }
                    },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
                    isError = phoneError,
                    supportingText = { if (phoneError) Text(phoneErrorMessage) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 30) {
                            password = it
                        }
                        if (passwordError && it.length >= 6) {
                            passwordError = false
                        }
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError,
                    supportingText = { if (passwordError) Text(passwordErrorMessage) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                generalError?.let {
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
                onClick = {
                    val isValidPassword = password.isNotEmpty() && password.length >= 6
                    phoneError = phoneNumber.isEmpty()
                    passwordError = !isValidPassword

                    if (!phoneError && !passwordError) {
                        isLoggingIn = true
                        generalError = null
                        coroutineScope.launch {
                          try {
                            // Ensure any existing session is cleaned up first
                            // This handles edge cases where local logout happened but server session remains
                            SessionManager.ensureCleanLogin()

                            // Now proceed with login
                            supabaseClient.auth.signInWith(Email){
                              email = "$phoneNumber@aryamahasangh.com"
                              this.password = password
                            }
                            // If we reach here, login was successful
                            onLoginSuccess()
                            onDismiss()
                          } catch (e: Exception) {
                            // Handle authentication errors
                            generalError = when {
                              e.message?.contains("Invalid login credentials") == true ->
                                "गलत username या password"

                              e.message?.contains("Email not confirmed") == true ->
                                "username की पुष्टि नहीं हुई है"

                              e.message?.contains("User not found") == true ->
                                "user नहीं मिला"

                              else ->
                                e.message ?: "लॉगिन में त्रुटि हुई"
                            }
                          } finally {
                            isLoggingIn = false
                          }
                        }
                    }
                },
                enabled = !isLoggingIn
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
