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
import sg.edu.nus.iss.client.dashboard.badges.BadgesViewModel
import sg.edu.nus.iss.client.dashboard.badges.model.BadgeType
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.databinding.PageDashboard1Binding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.DailyWellnessSummary
import kotlin.math.roundToInt

class DashboardPage1Fragment : Fragment() {

    companion object {
        // All goal-reached checkmarks are the same green, regardless of card.
        private val CHECKMARK_COLOR = Color.parseColor("#2E7D32")

        // Slight transparency so they don't read as a harsh solid dot against the
        // card's own softer palette.
        private const val CHECKMARK_ALPHA = 0.75f
    }

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
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]

        binding.cardDistance.setOnClickListener { openMetricDetail(MetricType.DISTANCE) }
        binding.cardCalBurned.setOnClickListener { openMetricDetail(MetricType.CALORIES) }
        binding.cardSleep.setOnClickListener { openMetricDetail(MetricType.SLEEP) }
        binding.cardHydration.setOnClickListener { openMetricDetail(MetricType.HYDRATION) }
        binding.cardWeight.setOnClickListener { openMetricDetail(MetricType.WEIGHT) }
        binding.cardBadges.setOnClickListener { RouteManager.toBadges(this) }

        listOf(
            binding.checkDistance, binding.checkCalBurned, binding.checkSleep, binding.checkHydration
        ).forEach { checkView ->
            checkView.setColorFilter(CHECKMARK_COLOR, PorterDuff.Mode.SRC_IN)
            checkView.alpha = CHECKMARK_ALPHA
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                badgesViewModel.badgeItems.collect { items ->
                    val collectedCount = items.count { it.collected }
                    binding.tvBadges.text = "$collectedCount of ${BadgeType.entries.size}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(dashboardViewModel.todaySummary, userGoalsViewModel.goals) { summary, goals ->
                    summary to goals
                }.collect { (summary, goals) ->
                    bindCards(summary, goals)
                }
            }
        }
    }

    private fun bindCards(summary: DailyWellnessSummary?, goals: Map<ActivityGoalType, Double>) {
        val distanceGoal = goals[ActivityGoalType.DISTANCE] ?: ActivityGoalType.DISTANCE.defaultValue
        val caloriesGoal = goals[ActivityGoalType.CALORIES] ?: ActivityGoalType.CALORIES.defaultValue
        val sleepGoal = goals[ActivityGoalType.SLEEP] ?: ActivityGoalType.SLEEP.defaultValue
        val hydrationGoal = goals[ActivityGoalType.HYDRATION] ?: ActivityGoalType.HYDRATION.defaultValue

        val distanceKm = summary?.totalDistanceKm ?: 0.0
        val caloriesBurned = summary?.totalCaloriesBurned ?: 0
        val sleepMinutes = summary?.sleepMinutes ?: 0
        val waterMl = summary?.totalWaterMl ?: 0
        val weightKg = summary?.weightKg

        // Distance and Sleep are scaled up (x100 / x10) since ProgressBar.max/progress
        // are Int-only and these metrics need decimal precision (centikm, deci-hours).
        binding.tvDistance.text = "%.2fkm".format(distanceKm)
        binding.progressDistance.max = (distanceGoal * 100).roundToInt().coerceAtLeast(1)
        binding.progressDistance.progress = (distanceKm * 100).roundToInt()
        binding.checkDistance.visibility = if (distanceKm >= distanceGoal) View.VISIBLE else View.GONE

        binding.tvCalBurned.text = "${caloriesBurned}Cal"
        binding.progressCalBurned.max = caloriesGoal.roundToInt().coerceAtLeast(1)
        binding.progressCalBurned.progress = caloriesBurned
        binding.checkCalBurned.visibility = if (caloriesBurned >= caloriesGoal) View.VISIBLE else View.GONE

        binding.tvSleep.text = formatSleepDuration(sleepMinutes)
        binding.progressSleep.max = (sleepGoal * 10).roundToInt().coerceAtLeast(1)
        binding.progressSleep.progress = (sleepMinutes / 60.0 * 10).roundToInt()
        binding.checkSleep.visibility = if (sleepMinutes / 60.0 >= sleepGoal) View.VISIBLE else View.GONE

        binding.tvHydration.text = "${waterMl}ml"
        binding.progressHydration.max = hydrationGoal.roundToInt().coerceAtLeast(1)
        binding.progressHydration.progress = waterMl
        binding.checkHydration.visibility = if (waterMl >= hydrationGoal) View.VISIBLE else View.GONE

        binding.tvWeight.text = if (weightKg != null) formatWeight(weightKg) else "--kg"
    }

    private fun formatSleepDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
    }

    private fun formatWeight(value: Double): String {
        val whole = if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
        return "${whole}kg"
    }

    private fun openMetricDetail(metricType: MetricType) {
        RouteManager.toMetricDetail(this, metricType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
