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
            val savedMessages = chatHistoryRepository.getRecentHistory()
            messages.addAll(savedMessages)
        }
    }

    fun processUserQuery(userQuery: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        if (userQuery.isBlank()) return

        android.util.Log.d("ChatViewModel", "Processing query: $userQuery")

        viewModelScope.launch {
            try {
                // Only the most recent messages are sent, not the full history
                val recentMessages = messages.takeLast(10)

                // Repository decides backend-vs-local path and fallback behavior.
                val answer = ragRepository.answer(userQuery, recentMessages)

                android.util.Log.d(
                    "ChatViewModel",
                    "Answer received. length=${answer.length}, blank=${answer.isBlank()}, preview=${answer.take(180)}"
                )

                onResult(answer)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "processUserQuery failed", e)
                onError(e)
            }
        }
    }

    fun checkChatHealth(onResult: (Boolean, String) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                // Mirrors the same path used by real chat calls, but with a cheap
                // endpoint so UI can show bridge/service availability
                val status = ragRepository.getChatHealthStatus()
                onResult(status.first, status.second)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun clearChatHistory() {
        messages.clear()
        if (FeatureFlags.ENABLE_CHAT_HISTORY_PERSISTENCE) {
            chatHistoryRepository.clearAllMessages()
        }
    }
}