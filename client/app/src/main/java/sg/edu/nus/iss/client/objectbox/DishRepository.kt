package sg.edu.nus.iss.client.objectbox

import io.objectbox.Box
import io.objectbox.BoxStore

class DishRepository(boxStore: BoxStore) {

    private val dishBox: Box<Dish> = boxStore.boxFor(Dish::class.java)

    /**
     * Finds the topK dishes whose embedding is closest to [queryVector]
     * using the HNSW cosine-distance index built on the Dish entity.
     *
     * Returns pairs of (Dish, cosineDistance) — lower distance = stronger match.
     */
    fun retrieve(queryVector: FloatArray, topK: Int = 3): List<Pair<Dish, Double>> {
        val results = dishBox.query()
            .nearestNeighbors(Dish_.embedding, queryVector, topK)
            .build()
            .findWithScores()

        return results.map { Pair(it.get(), it.score) }
    }

    fun getAllDishNames(): List<String> {
        return dishBox.all.map { it.name }
    }
}