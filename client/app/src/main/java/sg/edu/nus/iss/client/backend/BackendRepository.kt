package sg.edu.nus.iss.client.backend

// Authors: Amelia Wong, Soo Kwok Heng
import org.json.JSONObject
import retrofit2.HttpException
import sg.edu.nus.iss.client.chatbot.ChatMessage

class BackendRepository(
    private val backendApi: BackendApi
) {
    suspend fun answer(
        query: String,
        recentMessages: List<ChatMessage>,
        relevantPastMessages: List<ChatMessage>
    ): String {
        return try {
            // Payload shape must match FastAPI ChatRequest in mcp_agent_wellness.py
            val request = ChatRequest(query, recentMessages, relevantPastMessages)
            val response = backendApi.sendQuery(request)
            response.answer
        } catch (error: HttpException) {
            val errorBody = error.response()?.errorBody()?.string().orEmpty()
            val detail = runCatching {
                JSONObject(errorBody).optString("detail").takeIf { it.isNotBlank() }
            }.getOrNull()

            val message = detail ?: "Backend request failed with HTTP ${error.code()}"
            throw IllegalStateException(message, error)
        }
    }

    suspend fun isHealthy(): Boolean {
        return try {
            // /api/tools is a lightweight probe that verifies bridge + MCP wiring
            backendApi.getAvailableTools()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getRecommendations(): RecommendationPayload {
        return backendApi.getRecommendations()
    }
}