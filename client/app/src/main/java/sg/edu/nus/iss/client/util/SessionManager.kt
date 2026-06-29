package sg.edu.nus.iss.client.util

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        sharedPreferences.edit { putString("auth_token", token) }
    }

    fun fetchAuthToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun clearSession() {
        sharedPreferences.edit { remove("auth_token") }
    }
}