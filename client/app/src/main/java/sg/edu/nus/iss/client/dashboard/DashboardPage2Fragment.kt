package sg.edu.nus.iss.client.dashboard

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.detail.ExerciseDaysViewModel
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.databinding.PageDashboard2Binding
import sg.edu.nus.iss.client.navigation.RouteManager

class DashboardPage2Fragment : Fragment() {

    companion object {
        // Matches DashboardPage1Fragment's checkmarks - same green, slight transparency
        // so it doesn't read as a harsh solid dot against the card's softer palette.
        private val CHECKMARK_COLOR = Color.parseColor("#2E7D32")
        private const val CHECKMARK_ALPHA = 0.75f
    }

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
        binding.cardFoodIntake.setOnClickListener { RouteManager.toMetricDetail(this, MetricType.FOOD_INTAKE) }

        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.checkExerciseDays.setColorFilter(CHECKMARK_COLOR, PorterDuff.Mode.SRC_IN)
        binding.checkExerciseDays.alpha = CHECKMARK_ALPHA

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(dashboardViewModel.activityRecords, userGoalsViewModel.goals) { records, goals ->
                    records to goals
                }.collect { (records, goals) ->
                    val goalDays =
                        (goals[ActivityGoalType.EXERCISE_DAYS] ?: ActivityGoalType.EXERCISE_DAYS.defaultValue).toInt()
                    val exercisedDays = ExerciseDaysViewModel.daysExercisedThisWeek(records)
                    binding.progressExerciseDays.max = goalDays.coerceAtLeast(1)
                    binding.progressExerciseDays.progress = exercisedDays
                    binding.tvExerciseDays.text = "$exercisedDays of $goalDays"
                    binding.checkExerciseDays.visibility = if (exercisedDays >= goalDays) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.todaySummary.collect { summary ->
                    binding.tvMentalHealth.text = summary?.moodScore?.let { "$it/10" } ?: "--"

                    val calories = summary?.totalCaloriesIntake ?: 0
                    binding.tvFoodIntake.text = "$calories kcal"
                    binding.progressFoodIntake.progress =
                        calories.coerceIn(0, binding.progressFoodIntake.max)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ViewModelProvider(requireActivity())[DashboardViewModel::class.java].refreshToday()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
