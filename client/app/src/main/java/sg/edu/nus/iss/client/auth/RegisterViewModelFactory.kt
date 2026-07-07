package sg.edu.nus.iss.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.network.AuthApiService

class RegisterViewModelFactory(private val authApiService: AuthApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(authApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
