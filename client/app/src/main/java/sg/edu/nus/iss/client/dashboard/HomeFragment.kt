// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.RagApplication
import sg.edu.nus.iss.client.databinding.FragmentHomeBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.util.SessionManager
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    // Requests runtime notification permission on Android 13+.
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // granted ->
            // if (!granted && isAdded) {
            //    Toast.makeText(requireContext(), "Enable notifications to receive recommendation alerts", Toast.LENGTH_LONG).show()
            // }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize dependencies used by notification polling and badge rendering.
        sessionManager = (requireActivity().application as RagApplication).sessionManager
        ensureNotificationPermission()
        scheduleBackgroundRecommendationPolling()

        // Adjust top and bottom bars to avoid being blocked by system bars (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)

            // The bottom nav belongs to the screen edge, not the keyboard: on devices
            // where the window resizes for the IME (adjustResize pre-edge-to-edge) it
            // would otherwise float directly above the keyboard while typing in Chat,
            // so hide it whenever the IME is up for non-chat tabs.
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val isChatTab = binding.bottomNavigation.selectedItemId == R.id.nav_chat
            val shouldHideBottomNav = imeVisible && !isChatTab
            binding.bottomNavigation.visibility = if (shouldHideBottomNav) View.GONE else View.VISIBLE

            // On edge-to-edge devices (API 30+) the window does NOT resize for the IME,
            // which would leave the Chat input hidden behind the keyboard - pad the
            // content by the keyboard's overlap instead. On devices where the window
            // already resized, the reported IME inset is 0, so this pads nothing and
            // there's no double shift.
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.homeContentContainer.setPadding(
                0, 0, 0, (ime.bottom - systemBars.bottom).coerceAtLeast(0)
            )
            insets
        }

        // Route to dashboard on first load
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_main
            RouteManager.switchHomeTab(this, RouteManager.HomeTab.MAIN)
        }
        applySoftInputModeForSelectedTab()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.unreadCountFlow.collect { count ->
                    renderNotificationBadge(count)
                }
            }
        }

        binding.btnNotifications.setOnClickListener {
            // Open recommendation history screen from the notifications icon.
            RouteManager.toRecommendationHistory(this)
        }

        binding.btnProfile.setOnClickListener {
            RouteManager.toProfile(this)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    applySoftInputModeForSelectedTab(R.id.nav_main)
                    RouteManager.switchHomeTab(this, RouteManager.HomeTab.MAIN)
                    true
                }
                R.id.nav_chat -> {
                    applySoftInputModeForSelectedTab(R.id.nav_chat)
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

        // Recommendation delivery is handled by periodic background polling.
    }

    override fun onResume() {
        super.onResume()
        applySoftInputModeForSelectedTab()
    }

    private fun applySoftInputModeForSelectedTab(selectedItemId: Int = binding.bottomNavigation.selectedItemId) {
        // Keep bottom nav pinned on Chat; preserve resize behavior elsewhere.
        val mode = if (selectedItemId == R.id.nav_chat) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        requireActivity().window.setSoftInputMode(mode)
    }

    private fun scheduleBackgroundRecommendationPolling() {
        // Enforce network availability before worker runs.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialRequest = OneTimeWorkRequestBuilder<RecommendationPollWorker>()
            .setConstraints(constraints)
            .setInputData(
                Data.Builder()
                    .putBoolean(RecommendationPollWorker.KEY_FORCE_NOTIFY_ON_FIRST_FETCH, true)
                    .build()
            )
            .build()

        // Poll every 3 hours so recommendations are periodic and not tied to each new log entry.
        val periodicRequest = PeriodicWorkRequestBuilder<RecommendationPollWorker>(3, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(requireContext().applicationContext)

        // Force an immediate fetch on startup if not checked recently.
        // This prevents spamming the user when they navigate in/out of the notification inbox.
        val lastFetch = sessionManager.getLastRecommendationFetchTime()
        val fifteenMinutesMillis = TimeUnit.MINUTES.toMillis(15)
        
        if (System.currentTimeMillis() - lastFetch > fifteenMinutesMillis) {
            workManager.enqueueUniqueWork(
                RecommendationPollWorker.INIT_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                initialRequest
            )
        }

        // Keep a single unique periodic worker across app launches.
        workManager
            .enqueueUniquePeriodicWork(
                RecommendationPollWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }

    private fun ensureNotificationPermission() {
        // Runtime notification permission only exists on Android 13+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun renderNotificationBadge(unreadCount: Int) {
        // Hide badge when there are no unread recommendations.
        if (unreadCount <= 0) {
            binding.tvNotificationBadge.visibility = View.GONE
            return
        }

        // Cap visual badge value for compact display.
        binding.tvNotificationBadge.visibility = View.VISIBLE
        binding.tvNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = null
    }
}