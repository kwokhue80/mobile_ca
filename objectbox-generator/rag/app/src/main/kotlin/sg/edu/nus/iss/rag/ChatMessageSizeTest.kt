package sg.edu.nus.iss.rag

// AUthor: Soo Kwok Heng with significant guidance from Claude
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel

// A separate copy of the embedding model, kept isolated from RAGApplication
// so this size-testing script does not require any changes to that file
private val testEmbeddingModel = AllMiniLmL6V2QuantizedEmbeddingModel()

private fun embed(text: String): FloatArray {
    return testEmbeddingModel.embed(text).content().vector()
}

fun testChatMessageDatabaseSize() {
    val testStore = MyObjectBox.builder()
        .name("chat-size-test")
        .build()

    val messageBox = testStore.boxFor(ChatMessageEntity::class.java)

    val sampleTexts = listOf(
        "Hi",
        "What is laksa?",
        "How many calories are in char kway teow?",
        "I had a plate of chicken rice for lunch today.",
        "Can you tell me if pad thai contains peanuts, since I have a mild allergy?",
        "What is a good low sodium option among the Vietnamese dishes listed?",
        "Thanks, that was helpful.",
        "Is there a dessert option with less sugar than mango sticky rice?"
    )

    for ((index, text) in sampleTexts.withIndex()) {
        val message = ChatMessageEntity(
            text = text,
            isUser = (index % 2 == 0),
            embedding = embed(text)
        )
        messageBox.put(message)
    }

    testStore.close()
    println("Test database created with ${sampleTexts.size} messages.")
}

fun main() {
    testChatMessageDatabaseSize()
}