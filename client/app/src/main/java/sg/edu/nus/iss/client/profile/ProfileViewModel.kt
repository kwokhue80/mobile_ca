package sg.edu.nus.iss.client.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.UserProfileUpdateRequest

sealed class ProfileLoadState {
    object Loading : ProfileLoadState()
    data class Loaded(
        val emailAddress: String,
        val fullName: String,
        val dateOfBirth: String,
        val gender: String,
        val heightCm: String
    ) : ProfileLoadState()
    data class Error(val message: String) : ProfileLoadState()
}

sealed class ProfileSaveState {
    object Idle : ProfileSaveState()
    object Saving : ProfileSaveState()
    object Success : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

private data class ApiErrorBody(val message: String?)

class ProfileViewModel(private val authApiService: AuthApiService) : ViewModel() {

    private val _loadState = MutableStateFlow<ProfileLoadState>(ProfileLoadState.Loading)
    val loadState: StateFlow<ProfileLoadState> = _loadState.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _loadState.value = ProfileLoadState.Loading
        viewModelScope.launch {
            try {
                val response = authApiService.getUserProfile()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _loadState.value = ProfileLoadState.Loaded(
                        emailAddress = body.emailAddress,
                        fullName = body.fullName.orEmpty(),
                        dateOfBirth = body.dateOfBirth.orEmpty(),
                        gender = body.gender.orEmpty(),
                        heightCm = body.heightCm?.let(::formatHeight).orEmpty()
                    )
                } else {
                    _loadState.value = ProfileLoadState.Error(
                        parseErrorMessage(response.errorBody()?.string()) ?: "Failed to load profile"
                    )
                }
            } catch (e: Exception) {
                _loadState.value = ProfileLoadState.Error("Network error: ${e.message}")
            }
        }
    }

    fun saveProfile(fullName: String, dateOfBirth: String, gender: String, heightCm: Double) {
        _saveState.value = ProfileSaveState.Saving
        viewModelScope.launch {
            try {
                val request = UserProfileUpdateRequest(fullName, dateOfBirth, gender, heightCm)
                val response = authApiService.updateUserProfile(request)
                _saveState.value = if (response.isSuccessful) {
                    ProfileSaveState.Success
                } else {
                    ProfileSaveState.Error(
                        parseErrorMessage(response.errorBody()?.string()) ?: "Failed to save profile"
                    )
                }
            } catch (e: Exception) {
                _saveState.value = ProfileSaveState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = ProfileSaveState.Idle
    }

    private fun formatHeight(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            Gson().fromJson(errorBody, ApiErrorBody::class.java)?.message
        } catch (e: Exception) {
            null
        }
    }
}
