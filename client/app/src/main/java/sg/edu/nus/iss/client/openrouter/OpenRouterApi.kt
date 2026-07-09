package sg.edu.nus.iss.client.openrouter

// Author: Soo Kwok Heng
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}