package sg.edu.nus.iss.client.chatbot

import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.openrouter.OpenRouterClient
import java.util.Locale

class RagRepository(
    private val embeddingModel: OnnxEmbeddingModel,
    private val dishRepository: DishRepository,
    private val openRouterClient: OpenRouterClient
) {

    suspend fun answer(query: String, topK: Int = 3): String {
        val queryVector = embeddingModel.embed(query)
        val topChunks = dishRepository.retrieve(queryVector, topK)

        // this line is for testing
        android.util.Log.d("RagRepository", "Retrieved dishes: ${topChunks.map { it.first.name }}")

        val context = topChunks.mapIndexed { i, (doc, score) ->
            "Dish Context Profile ${i + 1} (Source File: ${doc.sourceFile}, Cosine Distance: ${
                String.format(Locale.US, "%.4f", score)
            }):\n${doc.content}"
        }.joinToString("\n\n")

        val prompt = buildPrompt(query, context)

        return openRouterClient.chatCompletion(prompt)
    }

    private fun buildPrompt(query: String, context: String): String = """
        Role:
        You are an expert, supportive fitness and wellness AI assistant. Your purpose is to help users manage their dietary tracking by answering food analysis, calorie, macro-nutrition, allergy, and recipe questions accurately.

        Task:
        Answer the user's inquiry directly using the verified Singapore, Thai and Vietnamese food profiles provided in the Context below. Before providing the hard numbers on calories, protein, carbs, and fats, give a brief overview of the dish and its nutritional highlights. Before providing the macro-nutrition breakdown, state the serving size. If the food profile requested is absent or if you cannot logically infer the answer from the Context, tell the user politely that you do not have nutritional records for that item.

        Rules:
        - Give answers tailored to fitness tracking metrics (calories, protein, carbs, fats).
        - Stay clean, concise, clear, and beginner-friendly.
        - Do not invent numbers or nutritional values. Only use the data provided in the Context.
        - State the data source notes at the very end of your answer for transparency. Do not mention the source file, nor the cosine distance score, nor the chunk index in your answer. Also do not make any mention of "food profile" in your answer.
        - Use the notes in Dietary Considerations to provide gentle cautionary advice if the dish is high in sodium, sugar, or saturated fat. Offer practical tips for healthier consumption.

        Output style and format:
        - Be empathetic, friendly and encouraging in your tone.
        - If a dish is high in sodium, sugar, or saturated fat, provide a gentle cautionary note and offer a practical tip, eg. "Asking for less gravy or sauce can help reduce sodium intake."
        - Format macro items nicely with clean typography.
        - Avoid mentioning inner mechanics like system scores, distance values, chunks, or technical indices.

        Context:
        $context

        Question: $query

        Answer:
    """.trimIndent()
}