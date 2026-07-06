package sg.edu.nus.iss.client.chatbot

import sg.edu.nus.iss.client.backend.BackendConfig
import sg.edu.nus.iss.client.backend.BackendRepository
import sg.edu.nus.iss.client.chathistory.ChatHistoryRepository
import sg.edu.nus.iss.client.chathistory.ChatMessageEntity
import sg.edu.nus.iss.client.chathistory.FeatureFlags
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.Dish
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.openrouter.OpenRouterClient

class RagRepository(
    private val embeddingModel: OnnxEmbeddingModel,
    private val dishRepository: DishRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val openRouterClient: OpenRouterClient,
    private val backendRepository: BackendRepository
) {
    suspend fun answer(
        query: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        topK: Int = 3
    ): String {
        // The query only needs to be converted into a vector once, since
        // both the dish search and the chat history search rely on it
        val queryVector = embeddingModel.embed(query)

        var context = ""
        var localMatchFound = false

        if (FeatureFlags.ENABLE_DISH_VECTOR_SEARCH) {
            val topChunks = dishRepository.retrieve(queryVector, topK)

            topChunks.forEachIndexed { index, (dish, distance) ->
                android.util.Log.d("RagRepository", "Match ${index + 1}: ${dish.name} - distance $distance")
            }

            val confidentMatches = topChunks.filter { (_, distance) -> distance < DISTANCE_THRESHOLD }

            if (confidentMatches.isNotEmpty()) {
                context = buildContext(confidentMatches)
                localMatchFound = true
            }
        }

        // Look through previously saved messages for anything relevant
        // to the current question, separate from the recent messages
        // already tracked on screen
        var relevantPastMessages: List<ChatMessage> = emptyList()
        if (FeatureFlags.ENABLE_CHAT_HISTORY_PERSISTENCE) {
            relevantPastMessages = chatHistoryRepository.searchMessages(queryVector, limit = 3)
        }

        val answer = if (BackendConfig.USE_BACKEND) {
            backendRepository.answer(query, context, localMatchFound, conversationHistory)
        } else {
            val prompt = buildPrompt(query, context, conversationHistory, relevantPastMessages)
            openRouterClient.chatCompletion(prompt)
        }

        // Save both sides of this exchange, along with their embeddings,
        // so later questions can be matched against them
        if (FeatureFlags.ENABLE_CHAT_HISTORY_PERSISTENCE) {
            chatHistoryRepository.saveMessage(
                ChatMessageEntity(text = query, isUser = true, embedding = queryVector)
            )
            val answerVector = embeddingModel.embed(answer)
            chatHistoryRepository.saveMessage(
                ChatMessageEntity(text = answer, isUser = false, embedding = answerVector)
            )
        }

        return answer
    }

    private fun buildContext(topChunks: List<Pair<Dish, Double>>): String {
        return topChunks.mapIndexed { i, (dish, score) ->
            "Dish ${i + 1} (${dish.name}):\n${dish.content}"
        }.joinToString("\n\n")
    }

    private fun buildPrompt(
        query: String,
        context: String,
        conversationHistory: List<ChatMessage>,
        relevantPastMessages: List<ChatMessage>
    ): String {
        val recentText = conversationHistory.joinToString("\n") { msg ->
            if (msg.isUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
        }

        val relevantText = relevantPastMessages.joinToString("\n") { msg ->
            val speaker = if (msg.isUser) "user" else "assistant"
            "Earlier, the $speaker said: \"${msg.text}\""
        }

        return """
    ...

    Relevant earlier context:
    $relevantText

    Conversation so far:
    $recentText

    Context:
    $context

    Question: $query

    Answer:
    """.trimIndent()
    }

    companion object {
        // Cosine distance ranges from 0 (identical) to 2 (completely different).
        // A real match tends to score well under this cutoff, while
        // unrelated dishes tend to score much higher.
        private const val DISTANCE_THRESHOLD = 0.35
    }
}