// ======================================================================== //
//  AUTHORS: Amelia
// ======================================================================== //
package sg.edu.nus.iss.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RegisterRequest

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val token: String) : RegisterUiState()
    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : RegisterUiState()
}

class RegisterViewModel(private val authApiService: AuthApiService) : ViewModel() {
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, confirmPassword: String) {
        val normalizedEmail = email.trim().lowercase()

        if (normalizedEmail.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            val localFieldErrors = linkedMapOf<String, String>()
            if (normalizedEmail.isBlank()) localFieldErrors["emailAddress"] = "Email address must not be blank"
            if (password.isBlank()) localFieldErrors["password"] = "Password cannot be blank"
            if (confirmPassword.isBlank()) localFieldErrors["confirmPassword"] = "Please confirm your password"
            _uiState.value = RegisterUiState.Error(
                message = "Please fill in all required fields",
                fieldErrors = localFieldErrors
            )
            return
        }

        if (password != confirmPassword) {
            _uiState.value = RegisterUiState.Error(
                message = "Password confirmation does not match",
                fieldErrors = mapOf("confirmPassword" to "Password confirmation does not match")
            )
            return
        }

        _uiState.value = RegisterUiState.Loading

        viewModelScope.launch {
            try {
                val request = RegisterRequest(normalizedEmail, password)
                val response = authApiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = RegisterUiState.Success(response.body()!!.token)
                } else {
                    val backendError = parseBackendError(response.errorBody()?.string())
                    _uiState.value = RegisterUiState.Error(
                        message = backendError.message,
                        fieldErrors = backendError.fieldErrors
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error("Network error: ${e.message}")
            }
        }
    }

    // Parse GlobalExceptionHandler payload to show specific backend validation and runtime messages.
    private fun parseBackendError(rawBody: String?): ParsedRegisterError {
        if (rawBody.isNullOrBlank()) {
            return ParsedRegisterError(
                message = "Registration failed. Please verify your details.",
                fieldErrors = emptyMap()
            )
        }

        return runCatching {
            val json = JSONObject(rawBody)
            val fieldErrors = json.optJSONObject("fieldErrors")
            val parsedFieldErrors = linkedMapOf<String, String>()

            if (fieldErrors != null && fieldErrors.length() > 0) {
                fieldErrors.keys().asSequence().forEach { key ->
                    fieldErrors.optString(key).takeIf { it.isNotBlank() }?.let { parsedFieldErrors[key] = it }
                }
            }

            val preferredOrder = listOf("emailAddress", "password", "confirmPassword")
            val orderedMessages = linkedSetOf<String>()
            preferredOrder.forEach { key -> parsedFieldErrors[key]?.let { orderedMessages.add(it) } }
            parsedFieldErrors.values.forEach { orderedMessages.add(it) }

            val fallbackMessage = json.optString("message")
            val message = orderedMessages.joinToString("\n").ifBlank { fallbackMessage }

            ParsedRegisterError(
                message = message.ifBlank { "Registration failed. Please verify your details." },
                fieldErrors = parsedFieldErrors
            )
        }.getOrElse {
            ParsedRegisterError(
                message = "Registration failed. Please verify your details.",
                fieldErrors = emptyMap()
            )
        }
    }

    private data class ParsedRegisterError(
        val message: String,
        val fieldErrors: Map<String, String>
    )

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
