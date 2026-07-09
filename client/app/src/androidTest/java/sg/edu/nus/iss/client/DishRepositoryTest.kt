package sg.edu.nus.iss.client

// Author: Soo Kwok Heng with significant guidance from Claude
import sg.edu.nus.iss.client.objectbox.DishRepository
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import sg.edu.nus.iss.client.RagApplication
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel

@RunWith(AndroidJUnit4::class)
class DishRepositoryTest {

    @Test
    fun retrieve_returnsRealDishesForKnownQuery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as RagApplication

        val dishRepository = DishRepository(app.boxStore)
        val embeddingModel = OnnxEmbeddingModel(context)

        val queryVector = embeddingModel.embed("chicken rice")
        val results = dishRepository.retrieve(queryVector, topK = 3)

        android.util.Log.d("DishRepositoryTest", "Got ${results.size} results")
        results.forEach { (dish, score) ->
            android.util.Log.d(
                "DishRepositoryTest",
                "name=${dish.name}, sourceFile=${dish.sourceFile}, distance=$score"
            )
        }

        assertTrue("Expected at least 1 result, got ${results.size}", results.isNotEmpty())
        assertEquals(3, results.size)

        results.forEach { (_, score) ->
            assertTrue("Distance should be between 0 and 2, got $score", score in 0.0..2.0)
        }

        embeddingModel.close()
    }
}