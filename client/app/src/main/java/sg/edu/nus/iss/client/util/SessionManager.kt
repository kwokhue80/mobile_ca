package sg.edu.nus.iss.client.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    // private var inMemoryToken: String? = null

    companion object {
        private const val KEY_STORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "biometric_jwt_key"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

        private const val PREF_ENCRYPTED_TOKEN = "encrypted_auth_token"
        private const val PREF_IV = "encryption_iv"

        @Volatile
        private var inMemoryToken: String? = null
    }

    private fun generateKeyIfNecessary() {
        val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    private fun getSecretKey(): SecretKey {
        generateKeyIfNecessary()
        val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher
    }
    fun getInitializedCipherForDecryption(): Cipher {
        val ivString = sharedPreferences.getString(PREF_IV, null)
            ?: throw IllegalStateException("No IV found. Token has not been encrypted yet.")

        val iv = Base64.decode(ivString, Base64.DEFAULT)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return cipher
    }
    fun saveEncryptedAuthToken(token: String, cipher: Cipher) {

        val encryptedData = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encryptedTokenBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT)
        val ivBase64 = Base64.encodeToString(cipher.iv, Base64.DEFAULT)

        sharedPreferences.edit {
            putString(PREF_ENCRYPTED_TOKEN, encryptedTokenBase64)
            putString(PREF_IV, ivBase64)
        }

        inMemoryToken = token
    }

    fun getDecryptedAuthToken(cipher: Cipher): String {
        val encryptedTokenBase64 = sharedPreferences.getString(PREF_ENCRYPTED_TOKEN, null)
            ?: throw IllegalStateException("No encrypted token found.")

        val encryptedData = Base64.decode(encryptedTokenBase64, Base64.DEFAULT)
        val decryptedData = cipher.doFinal(encryptedData)
        val decryptedToken = String(decryptedData, Charsets.UTF_8)

        inMemoryToken = decryptedToken

        return decryptedToken
    }
    fun getDecryptedTokenFromMemory(): String? {
        return inMemoryToken
    }
    fun clearSession() {
        inMemoryToken = null
        sharedPreferences.edit {
            remove(PREF_ENCRYPTED_TOKEN)
            remove(PREF_IV)
        }
    }
}