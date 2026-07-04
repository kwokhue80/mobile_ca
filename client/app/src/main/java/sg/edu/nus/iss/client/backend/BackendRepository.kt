package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.chatbot.ChatMessage

class BackendRepository(
    private val backendApi: BackendApi
) {
    suspend fun answer(
        query: String,
        retrievedContext: String,
        localMatchFound: Boolean,
        conversationHistory: List<ChatMessage>
    ): String {
        val request = ChatRequest(query, retrievedContext, localMatchFound, conversationHistory)
        val response = backendApi.sendQuery(request)
        return response.answer
    }
}