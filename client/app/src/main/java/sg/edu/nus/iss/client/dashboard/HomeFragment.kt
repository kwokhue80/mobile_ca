package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentHomeBinding
import sg.edu.nus.iss.client.navigation.RouteManager

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
