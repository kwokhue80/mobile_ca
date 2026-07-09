package sg.edu.nus.iss.client.backend
// Authors: Amelia Wong, Soo Kwok Heng
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BackendApi {

    // One request = one response
    // The backend will decide anything
    // extra it needs (like calling Spring Boot or doing a web search)
    // on its own side, without asking the app for help.
    @POST("api/chat")
    suspend fun sendQuery(@Body request: ChatRequest): ChatResponse

    @GET("api/tools")
    suspend fun getAvailableTools(): List<String>

    // For personalised recommendation notifs
    @GET("api/recommendations")
    suspend fun getRecommendations(): RecommendationPayload
}