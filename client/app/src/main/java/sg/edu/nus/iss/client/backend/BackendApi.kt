package sg.edu.nus.iss.client.backend

import retrofit2.http.Body
import retrofit2.http.POST

interface BackendApi {

    // One request = one response
    // The backend will decide anything
    // extra it needs (like calling Spring Boot or doing a web search)
    // on its own side, without asking the app for help.
    @POST("chat")
    suspend fun sendQuery(@Body request: ChatRequest): ChatResponse
}