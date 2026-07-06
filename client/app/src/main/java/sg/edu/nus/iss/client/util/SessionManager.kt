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
        }
    }
}