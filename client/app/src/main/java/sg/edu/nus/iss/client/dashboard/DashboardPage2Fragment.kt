package sg.edu.nus.iss.client.dashboard

import android.graphics.Color
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
import sg.edu.nus.iss.client.dashboard.detail.ExerciseDaysViewModel
import sg.edu.nus.iss.client.dashboard.detail.MentalHealthDetailViewModel
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.databinding.PageDashboard2Binding
import sg.edu.nus.iss.client.navigation.RouteManager

class DashboardPage2Fragment : Fragment() {

    companion object {
        // Matches DashboardPage1Fragment's goal-reached style: green card border ring
        // + top-right corner wedge, same color regardless of card.
        private val GOAL_REACHED_COLOR = Color.parseColor("#22C55E")
        private val GOAL_NOT_REACHED_COLOR = Color.TRANSPARENT
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

        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(dashboardViewModel.activityRecords, userGoalsViewModel.goals) { records, goals ->
                    records to goals
                }.collect { (records, goals) ->
                    val goalDays =
                        (goals[ActivityGoalType.EXERCISE_DAYS] ?: ActivityGoalType.EXERCISE_DAYS.defaultValue).toInt()
                    val exercisedDays = ExerciseDaysViewModel.daysExercisedThisWeek(records)
                    // Goal only scales the progress bar's max; the card text always
                    // counts against 7 (days in a week), since that's what this card tracks.
                    binding.progressExerciseDays.max = goalDays.coerceAtLeast(1)
                    binding.progressExerciseDays.progress = exercisedDays
                    binding.tvExerciseDays.text = "$exercisedDays of 7"
                    val reached = exercisedDays >= goalDays
                    binding.cardExerciseDays.strokeColor = if (reached) GOAL_REACHED_COLOR else GOAL_NOT_REACHED_COLOR
                    binding.cornerWedgeExerciseDays.visibility = if (reached) View.VISIBLE else View.GONE
                    binding.checkExerciseDays.visibility = if (reached) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.todaySummary.collect { summary ->
                    binding.tvMentalHealth.text = summary?.moodScore?.let {
                        MentalHealthDetailViewModel.moodCategory(it.toDouble())
                    } ?: "--"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
