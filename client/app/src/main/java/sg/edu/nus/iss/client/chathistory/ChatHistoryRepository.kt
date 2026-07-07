package sg.edu.nus.iss.client.chathistory

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import sg.edu.nus.iss.client.chatbot.ChatMessage

class ChatHistoryRepository(store: BoxStore) {

    private val box: Box<ChatMessageEntity> = store.boxFor(ChatMessageEntity::class.java)

    companion object {
        // Highest number of chat messages kept in persistent storage
        private const val MAX_STORED_MESSAGES = 2500

        // Number of most recent messages reloaded into the chat window
        // when the app restarts
        private const val DISPLAYED_MESSAGE_COUNT = 30
    }

    fun getRecentHistory(limit: Int = DISPLAYED_MESSAGE_COUNT): List<ChatMessage> {
        val query = box.query()
            .order(ChatMessageEntity_.timestamp, QueryBuilder.DESCENDING)
            .build()
        val recent = query.find(0, limit.toLong())
        query.close()
        return recent.reversed().map { it.toChatMessage() }
    }

    fun saveMessage(message: ChatMessageEntity, limit: Int = MAX_STORED_MESSAGES) {
        box.put(message)
        if (box.count() > limit) {
            val query = box.query()
                .order(ChatMessageEntity_.timestamp)
                .build()
            val oldest = query.findFirst()
            query.close()
            oldest?.let { box.remove(it) }
        }
    }

    // Finds past messages whose embedding is closest in meaning to the
    // given query embedding
    fun searchMessages(queryEmbedding: FloatArray, limit: Int = 5): List<ChatMessage> {
        val query = box.query(
            ChatMessageEntity_.embedding.nearestNeighbors(queryEmbedding, limit)
        ).build()
        val results = query.find()
        query.close()
        return results.map { it.toChatMessage() }
    }

    // Removes every stored chat message from the database, without
    // affecting any other data such as the dish records
    fun clearAllMessages() {
        box.removeAll()
    }
}

fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(text = text, isUser = isUser)
}