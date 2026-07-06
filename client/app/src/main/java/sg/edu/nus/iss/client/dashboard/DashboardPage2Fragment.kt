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
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
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
        binding.cardExerciseDays.setOnClickListener { RouteManager.toExerciseDaysDetail(this) }
        binding.cardMentalHealth.setOnClickListener { RouteManager.toMentalHealthDetail(this) }

        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]
        val currentExerciseDays = binding.progressExerciseDays.progress

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userGoalsViewModel.goals.collect { goals ->
                    val goalDays =
                        (goals[ActivityGoalType.EXERCISE_DAYS] ?: ActivityGoalType.EXERCISE_DAYS.defaultValue).toInt()
                    binding.progressExerciseDays.max = goalDays
                    binding.tvExerciseDays.text = "$currentExerciseDays of $goalDays"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
