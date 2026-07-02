package sg.edu.nus.iss.client.chat

import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMiniLMVectorizer {
    // Note: embedding model used in android app must
    // be the same model used for the generation of the ObjectBox vector db
    // for querying with vector db to work
    private val embeddingModel by lazy { AllMiniLmL6V2QuantizedEmbeddingModel() }

    suspend fun vectorize(text: String): FloatArray = withContext(Dispatchers.Default) {
        return@withContext try {
            embeddingModel.embed(text).content().vector()
        } catch (e: Exception) {
            e.printStackTrace()
            floatArrayOf() // returns empty array
        }
    }
}