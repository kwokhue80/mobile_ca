package sg.edu.nus.iss.client.network

import okhttp3.Interceptor
import okhttp3.Response
import sg.edu.nus.iss.client.util.SessionManager

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        sessionManager.getDecryptedTokenFromMemory()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401) {
            sessionManager.clearSession()
        }

        return response
    }
}