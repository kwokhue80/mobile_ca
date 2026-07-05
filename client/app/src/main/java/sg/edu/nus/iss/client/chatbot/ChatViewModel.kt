package sg.edu.nus.iss.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(private val ragRepository: RagRepository) : ViewModel() {

    // "messages" holds every message for the app session
    val messages = mutableListOf<ChatMessage>()

    fun processUserQuery(userQuery: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        if (userQuery.isBlank()) return

        // used for testing
        android.util.Log.d("ChatViewModel", "Processing query: $userQuery")

        viewModelScope.launch {
            try {
                // Only send the most recent messages, not the whole history
                val recentHistory = messages.takeLast(10)

                val answer = ragRepository.answer(userQuery, recentHistory)
                onResult(answer)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}