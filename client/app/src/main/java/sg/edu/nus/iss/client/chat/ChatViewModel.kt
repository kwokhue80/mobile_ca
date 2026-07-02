package sg.edu.nus.iss.client.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel(private val vectorizer: LocalMiniLMVectorizer): ViewModel() {
    fun processUserQuery(userQuery: String) {
        if (userQuery.isBlank()) return

        viewModelScope.launch {
            // vectorize the query
            val queryVector: FloatArray = vectorizer.vectorize(userQuery)

            if (queryVector.isNotEmpty()) {
                searchVectorDB(queryVector)
            } // else { }

        }
    }

    private fun searchVectorDB(queryVector: FloatArray) {

    }
}