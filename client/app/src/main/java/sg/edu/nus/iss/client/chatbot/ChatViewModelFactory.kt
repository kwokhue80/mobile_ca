package sg.edu.nus.iss.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.chathistory.ChatHistoryRepository

class ChatViewModelFactory(
    private val ragRepository: RagRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(ragRepository, chatHistoryRepository) as T
    }
}