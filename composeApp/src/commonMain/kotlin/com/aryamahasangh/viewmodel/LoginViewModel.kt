package com.aryamahasangh.viewmodel

import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.domain.error.AppError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

data class LoginUiState(
  val phoneNumber: String = "",
  val password: String = "",
  val phoneError: String? = null,
  val passwordError: String? = null,
  override val error: String? = null,
  override val appError: AppError? = null,
  override val isLoading: Boolean = false,
  val isLoginSuccessful: Boolean = false
) : ErrorState

class LoginViewModel(
  private val supabaseClient: SupabaseClient,
  private val sessionManager: SessionManager
) : BaseViewModel<LoginUiState>(LoginUiState()) {

  fun onPhoneNumberChange(input: String) {
    // Only allow digits and common phone number separators
    if (input.length <= 15 && input.matches(Regex("^[0-9+\\-() ]*$"))) {
      updateState { it.copy(phoneNumber = input) }
    }
    if (currentState.phoneError != null && input.isNotEmpty()) {
      updateState { it.copy(phoneError = null) }
    }
  }

  fun onPasswordChange(input: String) {
    if (input.length <= 30) {
      updateState { it.copy(password = input) }
    }
    if (currentState.passwordError != null && input.length >= 6) {
      updateState { it.copy(passwordError = null) }
    }
  }

  fun login() {
    val phoneNumber = currentState.phoneNumber
    val password = currentState.password
    
    val isValidPassword = password.isNotEmpty() && password.length >= 6
    val isPhoneEmpty = phoneNumber.isEmpty()
    
    if (isPhoneEmpty || !isValidPassword) {
      updateState {
        it.copy(
          phoneError = if (isPhoneEmpty) "Phone number is required" else null,
          passwordError = if (!isValidPassword) "Password is required (min 6 chars)" else null
        )
      }
      return
    }

    updateState { it.copy(isLoading = true, error = null, appError = null) }

    launch {
      try {
        // Ensure any existing session is cleaned up first
        sessionManager.ensureCleanLogin()

        // Now proceed with login
        supabaseClient.auth.signInWith(Email) {
          email = "$phoneNumber@aryamahasangh.com"
          this.password = password
        }
        
        updateState { it.copy(isLoading = false, isLoginSuccessful = true) }
      } catch (e: Exception) {
        val errorMessage = when {
          e.message?.contains("Invalid login credentials") == true ->
            "गलत username या password"

          e.message?.contains("Email not confirmed") == true ->
            "username की पुष्टि नहीं हुई है"

          e.message?.contains("User not found") == true ->
            "user नहीं मिला"

          else ->
            e.message ?: "लॉगिन में त्रुटि हुई"
        }
        
        updateState { 
          it.copy(
            isLoading = false, 
            error = errorMessage
          ) 
        }
      }
    }
  }

  fun resetState() {
    updateState { LoginUiState() }
  }
}
