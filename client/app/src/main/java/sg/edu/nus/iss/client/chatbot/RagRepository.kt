package sg.edu.nus.iss.client.chatbot

import sg.edu.nus.iss.client.backend.BackendConfig
import sg.edu.nus.iss.client.backend.BackendRepository
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.Dish
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.openrouter.OpenRouterClient

class RagRepository(
    private val embeddingModel: OnnxEmbeddingModel,
    private val dishRepository: DishRepository,
    private val openRouterClient: OpenRouterClient,
    private val backendRepository: BackendRepository
) {
    suspend fun answer(
        query: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        useVectorSearch: Boolean = true,
        topK: Int = 3
    ): String {
        var context = ""
        var localMatchFound = false

        if (useVectorSearch) {
            val queryVector = embeddingModel.embed(query)
            val topChunks = dishRepository.retrieve(queryVector, topK)

            // Keep only results that are actually a close match, not
            // just whatever happened to be closest out of a bad bunch.
            val confidentMatches = topChunks.filter { (_, distance) -> distance < DISTANCE_THRESHOLD }

            if (confidentMatches.isNotEmpty()) {
                context = buildContext(confidentMatches)
                localMatchFound = true
            }
        }

        return if (BackendConfig.USE_BACKEND) {
            backendRepository.answer(query, context, localMatchFound, conversationHistory)
        } else {
            val prompt = buildPrompt(query, context, conversationHistory)
            openRouterClient.chatCompletion(prompt)
        }
    }

    private fun buildContext(topChunks: List<Pair<Dish, Double>>): String {
        return topChunks.mapIndexed { i, (dish, score) ->
            "Dish ${i + 1} (${dish.name}):\n${dish.content}"
        }.joinToString("\n\n")
    }

    private fun buildPrompt(query: String, context: String, conversationHistory: List<ChatMessage>): String {
        val historyText = conversationHistory.joinToString("\n") { msg ->
            if (msg.isUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
        }

        return """
        Role:
        You are a supportive fitness and wellness AI assistant helping users track diet and nutrition.

        Task:
        Answer the user's question using ONLY the food data in the Context below. If the item isn't in the Context, politely say you don't have nutritional records for it.

        Rules:
        - Keep your answer SHORT: 3-4 sentences maximum, or a few short bullet points.
        - State calories, protein, carbs, and fat clearly and briefly.
        - Do not invent numbers. Only use data from the Context.
        - If the dish is high in sodium, sugar, or saturated fat, add ONE brief cautionary tip.
        - Do not mention source files, distance scores, chunk numbers, or the word "food profile."
        - Be warm and friendly, but concise — no long explanations.
        - If the user asks about the CHATBOT's own identity (e.g. "who are you", "tell me about yourself", "what is your name"), respond with a short note about your role as a wellness chatbot with no name.
        - If the user asks about THEMSELVES (e.g. "what is my name", "what did I say earlier") — look at the "Conversation so far" section below and answer using what has actually been said. Never confuse a question about the user with a question about the chatbot.

        Conversation so far:
        $historyText

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