package sg.edu.nus.iss.client.embedding

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/**
 * Minimal WordPiece tokenizer compatible with the tokenizer.json bundled
 * alongside all-MiniLM-L6-v2 (a standard uncased BERT-style tokenizer).
 */
class BertWordPieceTokenizer(context: Context, assetPath: String) {

    private val vocab: Map<String, Int>
    private val clsId: Int
    private val sepId: Int
    private val unkId: Int
    private val unkToken: String

    init {
        val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val model = root.getJSONObject("model")

        unkToken = model.optString("unk_token", "[UNK]")

        val vocabJson = model.getJSONObject("vocab")
        val map = HashMap<String, Int>(vocabJson.length())
        val keys = vocabJson.keys()
        while (keys.hasNext()) {
            val token = keys.next()
            map[token] = vocabJson.getInt(token)
        }
        vocab = map

        clsId = vocab["[CLS]"] ?: error("[CLS] token not found in vocab")
        sepId = vocab["[SEP]"] ?: error("[SEP] token not found in vocab")
        unkId = vocab[unkToken] ?: error("Unknown token '$unkToken' not found in vocab")
    }

    /**
     * Tokenizes [text] and returns input_ids including [CLS]/[SEP],
     * truncated to [maxLength] total tokens.
     */
    fun encode(text: String, maxLength: Int = 256): LongArray {
        val basicTokens = basicTokenize(text)
        val wordpieceIds = mutableListOf<Int>()
        for (token in basicTokens) {
            wordpieceIds.addAll(wordpieceTokenize(token))
        }

        val budget = maxLength - 2 // reserve room for [CLS] and [SEP]
        val truncated = if (wordpieceIds.size > budget) wordpieceIds.subList(0, budget) else wordpieceIds

        val ids = ArrayList<Int>(truncated.size + 2)
        ids.add(clsId)
        ids.addAll(truncated)
        ids.add(sepId)

        return LongArray(ids.size) { ids[it].toLong() }
    }

    // Lowercases, strips accents, splits on whitespace and punctuation.
    private fun basicTokenize(text: String): List<String> {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "") // strip combining accent marks

        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
        }

        for (ch in normalized) {
            when {
                ch.isWhitespace() -> flush()
                isPunctuation(ch) -> {
                    flush()
                    tokens.add(ch.toString())
                }
                else -> current.append(ch)
            }
        }
        flush()
        return tokens
    }

    private fun isPunctuation(ch: Char): Boolean {
        val code = ch.code
        return (code in 33..47) || (code in 58..64) ||
                (code in 91..96) || (code in 123..126) ||
                Character.getType(ch).let {
                    it == Character.CONNECTOR_PUNCTUATION.toInt() ||
                            it == Character.DASH_PUNCTUATION.toInt() ||
                            it == Character.START_PUNCTUATION.toInt() ||
                            it == Character.END_PUNCTUATION.toInt() ||
                            it == Character.OTHER_PUNCTUATION.toInt() ||
                            it == Character.MATH_SYMBOL.toInt()
                }
    }

    // Greedy longest-match-first WordPiece, using "##" continuation prefix.
    private fun wordpieceTokenize(token: String, maxCharsPerWord: Int = 200): List<Int> {
        if (token.length > maxCharsPerWord) return listOf(unkId)

        val outputIds = mutableListOf<Int>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var matchedId: Int? = null

            while (start < end) {
                var substr = token.substring(start, end)
                if (start > 0) substr = "##$substr"
                val id = vocab[substr]
                if (id != null) {
                    matchedId = id
                    break
                }
                end--
            }

            if (matchedId == null) return listOf(unkId)

            outputIds.add(matchedId)
            start = end
        }
        return outputIds
    }
}