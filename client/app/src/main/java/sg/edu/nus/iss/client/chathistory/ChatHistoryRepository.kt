package sg.edu.nus.iss.client.chathistory

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import sg.edu.nus.iss.client.chatbot.ChatMessage

class ChatHistoryRepository(store: BoxStore) {

    private val box: Box<ChatMessageEntity> = store.boxFor(ChatMessageEntity::class.java)

    // Returns the most recent messages in order from oldest to newest,
    // suitable for populating the chat display after app restart.
    fun getRecentMessages(limit: Int = 30): List<ChatMessage> {
        val query = box.query()
            .order(ChatMessageEntity_.timestamp, QueryBuilder.DESCENDING)
            .build()
        val recent = query.find(0, limit.toLong())
        query.close()
        return recent.reversed().map { it.toChatMessage() }
    }

    // Stores a new message and removes the oldest entry once the limit is passed.
    fun saveMessage(message: ChatMessageEntity, limit: Int = 30) {
        box.put(message)
        if (box.count() > limit) {
            val query = box.query()
                .order(ChatMessageEntity_.timestamp) // ascending by default
                .build()
            val oldest = query.findFirst()
            query.close()
            oldest?.let { box.remove(it) }
        }
    }
}

// Converts a stored entity back into the plain display model used by the UI
fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(text = text, isUser = isUser)
}