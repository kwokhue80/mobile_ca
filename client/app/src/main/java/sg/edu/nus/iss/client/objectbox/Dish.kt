package sg.edu.nus.iss.client.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType

@Entity
data class Dish (
    @Id var id: Long = 0,
    var name: String = "",
    var sourceFile: String = "",
    var content: String = "",

    // embedding model all-MiniLM-L6-v2 outputs a 384-dimensional float vector
    // Build HNSW index using Cosine Distance
    @HnswIndex(dimensions = 384, distanceType = VectorDistanceType.COSINE)
    var embedding: FloatArray? = null
)