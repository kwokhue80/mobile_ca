package sg.edu.nus.iss.client.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionManager(context: Context) {
    data class RecommendationHistoryEntry(
        val recommendation: String,
        val generatedAt: String
    )

    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    companion object {
        private const val KEY_STORE_PROVIDER = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

        // Key for Biometric Login
        private const val BIOMETRIC_KEY_ALIAS = "biometric_jwt_key"
        // Key for Non-Biometric Login
        private const val STANDARD_KEY_ALIAS = "standard_jwt_key"

        private const val PREF_ENCRYPTED_TOKEN = "encrypted_auth_token"
        private const val PREF_IV = "encryption_iv"
        private const val PREF_STANDARD_TOKEN = "standard_auth_token"
        private const val PREF_STANDARD_IV = "standard_encryption_iv"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_RECOMMENDATION_SIGNATURE = "recommendation_signature"
        private const val PREF_RECOMMENDATION_UNREAD_COUNT = "recommendation_unread_count"
        private const val PREF_LATEST_RECOMMENDATION_TEXT = "latest_recommendation_text"
        private const val PREF_LATEST_RECOMMENDATION_TIME = "latest_recommendation_time"
        private const val PREF_RECOMMENDATION_HISTORY = "recommendation_history"
        private const val MAX_RECOMMENDATION_HISTORY = 30

        @Volatile
        private var inMemoryToken: String? = null
    }

    // --- Biometric Toggle State --- //
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PREF_BIOMETRIC_ENABLED, enabled)
        }
    }
    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }

    // --- Key Generation and Encryption --- //
    private fun generateKeyIfNecessary(alias: String, requireBiometrics: Boolean) {
        val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER).apply { load(null) }

        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER)
            val specBuilder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

            if (requireBiometrics) {
                specBuilder.setUserAuthenticationRequired(true)
                specBuilder.setInvalidatedByBiometricEnrollment(true)
            }

            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        }
    }
    private fun getSecretKey(alias: String, requireBiometrics: Boolean): SecretKey {
        generateKeyIfNecessary(alias, requireBiometrics)
        val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(alias, null) as SecretKey
    }

    // --- Biometric Cipher Initialization --- //
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(BIOMETRIC_KEY_ALIAS, true))
        return cipher
    }
    fun getInitializedCipherForDecryption(): Cipher {
        val ivString = sharedPreferences.getString(PREF_IV, null)
            ?: throw IllegalStateException("No IV found. Token has not been encrypted yet.")

        val iv = Base64.decode(ivString, Base64.DEFAULT)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

        try {
            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(BIOMETRIC_KEY_ALIAS, true),
                GCMParameterSpec(128, iv)
            )
        } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
            clearSession()
            setBiometricEnabled(false)
            throw e
        }
        return cipher
    }

    // --- Token Storage & Retrieval --- //
    fun saveEncryptedAuthToken(token: String, cipher: Cipher?) {
        inMemoryToken = token

        if (cipher != null) {
            // Biometric Login Token
            val encryptedData = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            sharedPreferences.edit {
                putString(PREF_ENCRYPTED_TOKEN, Base64.encodeToString(encryptedData, Base64.DEFAULT))
                putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
                remove(PREF_STANDARD_TOKEN)
                remove(PREF_STANDARD_IV)
            }
        } else {
            // Standard Login Token
            val standardCipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            standardCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(STANDARD_KEY_ALIAS, false))
            val encryptedData = standardCipher.doFinal(token.toByteArray(Charsets.UTF_8))

            sharedPreferences.edit {
                putString(PREF_STANDARD_TOKEN, Base64.encodeToString(encryptedData, Base64.DEFAULT))
                putString(PREF_STANDARD_IV, Base64.encodeToString(standardCipher.iv, Base64.DEFAULT))
                remove(PREF_ENCRYPTED_TOKEN)
                remove(PREF_IV)
            }
        }
    }

    fun getDecryptedAuthToken(cipher: Cipher?): String {
        if (cipher == null) {
            // Decryption  of Standard Token
            val encryptedString = sharedPreferences.getString(PREF_STANDARD_TOKEN, null)
            val ivString = sharedPreferences.getString(PREF_STANDARD_IV, null)
            if (encryptedString == null || ivString == null) {
                throw IllegalStateException("No Standard Token Found.")
            }

            val standardCipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            standardCipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(STANDARD_KEY_ALIAS, false),
                GCMParameterSpec(128, Base64.decode(ivString, Base64.DEFAULT))
            )
            val decryptedData = standardCipher.doFinal(Base64.decode(encryptedString, Base64.DEFAULT))
            val token = String(decryptedData, Charsets.UTF_8)
            inMemoryToken = token
            return token
        }

        // Decryption of Biometric Token
        val encryptedTokenBase64 = sharedPreferences.getString(PREF_ENCRYPTED_TOKEN, null)
            ?: throw IllegalStateException("No Encrypted Token Found.")
        val decryptedData = cipher.doFinal(Base64.decode(encryptedTokenBase64, Base64.DEFAULT))
        val token = String(decryptedData, Charsets.UTF_8)
        inMemoryToken = token
        return token
    }
    fun getDecryptedTokenFromMemory(): String? {
        if (inMemoryToken == null && sharedPreferences.contains(PREF_STANDARD_TOKEN)) {
            // Recovery of Standard Token from Local Disk if Memory is Empty
            try {
                getDecryptedAuthToken(null)
            } catch (e: Exception) {
                throw e
            }
        }
        return inMemoryToken
    }
    fun clearSession() {
        inMemoryToken = null
        sharedPreferences.edit {
            remove(PREF_ENCRYPTED_TOKEN)
            remove(PREF_IV)
            remove(PREF_STANDARD_TOKEN)
            remove(PREF_STANDARD_IV)
            remove(PREF_RECOMMENDATION_SIGNATURE)
            remove(PREF_RECOMMENDATION_UNREAD_COUNT)
            remove(PREF_LATEST_RECOMMENDATION_TEXT)
            remove(PREF_LATEST_RECOMMENDATION_TIME)
            remove(PREF_RECOMMENDATION_HISTORY)
        }
    }

    fun getRecommendationSignature(): String? {
        // Signature uniquely identifies the latest recommendation payload.
        return sharedPreferences.getString(PREF_RECOMMENDATION_SIGNATURE, null)
    }

    fun setRecommendationSignature(signature: String) {
        // Persist signature to avoid duplicate unread increments.
        sharedPreferences.edit {
            putString(PREF_RECOMMENDATION_SIGNATURE, signature)
        }
    }

    fun getUnreadRecommendationCount(): Int {
        // Returns unread recommendation badge count.
        return sharedPreferences.getInt(PREF_RECOMMENDATION_UNREAD_COUNT, 0)
    }

    fun incrementUnreadRecommendationCount() {
        // Atomically bump unread counter by one.
        val currentCount = getUnreadRecommendationCount()
        sharedPreferences.edit {
            putInt(PREF_RECOMMENDATION_UNREAD_COUNT, currentCount + 1)
        }
    }

    fun clearUnreadRecommendationCount() {
        // Reset unread counter when inbox is viewed.
        sharedPreferences.edit {
            putInt(PREF_RECOMMENDATION_UNREAD_COUNT, 0)
        }
    }

    fun getLatestRecommendationText(): String? {
        // Retrieve cached recommendation text for inbox display.
        return sharedPreferences.getString(PREF_LATEST_RECOMMENDATION_TEXT, null)
    }

    fun getLatestRecommendationTime(): String? {
        // Retrieve cached generation time for inbox display.
        return sharedPreferences.getString(PREF_LATEST_RECOMMENDATION_TIME, null)
    }

    fun setLatestRecommendation(text: String, generatedAt: String) {
        // Store latest recommendation payload shown in notification inbox.
        sharedPreferences.edit {
            putString(PREF_LATEST_RECOMMENDATION_TEXT, text)
            putString(PREF_LATEST_RECOMMENDATION_TIME, generatedAt)
        }
    }

    fun upsertRecommendationAndDetectNew(recommendationText: String, generatedAt: String): Boolean {
        // Build stable signature from server timestamp and message.
        val text = recommendationText.trim()
        val signature = "$generatedAt|$text"
        val previousSignature = getRecommendationSignature()
        val previousText = getLatestRecommendationText()

        if (previousSignature.isNullOrBlank()) {
            // First payload initializes cache without triggering unread state.
            setRecommendationSignature(signature)
            setLatestRecommendation(text, generatedAt)
            prependRecommendationHistory(text, generatedAt)
            return false
        }

        // If the signature is identical, it's definitely not new.
        if (signature == previousSignature) {
            return false
        }

        // If the text is identical to the last one, even if the timestamp is different,
        // we update the timestamp in our cache but don't treat it as a "new" unread event
        // to avoid spamming the user with the same message.
        if (text == previousText) {
            setRecommendationSignature(signature)
            setLatestRecommendation(text, generatedAt)
            // We don't prepend to history or increment unread count if text is same.
            return false
        }

        // New payload (different text) updates cache and increments unread counter.
        setRecommendationSignature(signature)
        setLatestRecommendation(text, generatedAt)
        prependRecommendationHistory(text, generatedAt)
        incrementUnreadRecommendationCount()
        return true
    }

    fun getRecommendationHistory(): List<RecommendationHistoryEntry> {
        val json = sharedPreferences.getString(PREF_RECOMMENDATION_HISTORY, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(json)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    add(
                        RecommendationHistoryEntry(
                            recommendation = obj.optString("recommendation", ""),
                            generatedAt = obj.optString("generatedAt", "")
                        )
                    )
                }
            }.filter { it.recommendation.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun prependRecommendationHistory(recommendation: String, generatedAt: String) {
        val history = getRecommendationHistory().toMutableList()
        history.removeAll { it.recommendation == recommendation && it.generatedAt == generatedAt }
        history.add(0, RecommendationHistoryEntry(recommendation = recommendation, generatedAt = generatedAt))

        val trimmedHistory = history.take(MAX_RECOMMENDATION_HISTORY)
        val jsonArray = JSONArray()
        trimmedHistory.forEach { entry ->
            jsonArray.put(
                JSONObject()
                    .put("recommendation", entry.recommendation)
                    .put("generatedAt", entry.generatedAt)
            )
        }

        sharedPreferences.edit {
            putString(PREF_RECOMMENDATION_HISTORY, jsonArray.toString())
        }
    }

    fun removeRecommendationFromHistory(recommendation: String, generatedAt: String) {
        // Remove a single history item when user dismisses/cancels a recommendation.
        val history = getRecommendationHistory().toMutableList()
        val removed = history.removeAll {
            it.recommendation == recommendation && it.generatedAt == generatedAt
        }
        if (!removed) return

        val jsonArray = JSONArray()
        history.forEach { entry ->
            jsonArray.put(
                JSONObject()
                    .put("recommendation", entry.recommendation)
                    .put("generatedAt", entry.generatedAt)
            )
        }

        sharedPreferences.edit {
            putString(PREF_RECOMMENDATION_HISTORY, jsonArray.toString())
        }
    }
}