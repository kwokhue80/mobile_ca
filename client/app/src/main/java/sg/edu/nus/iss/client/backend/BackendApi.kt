package sg.edu.nus.iss.client.backend

import retrofit2.http.Body
import retrofit2.http.POST

interface BackendApi {

    // Step 1: send the user's raw question, backend decides what it needs
    @POST("chat")
    suspend fun sendQuery(@Body request: BackendChatRequest): BackendChatResponse

    // Step 2 (only if backend asks): send back local vector db search results
    @POST("chat/vector-results")
    suspend fun sendVectorResults(@Body request: VectorResultsRequest): BackendChatResponse
}