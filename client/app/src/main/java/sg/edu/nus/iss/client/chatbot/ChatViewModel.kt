package sg.edu.nus.iss.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.chathistory.ChatHistoryRepository
import sg.edu.nus.iss.client.chathistory.FeatureFlags

class ChatViewModel(
    private val ragRepository: RagRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    // Holds every message for the current app session
    val messages = mutableListOf<ChatMessage>()

    init {
        // Restores messages saved from a previous session, only when the
        // persistence feature is enabled
        if (FeatureFlags.ENABLE_CHAT_HISTORY_PERSISTENCE) {
            val savedMessages = chatHistoryRepository.getRecentMessages()
            messages.addAll(savedMessages)
        }
    }

    fun processUserQuery(userQuery: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        if (userQuery.isBlank()) return

        android.util.Log.d("ChatViewModel", "Processing query: $userQuery")

        viewModelScope.launch {
            try {
                // Only the most recent messages are sent, not the full history
                val recentHistory = messages.takeLast(10)

                val answer = ragRepository.answer(userQuery, recentHistory)

                onResult(answer)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}