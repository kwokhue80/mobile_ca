package sg.edu.nus.iss.client.chathistory

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType

// Stored (in vector db) representation of a chat message, kept separate from the
// ChatMessage class used by the adapter that is meant for the UI
@Entity
data class ChatMessageEntity(
    @Id var id: Long = 0,
    var text: String = "",
    var isUser: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),

    @HnswIndex(dimensions = 384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null
)