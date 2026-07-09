// ======================================================================== //
//  AUTHORS: Amelia
// ======================================================================== //
package sg.edu.nus.iss.client.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.network.AuthApiService

sealed class LogoutUiState {
    object Idle : LogoutUiState()
    object Loading : LogoutUiState()
    object Success : LogoutUiState()
    data class Error(val message: String) : LogoutUiState()
}

// Logout -> call backend api -> revoke JWT token -> redirect to login page
class LogoutViewModel(private val authApiService: AuthApiService) : ViewModel() {
    private val _uiState = MutableStateFlow<LogoutUiState>(LogoutUiState.Idle)
    val uiState: StateFlow<LogoutUiState> = _uiState.asStateFlow()

    fun logout() {
        _uiState.value = LogoutUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("LogoutViewModel", "Attempting logout")
                val response = authApiService.logout()
                Log.d("LogoutViewModel", "Logout response: ${response.code()}")

                if (response.isSuccessful) {
                    _uiState.value = LogoutUiState.Success
                } else {
                    _uiState.value = LogoutUiState.Error("Logout request failed. You have been signed out locally.")
                }
            } catch (e: Exception) {
                _uiState.value = LogoutUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LogoutUiState.Idle
    }
}