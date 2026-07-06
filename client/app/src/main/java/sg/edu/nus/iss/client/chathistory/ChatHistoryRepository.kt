package sg.edu.nus.iss.client.chathistory

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import sg.edu.nus.iss.client.chatbot.ChatMessage

class ChatHistoryRepository(store: BoxStore) {

    private val box: Box<ChatMessageEntity> = store.boxFor(ChatMessageEntity::class.java)

    fun getRecentMessages(limit: Int = 30): List<ChatMessage> {
        val query = box.query()
            .order(ChatMessageEntity_.timestamp, QueryBuilder.DESCENDING)
            .build()
        val recent = query.find(0, limit.toLong())
        query.close()
        return recent.reversed().map { it.toChatMessage() }
    }

    fun saveMessage(message: ChatMessageEntity, limit: Int = 30) {
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
}

fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(text = text, isUser = isUser)
}