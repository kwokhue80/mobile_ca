package sg.edu.nus.iss.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(private val ragRepository: RagRepository) : ViewModel() {

    fun processUserQuery(userQuery: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        if (userQuery.isBlank()) return

        viewModelScope.launch {
            try {
                val answer = ragRepository.answer(userQuery)
                onResult(answer)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}