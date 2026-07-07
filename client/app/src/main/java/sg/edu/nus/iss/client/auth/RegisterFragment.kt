package sg.edu.nus.iss.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentRegisterBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.SessionManager

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var authApiService: AuthApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        authApiService = RetrofitClient.getApiService(requireContext())

        val factory = RegisterViewModelFactory(authApiService)
        viewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]

        // Submit registration details to backend register endpoint.
        binding.btnRegister.setOnClickListener {
            clearInlineErrors()
            val email = binding.etRegisterEmailAddress.text.toString()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etRegisterConfirmPassword.text.toString()
            viewModel.register(email, password, confirmPassword)
        }

        // Return existing users to login screen.
        binding.btnBackToLogin.setOnClickListener {
            RouteManager.toLoginFromRegister(this)
        }

        // Clear only the edited field error to keep feedback responsive while typing.
        binding.etRegisterEmailAddress.doAfterTextChanged {
            binding.etRegisterEmailAddress.error = null
        }
        binding.etRegisterPassword.doAfterTextChanged {
            binding.etRegisterPassword.error = null
        }
        binding.etRegisterConfirmPassword.doAfterTextChanged {
            binding.etRegisterConfirmPassword.error = null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RegisterUiState.Idle -> Unit
                        is RegisterUiState.Loading -> {
                            clearInlineErrors()
                            Toast.makeText(requireContext(), "Creating account...", Toast.LENGTH_SHORT).show()
                        }
                        is RegisterUiState.Success -> {
                            clearInlineErrors()
                            // Persist token from register response so user enters authenticated area immediately.
                            sessionManager.saveEncryptedAuthToken(state.token, null)
                            Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                            RouteManager.toHomeFromRegister(this@RegisterFragment)
                        }
                        is RegisterUiState.Error -> {
                            applyInlineErrors(state.fieldErrors)
                            // Show toast only when there is a general error outside field-specific validation.
                            if (state.fieldErrors.isEmpty()) {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear stale input errors before new submission/results.
    private fun clearInlineErrors() {
        binding.etRegisterEmailAddress.error = null
        binding.etRegisterPassword.error = null
        binding.etRegisterConfirmPassword.error = null
    }

    // Map backend/local field keys to specific input fields.
    private fun applyInlineErrors(fieldErrors: Map<String, String>) {
        fieldErrors["emailAddress"]?.let { binding.etRegisterEmailAddress.error = it }
        fieldErrors["password"]?.let { binding.etRegisterPassword.error = it }
        fieldErrors["confirmPassword"]?.let { binding.etRegisterConfirmPassword.error = it }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
