package sg.edu.nus.iss.client.backend

data class BackendChatRequest(
    val query: String
)

// The backend might reply in one of two ways:
// 1. "Here's your final answer" (done!)
// 2. "I need you to search your local vector db with this embedding" (needs a follow-up)
data class BackendChatResponse(
    val status: String,              // e.g. "done" or "needs_vector_search"
    val answer: String? = null,      // present if status == "done"
    val queryEmbedding: List<Float>? = null  // present if status == "needs_vector_search"
)

data class VectorResultsRequest(
    val originalQuery: String,
    val matchedDishes: List<MatchedDish>
)

data class MatchedDish(
    val name: String,
    val content: String,
    val distance: Double
)