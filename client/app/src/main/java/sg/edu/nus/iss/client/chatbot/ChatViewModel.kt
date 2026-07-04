package sg.edu.nus.iss.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(private val ragRepository: RagRepository) : ViewModel() {

    // "messages" holds every message for the app session
    val messages = mutableListOf<ChatMessage>()

    fun processUserQuery(userQuery: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        if (userQuery.isBlank()) return

        viewModelScope.launch {
            try {
                // Only send the most recent messages, not the whole history
                val recentHistory = messages.takeLast(10)

                // Decide whether this query is worth checking against the local vector db
                val shouldSearch = QueryRouter.shouldSearchVectorDb(userQuery)

                val answer = ragRepository.answer(userQuery, recentHistory, shouldSearch)
                onResult(answer)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}