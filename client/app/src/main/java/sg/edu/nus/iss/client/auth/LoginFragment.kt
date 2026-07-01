package sg.edu.nus.iss.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentLoginBinding
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.BiometricHelper
import sg.edu.nus.iss.client.util.SessionManager

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var pendingTokenToSave: String? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var authApiService: AuthApiService

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
        biometricHelper = BiometricHelper(this)
        authApiService = RetrofitClient.getApiService(requireContext())

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
                        is LoginUiState.Idle -> { }
                        is LoginUiState.Loading -> { }
                        is LoginUiState.Success -> {
                            pendingTokenToSave = state.token
                            authenticateToEncrypt()
                            viewModel.resetState()
                        }
                        is LoginUiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT)
                                .show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
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
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.token_encrypted),
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.auth_error)}: $errorMessage",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "${getString(R.string.token_error)}: ${e.message}",
                Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        try {
            if (biometricHelper.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                val cipher = sessionManager.getInitializedCipherForDecryption()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                biometricHelper.showBiometricPrompt(
                    cryptoObject = cryptoObject,
                    onSuccess = { unlockedCipher ->
                        if (unlockedCipher != null) {
                            sessionManager.getDecryptedAuthToken(unlockedCipher)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.token_decrypted),
                                Toast.LENGTH_SHORT)
                                .show()
                            navigateToMainActivity()
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.auth_error)}: $errorMessage",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.biometric_error_hw_unavailable),
                    Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "${getString(R.string.biometric_login_failed)}: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMainActivity() {
        // TODO: Replace with your actual navigation graph logic or Intent
        // Example:
        // val intent = Intent(requireContext(), MainActivity::class.java)
        // startActivity(intent)
        // requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}