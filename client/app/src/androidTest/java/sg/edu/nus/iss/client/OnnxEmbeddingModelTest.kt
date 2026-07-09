package sg.edu.nus.iss.client

// Author: Soo Kwok Heng with significant guidance from Claude
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel

@RunWith(AndroidJUnit4::class)
class OnnxEmbeddingModelTest {

    @Test
    fun embed_producesNormalized384DimVector() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = OnnxEmbeddingModel(context)

        val vector = model.embed("chicken rice")

        android.util.Log.d("EmbedCompare", "size=${vector.size}")
        android.util.Log.d("EmbedCompare", vector.take(10).joinToString(", "))

        assertEquals(384, vector.size)

        val sumOfSquares = vector.sumOf { (it * it).toDouble() }
        assertEquals(1.0, sumOfSquares, 0.01) // allow small float tolerance

        model.close()
    }
}