package sg.edu.nus.iss.client.embedding

// Note: This file was generated entirely by Claude by Soo Kwok Heng
// Reason for AI use: This code requires knowledge about how the embedding
// model works exactly
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxEmbeddingModel(
    context: Context,
    modelAssetPath: String = "embedding/all-minilm-l6-v2-q.onnx",
    tokenizerAssetPath: String = "embedding/all-minilm-l6-v2-q-tokenizer.json"
) {
    // The tokenizer converts raw text into the numeric IDs the model
    // actually understands, following the steps defined in
    // BertWordPieceTokenizer.
    private val tokenizer = BertWordPieceTokenizer(context, tokenizerAssetPath)

    // The ONNX Runtime environment manages the underlying resources
    // needed to run a machine learning model on the device.
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    // The session represents the actual loaded embedding model, ready
    // to accept input and produce output.
    private val session: OrtSession

    init {
        // Loads the raw model file from the app's assets folder and
        // uses it to start a new model session.
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    // Converts a piece of text into a 384-number vector representing
    // its meaning, which can then be compared against other vectors
    // using cosine distance.
    fun embed(text: String): FloatArray {
        // Step 1: turn the text into a sequence of numeric token IDs.
        val inputIds = tokenizer.encode(text)
        val seqLen = inputIds.size

        // Step 2: build two extra arrays required by the model alongside
        // the token IDs. attentionMask marks every position as real
        // content (1), since no padding is being used here. tokenTypeIds
        // marks every position as belonging to a single sentence (0),
        // since only one piece of text is being processed at a time.
        val attentionMask = LongArray(seqLen) { 1L }
        val tokenTypeIds = LongArray(seqLen) { 0L }

        // Describes the shape of the input data: one sentence, made up
        // of seqLen individual tokens.
        val shape = longArrayOf(1, seqLen.toLong())

        // Step 3: wrap each array into a tensor, the specific data
        // format ONNX Runtime requires for running a model. The nested
        // "use" blocks make sure each tensor's underlying memory gets
        // released automatically once it is no longer needed.
        OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape).use { inputIdsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape).use { attentionMaskTensor ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape).use { tokenTypeIdsTensor ->

                    // Step 4: gather the three tensors under the input
                    // names the model expects to receive them as.
                    val inputs = mapOf(
                        "input_ids" to inputIdsTensor,
                        "attention_mask" to attentionMaskTensor,
                        "token_type_ids" to tokenTypeIdsTensor
                    )

                    // Step 5: run the model itself, producing a result
                    // for every token in the input sentence.
                    session.run(inputs).use { results ->
                        @Suppress("UNCHECKED_CAST")
                        val lastHiddenState = results[0].value as Array<Array<FloatArray>> // [1, seqLen, 384]

                        // Step 6: combine the individual per-token results
                        // into a single 384-number vector representing the
                        // whole sentence's meaning.
                        return meanPoolAndNormalize(lastHiddenState[0], attentionMask)
                    }
                }}}
    }

    // Combines the model's per-token output into one single vector,
    // then scales that vector to a standard length. This second step
    // is what allows cosine distance comparisons to work correctly
    // between different vectors later on.
    private fun meanPoolAndNormalize(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val dim = tokenEmbeddings[0].size
        val summed = FloatArray(dim)
        var maskSum = 0f

        // Adds up the values from every token position, position by
        // position, across all 384 numbers.
        for (i in tokenEmbeddings.indices) {
            val maskValue = attentionMask[i].toFloat()
            maskSum += maskValue
            for (d in 0 until dim) {
                summed[d] += tokenEmbeddings[i][d] * maskValue
            }
        }

        // Divides the summed values by the number of tokens, producing
        // the average (mean) value at each of the 384 positions.
        val mean = FloatArray(dim) { summed[it] / maskSum }

        // Calculates the overall length (magnitude) of the resulting
        // vector, which is needed to scale it down to a standard length.
        var norm = 0f
        for (v in mean) norm += v * v
        norm = sqrt(norm)

        // Divides every value in the vector by its overall length,
        // producing the final vector used for similarity comparisons.
        return FloatArray(dim) { mean[it] / norm }
    }

    // Releases the resources held by the model session once the
    // embedding model is no longer needed, such as when the app closes.
    fun close() {
        session.close()
    }
}