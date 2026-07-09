package sg.edu.nus.iss.client.backend

// Authors: Amelia Wong, Soo Kwok Heng
import com.google.gson.annotations.SerializedName
import sg.edu.nus.iss.client.chatbot.ChatMessage

data class ChatRequest(
    val query: String,
    val recentMessages: List<ChatMessage>,
    val relevantPastMessages: List<ChatMessage>
)

data class ChatResponse(
    val answer: String
)

data class RecommendationPayload(
    val type: String,
    val title: String,
    val message: String,
    val category: String,
    val priority: String,
    val actionable: Boolean,
    @SerializedName("generated_at") val generatedAt: String,
)