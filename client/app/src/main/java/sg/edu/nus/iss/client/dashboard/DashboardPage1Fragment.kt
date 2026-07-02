package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.databinding.PageDashboard1Binding
import sg.edu.nus.iss.client.util.RouteManager

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

        binding.cardDistance.setOnClickListener { openMetricDetail(MetricType.DISTANCE) }
        binding.cardSteps.setOnClickListener { openMetricDetail(MetricType.STEPS) }
        binding.cardCalBurned.setOnClickListener { openMetricDetail(MetricType.CALORIES) }
        binding.cardHydration.setOnClickListener { openMetricDetail(MetricType.HYDRATION) }
    }

    private fun openMetricDetail(metricType: MetricType) {
        RouteManager.toMetricDetail(this, metricType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
