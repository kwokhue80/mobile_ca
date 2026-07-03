package sg.edu.nus.iss.client.openrouter

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)