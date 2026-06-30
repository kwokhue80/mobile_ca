package sg.edu.nus.iss.client.chatbot


data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = sent by user (right side), false = sent by bot (left side)
)