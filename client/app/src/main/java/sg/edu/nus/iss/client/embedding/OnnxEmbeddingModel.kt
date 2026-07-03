package sg.edu.nus.iss.client.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxEmbeddingModel(
    context: Context,
    modelAssetPath: String = "embedding/all-minilm-l6-v2.onnx",
    tokenizerAssetPath: String = "embedding/all-minilm-l6-v2-tokenizer.json"
) {
    private val tokenizer = BertWordPieceTokenizer(context, tokenizerAssetPath)
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    fun embed(text: String): FloatArray {
        val inputIds = tokenizer.encode(text)
        val seqLen = inputIds.size
        val attentionMask = LongArray(seqLen) { 1L }
        val tokenTypeIds = LongArray(seqLen) { 0L }

        val shape = longArrayOf(1, seqLen.toLong())

        OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape).use { inputIdsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape).use { attentionMaskTensor ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape).use { tokenTypeIdsTensor ->

                    val inputs = mapOf(
                        "input_ids" to inputIdsTensor,
                        "attention_mask" to attentionMaskTensor,
                        "token_type_ids" to tokenTypeIdsTensor
                    )

                    session.run(inputs).use { results ->
                        @Suppress("UNCHECKED_CAST")
                        val lastHiddenState = results[0].value as Array<Array<FloatArray>> // [1, seqLen, 384]

                        return meanPoolAndNormalize(lastHiddenState[0], attentionMask)
                    }
                }}}
    }

    private fun meanPoolAndNormalize(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val dim = tokenEmbeddings[0].size
        val summed = FloatArray(dim)
        var maskSum = 0f

        for (i in tokenEmbeddings.indices) {
            val maskValue = attentionMask[i].toFloat()
            maskSum += maskValue
            for (d in 0 until dim) {
                summed[d] += tokenEmbeddings[i][d] * maskValue
            }
        }

        val mean = FloatArray(dim) { summed[it] / maskSum }

        var norm = 0f
        for (v in mean) norm += v * v
        norm = sqrt(norm)

        return FloatArray(dim) { mean[it] / norm }
    }

    fun close() {
        session.close()
    }
}