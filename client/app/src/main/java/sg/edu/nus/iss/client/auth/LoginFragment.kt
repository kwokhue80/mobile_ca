package sg.edu.nus.iss.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentLoginBinding
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.util.BiometricHelper
import sg.edu.nus.iss.client.util.SessionManager
import java.util.concurrent.Executor

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var pendingTokenToSave: String? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var authApiService: AuthApiService

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var viewModel: LoginViewModel
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        setupRetrofit()
        setupBiometricPrompt()
        val factory = LoginViewModelFactory(authApiService)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailAddress.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        binding.btnFingerprint.setOnClickListener {
            checkBiometricSupportAndAuthenticate()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                        }
                        is LoginUiState.Loading -> {
                        }
                        is LoginUiState.Success -> {
                            pendingTokenToSave = state.token
                            authenticateToEncrypt()
                            viewModel.resetState()
                        }
                        is LoginUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(requireContext())

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.auth_error)}: $errString",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                val cipher = result.cryptoObject?.cipher

                if (cipher != null) {
                    try {
                        pendingTokenToSave?.let { token ->
                            sessionManager.saveEncryptedAuthToken(token, cipher)
                            pendingTokenToSave = null
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.token_encrypted),
                                Toast.LENGTH_SHORT
                            ).show()
                        } ?: run {
                            val decryptedToken = sessionManager.getDecryptedAuthToken(cipher)
                            Toast.makeText(
                                requireContext(),
                                "${getString(R.string.token_decrypted)}: $decryptedToken",
                                Toast.LENGTH_SHORT
                            ).show()

                            // TODO: Navigate to Main Activity Page
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.auth_error)}: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.auth_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_negativeButtonText))
            .build()
    }

    private fun authenticateToEncrypt() {
        try {
            if (biometricHelper.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {

                val cipher = sessionManager.getInitializedCipherForEncryption()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                biometricHelper.showBiometricPrompt(
                    cryptoObject = cryptoObject,
                    onSuccess = { unlockedCipher ->
                        if (unlockedCipher != null) {
                            pendingTokenToSave?.let { token ->
                                sessionManager.saveEncryptedAuthToken(token, unlockedCipher)
                                pendingTokenToSave = null
                                Toast.makeText(requireContext(), getString(R.string.token_encrypted), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(requireContext(), "Auth Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to init encryption: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    val cipher = sessionManager.getInitializedCipherForDecryption()
                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                    biometricPrompt.authenticate(promptInfo, cryptoObject)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "${getString(R.string.token_error)}: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_error_no_hardware), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_error_hw_unavailable), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_error_none_enrolled), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_error_security_update_required), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_error_unsupported), Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Toast.makeText(requireContext(), getString(R.string.biometric_status_unknown), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authApiService = retrofit.create(AuthApiService::class.java)
    }

    private fun performLogout() {
        sessionManager.clearSession()
        Toast.makeText(
            requireContext(),
            getString(R.string.logout_success),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}