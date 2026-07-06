package sg.edu.nus.iss.client.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.network.AuthApiService

class ProfileViewModelFactory(private val authApiService: AuthApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
