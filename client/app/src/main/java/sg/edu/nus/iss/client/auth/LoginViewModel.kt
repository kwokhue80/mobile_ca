// ======================================================================== //
//  AUTHORS: Khai
// ======================================================================== //
package sg.edu.nus.iss.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.LoginRequest

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val token: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val authApiService: AuthApiService) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Attempting login for $email")
                val request = LoginRequest(email, password)
                val response = authApiService.login(request)
                Log.d("LoginViewModel", "Login response: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = LoginUiState.Success(response.body()!!.token)
                } else {
                    _uiState.value = LoginUiState.Error("Invalid credentials. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}