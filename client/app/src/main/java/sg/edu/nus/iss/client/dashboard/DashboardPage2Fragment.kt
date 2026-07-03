package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.databinding.PageDashboard2Binding
import sg.edu.nus.iss.client.navigation.RouteManager

class DashboardPage2Fragment : Fragment() {
    private var _binding: PageDashboard2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PageDashboard2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardWeight.setOnClickListener { RouteManager.toMetricDetail(this, MetricType.WEIGHT) }
        binding.cardExerciseDays.setOnClickListener { RouteManager.toExerciseDaysDetail(this) }
        binding.cardMentalHealth.setOnClickListener { RouteManager.toMentalHealthDetail(this) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
