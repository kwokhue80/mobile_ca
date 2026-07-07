package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.chatbot.ChatMessage

data class ChatRequest(
    val query: String,
    val conversationHistory: List<ChatMessage>,
    val relevantPastMessages: List<ChatMessage>
)

data class ChatResponse(
    val answer: String
)