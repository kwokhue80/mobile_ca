package sg.edu.nus.iss.client.embedding

// Note: This file was generated entirely by Claude by Soo Kwok Heng
// Reason for AI use: This code requires knowledge about how the embedding
// model works exactly
import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/**
 * Minimal WordPiece tokenizer compatible with the tokenizer.json bundled
 * alongside all-MiniLM-L6-v2 (a standard uncased BERT-style tokenizer).
 */
class BertWordPieceTokenizer(context: Context, assetPath: String) {

    // The vocabulary maps each known text piece (a full word, part of a
    // word, or punctuation mark) to a unique whole number ID. The
    // embedding model only understands these numbers, not raw text.
    private val vocab: Map<String, Int>

    // Special marker IDs required by BERT-style models:
    // [CLS] marks the start of the input, [SEP] marks the end,
    // and [UNK] stands in for any text piece not found in the vocabulary.
    private val clsId: Int
    private val sepId: Int
    private val unkId: Int
    private val unkToken: String

    init {
        // Reads the tokenizer.json file bundled with the app and parses
        // it into a JSON object, so the vocabulary can be read out of it.
        val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val model = root.getJSONObject("model")

        // Reads the placeholder text used for unknown tokens, defaulting
        // to "[UNK]" if the file does not specify one directly.
        unkToken = model.optString("unk_token", "[UNK]")

        // Copies every token-to-ID pair out of the JSON file into a
        // regular Kotlin map, since JSON objects are slower to read
        // from repeatedly compared to a plain HashMap.
        val vocabJson = model.getJSONObject("vocab")
        val map = HashMap<String, Int>(vocabJson.length())
        val keys = vocabJson.keys()
        while (keys.hasNext()) {
            val token = keys.next()
            map[token] = vocabJson.getInt(token)
        }
        vocab = map

        // Looks up the numeric ID for each special marker token.
        // If any of these are missing from the vocabulary, the app
        // cannot function correctly, so an error is raised immediately.
        clsId = vocab["[CLS]"] ?: error("[CLS] token not found in vocab")
        sepId = vocab["[SEP]"] ?: error("[SEP] token not found in vocab")
        unkId = vocab[unkToken] ?: error("Unknown token '$unkToken' not found in vocab")
    }

    /**
     * Tokenizes [text] and returns input_ids including [CLS]/[SEP],
     * truncated to [maxLength] total tokens.
     */
    fun encode(text: String, maxLength: Int = 256): LongArray {
        // Step 1: split the raw sentence into rough word-level pieces,
        // separating out punctuation along the way.
        val basicTokens = basicTokenize(text)

        // Step 2: break each rough word down further into smaller
        // known vocabulary pieces, collecting all resulting IDs.
        val wordpieceIds = mutableListOf<Int>()
        for (token in basicTokens) {
            wordpieceIds.addAll(wordpieceTokenize(token))
        }

        // Step 3: make room for the [CLS] and [SEP] markers by trimming
        // the token list down if it would otherwise exceed maxLength.
        val budget = maxLength - 2 // reserve room for [CLS] and [SEP]
        val truncated = if (wordpieceIds.size > budget) wordpieceIds.subList(0, budget) else wordpieceIds

        // Step 4: assemble the final sequence of IDs, with [CLS] at the
        // very start and [SEP] at the very end.
        val ids = ArrayList<Int>(truncated.size + 2)
        ids.add(clsId)
        ids.addAll(truncated)
        ids.add(sepId)

        // Converts the list of Int values into a LongArray, since the
        // embedding model expects its input in that specific format.
        return LongArray(ids.size) { ids[it].toLong() }
    }

    // Lowercases, strips accents, splits on whitespace and punctuation.
    private fun basicTokenize(text: String): List<String> {
        // Converts the text to lowercase, then splits accented letters
        // into a base letter plus a separate accent mark, so the accent
        // marks can be removed in the next step. For example, this
        // step would help treat an accented letter the same as its
        // plain, unaccented version.
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "") // strip combining accent marks

        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        // Moves whatever has been built up so far in "current" into the
        // final token list, then clears it to start building the next one.
        fun flush() {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
        }

        // Walks through the text one character at a time, splitting on
        // whitespace and punctuation, while grouping ordinary letters
        // together into words.
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

    // Checks whether a single character counts as punctuation, using a
    // combination of specific character code ranges and Java's built-in
    // character classification system.
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
        // Extremely long tokens are treated as unknown right away,
        // rather than attempting to break them down piece by piece.
        if (token.length > maxCharsPerWord) return listOf(unkId)

        val outputIds = mutableListOf<Int>()
        var start = 0

        // Repeatedly looks for the longest matching piece of the word
        // starting from the current position, shrinking the attempted
        // match length one character at a time until a known piece is
        // found in the vocabulary.
        while (start < token.length) {
            var end = token.length
            var matchedId: Int? = null

            while (start < end) {
                var substr = token.substring(start, end)
                // Any piece after the first one gets a "##" prefix, which
                // marks it as a continuation of the previous piece rather
                // than the start of a brand new word.
                if (start > 0) substr = "##$substr"
                val id = vocab[substr]
                if (id != null) {
                    matchedId = id
                    break
                }
                end--
            }

            // If no matching piece could be found at all for this
            // position, the whole word is treated as unknown.
            if (matchedId == null) return listOf(unkId)

            outputIds.add(matchedId)
            start = end
        }
        return outputIds
    }
}