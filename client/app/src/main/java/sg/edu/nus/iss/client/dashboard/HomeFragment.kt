package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import sg.edu.nus.iss.client.databinding.FragmentHomeBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.SessionManager

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var authApiService: AuthApiService
    private lateinit var logoutViewModel: LogoutViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Init session manager, api service, logout view model
        sessionManager = SessionManager(requireContext())
        authApiService = RetrofitClient.getApiService(requireContext())
        logoutViewModel = ViewModelProvider(
            this,
            LogoutViewModelFactory(authApiService)
        )[LogoutViewModel::class.java]

        // Adjust top and bottom bars to avoid being blocked by system bars (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Route to dashboard on first load
        if (savedInstanceState == null) {
            RouteManager.switchHomeTab(this, RouteManager.HomeTab.MAIN)
        }

        binding.btnNotifications.setOnClickListener {
            // TODO: Show notifications
        }

        // On select profile btn, a menu drops down
        binding.btnProfile.setOnClickListener {
            showProfileMenu(it)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    RouteManager.switchHomeTab(this, RouteManager.HomeTab.MAIN)
                    true
                }
                R.id.nav_chat -> {
                    RouteManager.switchHomeTab(this, RouteManager.HomeTab.CHAT)
                    true
                }
                R.id.nav_add -> {
                    RouteManager.showAddManually(this)
                    false
                }
                else -> false
            }
        }

        // Observe logout state while view is visible
        // On completion, clear session and return to login.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                logoutViewModel.uiState.collect { state ->
                    when (state) {
                        is LogoutUiState.Idle -> Unit
                        is LogoutUiState.Loading -> {}

                        // On logout success, clear session and return to login
                        is LogoutUiState.Success -> {
                            sessionManager.clearSession()
                            Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                            RouteManager.toLogin(this@HomeFragment)
                            logoutViewModel.resetState()
                        }

                        // On logout error, clear session and return to login
                        // REASON: Prioritising user experience + security; JWT has expiry on backend too
                        is LogoutUiState.Error -> {
                            sessionManager.clearSession()
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            RouteManager.toLogin(this@HomeFragment)
                            logoutViewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    // Functions: logout / my profile
    private fun showProfileMenu(anchor: View) {
        val popupMenu = PopupMenu(requireContext(), anchor)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                // On select logout, clear session and send logout request to server
                R.id.action_logout -> {
                    confirmLogout()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    // Alert on select logout
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
