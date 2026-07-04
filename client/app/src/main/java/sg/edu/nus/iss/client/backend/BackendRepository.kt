package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.objectbox.DishRepository

class BackendRepository(
    private val backendApi: BackendApi,
    private val dishRepository: DishRepository
) {
    suspend fun answer(query: String): String {
        // Step 1: ask backend what it wants to do
        val firstResponse = backendApi.sendQuery(BackendChatRequest(query))

        // Case A: backend already has a full answer (e.g. it used SQL data instead)
        if (firstResponse.status == "done" && firstResponse.answer != null) {
            return firstResponse.answer
        }

        // Case B: backend needs us to search the local vector db
        if (firstResponse.status == "needs_vector_search" && firstResponse.queryEmbedding != null) {
            val queryVector = firstResponse.queryEmbedding.toFloatArray()
            val results = dishRepository.retrieve(queryVector, topK = 3)

            val matchedDishes = results.map { (dish, score) ->
                MatchedDish(name = dish.name, content = dish.content, distance = score)
            }

            val secondResponse = backendApi.sendVectorResults(
                VectorResultsRequest(originalQuery = query, matchedDishes = matchedDishes)
            )

            return secondResponse.answer
                ?: throw IllegalStateException("Backend did not return a final answer.")
        }

        throw IllegalStateException("Unexpected backend response: $firstResponse")
    }
}