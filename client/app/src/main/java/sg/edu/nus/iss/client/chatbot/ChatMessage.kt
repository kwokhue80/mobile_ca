package sg.edu.nus.iss.client.chatbot


data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = sent by user (green bubble), false = sent by bot (grey bubble)
)