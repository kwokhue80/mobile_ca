package sg.edu.nus.iss.client.openrouter

// Author: Soo Kwok Heng
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice> = emptyList()
)

data class OpenRouterChoice(
    val message: OpenRouterMessage
)