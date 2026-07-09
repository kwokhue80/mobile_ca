// Author: HuaYuan Xie
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
import com.google.android.material.card.MaterialCardView
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
        // Goal-reached style: a green ring around the card plus a matching top-right
        // corner wedge, same color regardless of card.
        private val GOAL_REACHED_COLOR = Color.parseColor("#22C55E")
        private val GOAL_NOT_REACHED_COLOR = Color.TRANSPARENT
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
        setGoalReached(binding.cardDistance, binding.cornerWedgeDistance, binding.checkDistance, distanceKm >= distanceGoal)

        binding.tvCalBurned.text = "${caloriesBurned}Cal"
        binding.progressCalBurned.max = caloriesGoal.roundToInt().coerceAtLeast(1)
        binding.progressCalBurned.progress = caloriesBurned
        setGoalReached(binding.cardCalBurned, binding.cornerWedgeCalBurned, binding.checkCalBurned, caloriesBurned >= caloriesGoal)

        binding.tvSleep.text = formatSleepDuration(sleepMinutes)
        binding.progressSleep.max = (sleepGoal * 10).roundToInt().coerceAtLeast(1)
        binding.progressSleep.progress = (sleepMinutes / 60.0 * 10).roundToInt()
        setGoalReached(binding.cardSleep, binding.cornerWedgeSleep, binding.checkSleep, sleepMinutes / 60.0 >= sleepGoal)

        binding.tvHydration.text = "${waterMl}ml"
        binding.progressHydration.max = hydrationGoal.roundToInt().coerceAtLeast(1)
        binding.progressHydration.progress = waterMl
        setGoalReached(binding.cardHydration, binding.cornerWedgeHydration, binding.checkHydration, waterMl >= hydrationGoal)

        binding.tvWeight.text = if (weightKg != null) formatWeight(weightKg) else "--kg"
    }

    private fun setGoalReached(
        card: MaterialCardView,
        cornerWedge: View,
        check: View,
        reached: Boolean
    ) {
        card.strokeColor = if (reached) GOAL_REACHED_COLOR else GOAL_NOT_REACHED_COLOR
        cornerWedge.visibility = if (reached) View.VISIBLE else View.GONE
        check.visibility = if (reached) View.VISIBLE else View.GONE
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
