package sg.edu.nus.iss.client.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.auth.LogoutUiState
import sg.edu.nus.iss.client.auth.LogoutViewModel
import sg.edu.nus.iss.client.auth.LogoutViewModelFactory
import sg.edu.nus.iss.client.databinding.FragmentProfileBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.SessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var authApiService: AuthApiService
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var logoutViewModel: LogoutViewModel

    private val dateDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    private var isFirstResume = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        authApiService = RetrofitClient.getApiService(requireContext())
        profileViewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(authApiService)
        )[ProfileViewModel::class.java]
        logoutViewModel = ViewModelProvider(
            this,
            LogoutViewModelFactory(authApiService)
        )[LogoutViewModel::class.java]

        binding.btnBack.setOnClickListener { RouteManager.back(this) }
        binding.btnEdit.setOnClickListener { RouteManager.toEditProfile(this) }
        binding.btnLogout.setOnClickListener { confirmLogout() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.loadState.collect { state -> renderLoadState(state) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                logoutViewModel.uiState.collect { state -> renderLogoutState(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Skip the very first resume (ProfileViewModel already loads on init);
        // reload on subsequent resumes in case changes were saved on Edit Profile.
        if (isFirstResume) {
            isFirstResume = false
        } else {
            profileViewModel.loadProfile()
        }
    }

    private fun renderLoadState(state: ProfileLoadState) {
        when (state) {
            is ProfileLoadState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.contentContainer.visibility = View.GONE
            }

            is ProfileLoadState.Loaded -> {
                binding.progressLoading.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE

                binding.tvEmail.text = state.emailAddress
                binding.tvFullName.text = state.fullName.ifBlank { "Add your name" }

                val dob = state.dateOfBirth.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                binding.tvDateOfBirth.text = dob?.format(dateDisplayFormatter) ?: "—"
                binding.tvGender.text = state.gender.ifBlank { "—" }
                binding.tvHeight.text = state.heightCm.takeIf { it.isNotBlank() }?.let { "$it cm" } ?: "—"
            }

            is ProfileLoadState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderLogoutState(state: LogoutUiState) {
        when (state) {
            is LogoutUiState.Idle -> Unit
            is LogoutUiState.Loading -> Unit

            is LogoutUiState.Success -> {
                sessionManager.clearSession()
                Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                RouteManager.toLogin(this)
                logoutViewModel.resetState()
            }

            is LogoutUiState.Error -> {
                sessionManager.clearSession()
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                RouteManager.toLogin(this)
                logoutViewModel.resetState()
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logoutViewModel.logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
