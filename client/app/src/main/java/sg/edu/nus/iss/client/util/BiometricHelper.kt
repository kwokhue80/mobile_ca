// Authors: Khairulanwar
package sg.edu.nus.iss.client.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import sg.edu.nus.iss.client.R
import javax.crypto.Cipher

class BiometricHelper(private val fragment: Fragment) {

    private val context: Context = fragment.requireContext()

    /**
     * Checks if the device has the necessary hardware and enrolled biometrics
     * to perform a BIOMETRIC_STRONG authentication.
     */
    fun canAuthenticate(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    /**
     * Builds and launches the Biometric Prompt.
     * * @param cryptoObject The cryptographic wrapper containing the initialized Cipher.
     * @param onSuccess Callback triggered when the physical scan is successful. Returns the unlocked Cipher.
     * @param onError Callback triggered on hardware errors, user cancellation, or unrecognized scans.
     */
    fun showBiometricPrompt(
        cryptoObject: BiometricPrompt.CryptoObject,
        onSuccess: (Cipher?) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            fragment,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result.cryptoObject?.cipher)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError(context.getString(R.string.auth_failed))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_title))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_negativeButtonText))
            .build()

        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }
}