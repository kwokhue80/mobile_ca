package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.badges.BadgesViewModel
import sg.edu.nus.iss.client.dashboard.badges.model.BadgeType
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.databinding.PageDashboard1Binding
import sg.edu.nus.iss.client.navigation.RouteManager

class DashboardPage1Fragment : Fragment() {
    private var _binding: PageDashboard1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PageDashboard1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val badgesViewModel = ViewModelProvider(requireActivity())[BadgesViewModel::class.java]

        binding.cardDistance.setOnClickListener { openMetricDetail(MetricType.DISTANCE) }
        binding.cardSteps.setOnClickListener { openMetricDetail(MetricType.STEPS) }
        binding.cardCalBurned.setOnClickListener { openMetricDetail(MetricType.CALORIES) }
        binding.cardSleep.setOnClickListener { openMetricDetail(MetricType.SLEEP) }
        binding.cardHydration.setOnClickListener { openMetricDetail(MetricType.HYDRATION) }
        binding.cardBadges.setOnClickListener { RouteManager.toBadges(this) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                badgesViewModel.badgeItems.collect { items ->
                    val collectedCount = items.count { it.collected }
                    binding.tvBadges.text = "$collectedCount of ${BadgeType.entries.size}"
                }
            }
        }
    }

    private fun openMetricDetail(metricType: MetricType) {
        RouteManager.toMetricDetail(this, metricType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
