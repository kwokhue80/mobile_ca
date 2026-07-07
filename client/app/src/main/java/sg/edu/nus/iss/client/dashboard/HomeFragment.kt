package sg.edu.nus.iss.client.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import sg.edu.nus.iss.client.R
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && isAdded) {
                Toast.makeText(requireContext(), "Enable notifications to receive recommendation alerts", Toast.LENGTH_LONG).show()
            }
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
        sessionManager = SessionManager(requireContext())
        ensureNotificationPermission()
        scheduleBackgroundRecommendationPolling()

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

        renderNotificationBadge(sessionManager.getUnreadRecommendationCount())

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

        // Recommendation delivery is handled by periodic background polling.
    }

    override fun onResume() {
        super.onResume()
        // Refresh badge when returning from background or after notification tap.
        renderNotificationBadge(sessionManager.getUnreadRecommendationCount())
    }

    private fun scheduleBackgroundRecommendationPolling() {
        // Enforce network availability before worker runs.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Poll every 3 hours so recommendations are periodic and not tied to each new log entry.
        val periodicRequest = PeriodicWorkRequestBuilder<RecommendationPollWorker>(3, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Keep a single unique periodic worker across app launches.
        WorkManager.getInstance(requireContext().applicationContext)
            .enqueueUniquePeriodicWork(
                RecommendationPollWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
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
        _binding = null
    }
}