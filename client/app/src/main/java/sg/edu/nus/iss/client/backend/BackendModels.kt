package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.chatbot.ChatMessage

// Everything the app has already found out about the query, which is then
// sent to the backend in one request
data class ChatRequest(
    val query: String,
    val retrievedContext: String,
    val localMatchFound: Boolean,
    val conversationHistory: List<ChatMessage>
)

// The backend's final answer
data class ChatResponse(
    val answer: String
)