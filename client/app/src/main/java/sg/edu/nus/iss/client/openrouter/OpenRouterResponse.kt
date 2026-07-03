package sg.edu.nus.iss.client.openrouter

import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice> = emptyList()
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)