package sg.edu.nus.iss.client.chatbot

import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.Dish
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.openrouter.OpenRouterClient

class RagRepository(
    private val embeddingModel: OnnxEmbeddingModel,
    private val dishRepository: DishRepository,
    private val openRouterClient: OpenRouterClient
) {
    suspend fun answer(
        query: String,
        conversationHistory: List<ChatMessage> = emptyList(), topK: Int = 3
    ): String {
        val queryVector = embeddingModel.embed(query)
        val topChunks = dishRepository.retrieve(queryVector, topK)

        // this line is for testing
        android.util.Log.d("RagRepository", "Retrieved dishes: ${topChunks.map { it.first.name }}")

        val context = buildContext(topChunks)
        val prompt = buildPrompt(query, context, conversationHistory)
        return openRouterClient.chatCompletion(prompt)
    }

    private fun buildContext(topChunks: List<Pair<Dish, Double>>): String {
        return topChunks.mapIndexed { i, (dish, score) ->
            "Dish ${i + 1} (${dish.name}):\n${dish.content}"
        }.joinToString("\n\n")
    }
    private fun buildPrompt(
        query: String,
        context: String,
        conversationHistory: List<ChatMessage>
    ): String {
        // Turn the message list into a simple readable transcript
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
        - If the user asks about THEMSELVES (e.g. "what is my name", "what did I say earlier") — look at the "Conversation so far" section below and answer using what they've actually told you. Never confuse a question about the user with a question about yourself.
        Conversation so far:
        $historyText

        Context:
        $context

        Question: $query

        Answer:
    """.trimIndent()
    }
}