package sg.edu.nus.iss.client.openrouter

import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)