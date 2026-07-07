package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.chatbot.ChatMessage

class BackendRepository(
    private val backendApi: BackendApi
) {
    suspend fun answer(
        query: String,
        conversationHistory: List<ChatMessage>,
        relevantPastMessages: List<ChatMessage>
    ): String {
        val request = ChatRequest(query, conversationHistory, relevantPastMessages)
        val response = backendApi.sendQuery(request)
        return response.answer
    }
}