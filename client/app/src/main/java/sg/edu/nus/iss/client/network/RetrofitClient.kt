package sg.edu.nus.iss.client.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.util.SessionManager

object RetrofitClient {
    // If you intend to run the application on your Android Device, 
    // Ensure your Local Machine and Android Device are on the same network (i.e. the same IP address)
    // Replace with your server's IP4 address and Update the "network_security_config.xml"
    // private const val BASE_URL = "http://10.XXX.XX.XXX:8000/"

    private const val BASE_URL = "http://10.0.2.2:8000/"
    private var apiService: AuthApiService? = null

    fun getApiService(context: Context): AuthApiService {
        return apiService ?: synchronized(this) {
            val sessionManager = SessionManager(context.applicationContext)
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
