// Author: Khairulanwar
package sg.edu.nus.iss.client.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.BuildConfig
import sg.edu.nus.iss.client.util.SessionManager

object RetrofitClient {
    // Override SPRING_BASE_URL in client/local.properties when not using the emulator.
    private const val BASE_URL = BuildConfig.SPRING_BASE_URL

    private var apiService: AuthApiService? = null

    fun getApiService(context: Context): AuthApiService {
        return apiService ?: synchronized(this) {
            val sessionManager = SessionManager.getInstance(context.applicationContext)
            val authInterceptor = AuthInterceptor(sessionManager)

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(AuthApiService::class.java)
            apiService = service
            service
        }
    }
}
