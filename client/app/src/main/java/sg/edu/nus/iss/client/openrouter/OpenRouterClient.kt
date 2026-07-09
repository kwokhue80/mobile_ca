package sg.edu.nus.iss.client.openrouter

// Author: Soo Kwok Heng
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.BuildConfig
import java.util.concurrent.TimeUnit

class OpenRouterClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private val api: OpenRouterApi = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouterApi::class.java)

    suspend fun chatCompletion(
        prompt: String,
        model: String = "google/gemini-2.5-flash-lite"
    ): String {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        require(apiKey.isNotBlank()) {
            "OPENROUTER_API_KEY is missing. Check local.properties."
        }

        val response = api.chatCompletion(
            authorization = "Bearer $apiKey",
            request = OpenRouterRequest(
                model = model,
                messages = listOf(OpenRouterMessage(role = "user", content = prompt))
            )
        )

        return response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("OpenRouter returned no choices in response.")
    }
}