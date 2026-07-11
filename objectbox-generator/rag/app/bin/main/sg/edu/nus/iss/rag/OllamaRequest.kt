package sg.edu.nus.iss.rag

// Author: Soo Kwok Heng
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class OllamaRequest (val model: String,
                          val prompt: String,
                          @EncodeDefault
                          val stream: Boolean = false){
}