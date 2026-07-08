package sg.edu.nus.iss.client.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
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
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.databinding.FragmentLoginBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.BiometricHelper
import sg.edu.nus.iss.client.util.SessionManager

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
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
            android.util.Log.d("LoginFragment", "Login button clicked")
            // Toast.makeText(requireContext(), "Processing Login...", Toast.LENGTH_SHORT).show()
            val email = binding.etEmailAddress.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(email, password)
            }
        }
        // Define your full string sequence
        val fullText = "Don't have an account? Sign up"
        val spannableString = SpannableString(fullText)

        val startIndex = fullText.indexOf("Sign up")
        val endIndex = startIndex + "Sign up".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                           findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                           }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#00E5FF") // Keeps your custom cyan color
                ds.isFakeBoldText = true              // Keeps it bold
                ds.isUnderlineText = false            // Set to true if you want a classic underline link look!
            }
        }
        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textViewSignUp.text = spannableString
        binding.textViewSignUp.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        binding.btnFingerprint.setOnClickListener {
            checkBiometricSupportAndAuthenticate()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> { }
                        is LoginUiState.Loading -> {
                            Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show()
                        }
//                        is LoginUiState.Success -> {
//                            pendingTokenToSave = state.token
//                            authenticateToEncrypt()
//                            viewModel.resetState()
//                        }
                        is LoginUiState.Success -> {
                            // Save JWT token directly after successful login.
                            // AuthInterceptor will read this token and add:
                            // Authorization: Bearer <token>
                            sessionManager.saveEncryptedAuthToken(state.token, null)

                            // If the user previously enabled biometrics, prompt them
                            // to secure this brand-NEW token with their fingerprint right now.
                            if (sessionManager.isBiometricEnabled()) {
                                promptToSecureNewToken()
                            } else {
                                Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                                navigateToMainActivity()
                            }
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
        if (sessionManager.isBiometricEnabled()) {
            checkBiometricSupportAndAuthenticate()
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        // 1. Check if the user has opted in via the HomeFragment toggle
        if (!sessionManager.isBiometricEnabled()) {
            Toast.makeText(requireContext(), "Biometric login is not enabled for this account.", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricStatus = biometricHelper.canAuthenticate()
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            try {
                val cipher = sessionManager.getInitializedCipherForDecryption()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                biometricHelper.showBiometricPrompt(
                    cryptoObject = cryptoObject,
                    onSuccess = { unlockedCipher ->
                        if (unlockedCipher != null) {
                            sessionManager.getDecryptedAuthToken(unlockedCipher)
                            Toast.makeText(requireContext(), getString(R.string.token_decrypted), Toast.LENGTH_SHORT).show()
                            navigateToMainActivity()
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(requireContext(), "${getString(R.string.auth_error)}: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                Toast.makeText(requireContext(), "Biometrics changed for security. Please login with your password.", Toast.LENGTH_LONG).show()
            } catch (e: IllegalStateException) {
                Toast.makeText(requireContext(), "Session expired. Please log in with a password to refresh.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val message = when(biometricStatus) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> getString(R.string.biometric_error_none_enrolled)
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> getString(R.string.biometric_error_no_hardware)
                else -> getString(R.string.biometric_error_hw_unavailable)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptToSecureNewToken() {
        val status = biometricHelper.canAuthenticate()
        if (status == BiometricManager.BIOMETRIC_SUCCESS) {
            try {
                val cipher = sessionManager.getInitializedCipherForEncryption()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                biometricHelper.showBiometricPrompt(
                    cryptoObject = cryptoObject,
                    onSuccess = { unlockedCipher ->
                        val token = sessionManager.getDecryptedTokenFromMemory()
                        if (token != null && unlockedCipher != null) {
                            sessionManager.saveEncryptedAuthToken(token, unlockedCipher)
                        }
                        Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                        navigateToMainActivity()
                    },
                    onError = {
                        // If they cancel the prompt, let them in any way using the standard token
                        Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                        navigateToMainActivity()
                    }
                )
            } catch (e: Exception) {
                viewModel.resetState()
                navigateToMainActivity()
            }
        } else {
            viewModel.resetState()
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        // val intent = Intent(requireContext(), MainActivity::class.java)
        // startActivity(intent)
        // requireActivity().finish()

        // Testing convenience (requested explicitly): wipe today's wellness records on every
        // login so each test session starts clean, then force a fresh fetch. This is a
        // single-Activity app - DashboardViewModel/UserGoalsViewModel are scoped to
        // requireActivity(), so without this they'd persist stale data across a logout/login
        // cycle instead of being recreated. (ProfileViewModel doesn't need this: it's
        // fragment-scoped and already re-fetches every time the Profile screen is opened.)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.getApiService(requireContext()).resetToday()
            } catch (e: Exception) {
                // Best-effort - still proceed to Home even if this fails (e.g. offline).
            }
            ViewModelProvider(requireActivity())[DashboardViewModel::class.java].refreshToday()
            ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java].loadGoals()
            RouteManager.toHome(this@LoginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}