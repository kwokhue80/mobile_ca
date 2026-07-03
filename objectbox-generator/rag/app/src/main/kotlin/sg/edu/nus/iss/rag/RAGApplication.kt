
package sg.edu.nus.iss.rag

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import java.util.Scanner


object RAGApplication {
    private val DATA_DIR = File("data")
    private const val CHAT_MODEL = "llama3.1"

    // Loads the all-MiniLM-L6-v2 local model weights inside the JVM execution environment
    private val embeddingModel = AllMiniLmL6V2EmbeddingModel()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes in milliseconds
            connectTimeoutMillis = 30_000  // 30 seconds connection grace period
            socketTimeoutMillis = 300_000  // 5 minutes socket read timeout
        }
    }

    // Opens/creates a local DB directory named "objectbox-generator"
    private val boxStore: BoxStore by lazy {
        MyObjectBox.builder()
            .name("objectbox-generator")
            .build()
    }

    private val dishBox: Box<Dish> by lazy { boxStore.boxFor(Dish::class.java) }

    private fun embed(text: String): FloatArray {
        return embeddingModel.embed(text).content().vector()
    }

    private fun ingestToObjectBox(dirPath: File, reset: Boolean = true) {
        if (reset) {
            dishBox.removeAll()
        }

        val jsonFiles = dirPath.listFiles { _, name -> name.endsWith(".json") }
            ?: throw FileNotFoundException("No JSON files found in directory: ${dirPath.absolutePath}")

        val documentsToInsert = mutableListOf<Dish>()

        for (file in jsonFiles) {
            try {
                val jsonText = file.readText()
                val jsonElement = Json.parseToJsonElement(jsonText)

                // Extract items safely whether the file is a root JsonArray or a nested JsonObject
                val jsonArray = when (jsonElement) {
                    is JsonArray -> jsonElement
                    is JsonObject -> {
                        // Explicitly look for any key mapping directly to a list structure
                        val arrayKey =
                            jsonElement.keys.firstOrNull { key -> jsonElement[key] is JsonArray }
                        if (arrayKey != null) {
                            jsonElement[arrayKey] as JsonArray
                        } else {
                            JsonArray(listOf(jsonElement))
                        }
                    }
                    else -> continue
                }

                for (element in jsonArray) {
                    val dish = element.jsonObject
                    val name = dish["dish_name"]?.jsonPrimitive?.content ?: "Unknown Dish"
                    val cuisine = dish["cuisine"]?.jsonPrimitive?.content ?: "Unknown"
                    val portion = dish["portion_size"]?.jsonPrimitive?.content ?: "N/A"

                    // Safely extract text notes even if they are structured as arrays or primitives
                    val ingredients = when (val input = dish["ingredients_notes"]) {
                        is JsonArray -> input.joinToString(", ") { it.jsonPrimitive.content }
                        else -> input?.jsonPrimitive?.content ?: "N/A"
                    }

                    val dietaryInfo = when (val input = dish["dietary_considerations"]) {
                        is JsonArray -> input.joinToString("; ") { it.jsonPrimitive.content }
                        else -> input?.jsonPrimitive?.content ?: "N/A"
                    }

                    val dataSources = dish["data_source_note"]?.jsonPrimitive?.content ?: "N/A"

                    val nutrition = dish["nutrition"]?.jsonObject

                    // Check if nested calorie element is an object or primitive to avoid crashes
                    val calories = nutrition?.get("calories_kcal")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val protein = nutrition?.get("protein_g")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val carbs = nutrition?.get("carbohydrates_g")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val fat = nutrition?.get("saturated_fat_g")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val sodium = nutrition?.get("sodium_mg")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val sugar = nutrition?.get("sugars_g")?.let {
                        if (it is JsonObject) "N/A" else it.jsonPrimitive.contentOrNull?.toString()
                    } ?: "N/A"

                    val dishChunk = """
        Dish Name: $name. Cuisine: $cuisine. Portion Size: $portion. It contains $calories kcal of calories, $protein grams of protein, $carbs grams of carbohydrates, $fat grams of saturated fat, $sodium milligrams of sodium, and $sugar grams of sugar. Ingredients and Notes: $ingredients Dietary Considerations: $dietaryInfo. Data Sources: $dataSources.
    """.trimIndent()

                    val doc = Dish(
                        name = name,
                        sourceFile = file.name,
                        content = dishChunk,
                        embedding = embed(name)
                    )
                    documentsToInsert.add(doc)
                }
            } catch (e: Exception) {
                println("Skipping file ${file.name} due to parsing error: ${e.message}")
                e.printStackTrace() // This will help trace the exact field line structure if it breaks again
            }
        }

        dishBox.put(documentsToInsert)
        println("Successfully processed ${documentsToInsert.size} custom food items into ObjectBox tables.\n")
    }

    private fun retrieve(query: String, topK: Int = 3): List<Pair<Dish, Double>> {
        val queryVector = embed(query)

        // Executes native HNSW Vector search query indexing logic
        val results = dishBox.query()
            .nearestNeighbors(Dish_.embedding, queryVector, topK)
            .build()
            .findWithScores()

        return results.map { Pair(it.get(), it.score) }
    }

    private suspend fun answer(query: String, topChunks: List<Pair<Dish, Double>>): String {
        val context = topChunks.mapIndexed { i, (doc, score) ->
            "Dish Context Profile ${i + 1} (Source File: ${doc.sourceFile}, Cosine Distance: ${String.format(Locale.US, "%.4f", score)}):\n${doc.content}"
        }.joinToString("\n\n")

        val prompt = """
        Role:
        You are an expert, supportive fitness and wellness AI assistant. Your purpose is to help users manage their dietary tracking by answering food analysis, calorie, macro-nutrition, allergy, and recipe questions accurately.
        
        Task:
        Answer the user's inquiry directly using the verified Singapore, Thai and Vietnamese food profiles provided in the Context below. Before providing the hard numbers on calories, protein, carbs, and fats, give a brief overview of the dish and its nutritional highlights. Before providing the macro-nutrition breakdown, state the serving size. If the food profile requested is absent or if you cannot logically infer the answer from the Context, tell the user politely that you do not have nutritional records for that item.
        
        Rules:
        - Give answers tailored to fitness tracking metrics (calories, protein, carbs, fats).
        - Stay clean, concise, clear, and beginner-friendly.
        - Do not invent numbers or nutritional values. Only use the data provided in the Context.
        - State the data source notes at the very end of your answerfor transparency. Do not mention the source file, nor the cosine distance score, nor the chunk index in your answer. Also do not make any mention of "food profile" in your answer.
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

        val response: OllamaResponse = client.post("http://localhost:11434/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(OllamaRequest(model = CHAT_MODEL, prompt = prompt))
        }.body()

        return response.response
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        if (!DATA_DIR.exists()) {
            throw FileNotFoundException("Missing data directory: ${DATA_DIR.absolutePath}")
        }

        println("Ingesting structured meal logs into ObjectBox...")
        // TEMPORARY — for comparing against Android embedding, remove after checking
//        val testVector = embed("chicken rice")
//        println("JVM EMBEDDING (chicken rice), size=${testVector.size}")
//        println(testVector.take(10).joinToString(", "))
        // END TEMPORARY: TEST vector embedding
        ingestToObjectBox(DATA_DIR, reset = true)

        val scanner = Scanner(System.`in`)
        while (true) {
            print("\nAsk the Fitness Chatbot a question (or type 'exit'): ")
            val query = scanner.nextLine()?.trim() ?: ""
            if (query.lowercase() == "exit") break
            if (query.isEmpty()) continue

            val topChunks = retrieve(query, topK = 3)

            println("\n--- [System Match Logs Debugging] ---")
            topChunks.forEachIndexed { i, (doc, score) ->
                // Cosine Distance = 1 - Cosine Similarity. Lower distance = stronger match
                println("${i + 1}. Dish: ${doc.name} | Cosine Distance Score: ${String.format(Locale.US, "%.4f", score)}")
            }
            println("------------------------------------\n")

            println("Answer:")
            println(answer(query, topChunks))
        }

        boxStore.close()
        client.close()
    }
}