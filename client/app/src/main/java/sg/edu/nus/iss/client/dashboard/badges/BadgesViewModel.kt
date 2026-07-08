package sg.edu.nus.iss.client.dashboard.badges

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.badges.model.BadgeType
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.network.RetrofitClient
import java.time.LocalDateTime

data class BadgeUiItem(
    val type: BadgeType,
    val achieved: Boolean,
    val collected: Boolean
)

/** Activity-scoped so collected state survives navigating from the Badges grid
 *  back to the Dashboard. Achievement is computed from real backend data via
 *  [refresh] (called by BadgesFragment whenever goals are (re)loaded); the
 *  Dashboard's badge count only increases once the user explicitly collects a
 *  badge - being achievable alone isn't enough.
 *
 *  Step Champion has no backing data (steps aren't tracked anywhere in this
 *  app's schema) and can never be achieved. */
class BadgesViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val DISTANCE_MASTER_KM = 1000.0
        const val CALORIE_CRUSHER_KCAL = 50_000
        const val HYDRATION_HERO_ML = 100_000
        const val SLEEP_CHAMPION_HOURS = 8.0
        const val POSITIVE_MIND_SCORE = 7.0
        const val CONSISTENCY_KING_DAYS = 100

        // How far back the All-Rounder check scans (any single day that hit every
        // daily goal at once); comfortably covers the app's seeded history.
        const val ALL_ROUNDER_LOOKBACK_DAYS = 400
    }

    private val apiService = RetrofitClient.getApiService(application)

    private val achieved = MutableStateFlow<Set<BadgeType>>(emptySet())
    private val collected = MutableStateFlow<Set<BadgeType>>(emptySet())

    val badgeItems: StateFlow<List<BadgeUiItem>> = combine(achieved, collected) { achievedSet, collectedSet ->
        BadgeType.entries.map { type ->
            BadgeUiItem(type = type, achieved = achievedSet.contains(type), collected = collectedSet.contains(type))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var refreshJob: Job? = null

    fun refresh(goals: Map<ActivityGoalType, Double>) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            achieved.value = computeAchieved(goals)
        }
    }

    fun collect(type: BadgeType) {
        if (!achieved.value.contains(type)) return
        collected.value = collected.value + type
    }

    private suspend fun computeAchieved(goals: Map<ActivityGoalType, Double>): Set<BadgeType> {
        val result = mutableSetOf<BadgeType>()

        val progress = try {
            apiService.getBadgeProgress().body()
        } catch (e: Exception) {
            null
        }

        if (progress != null) {
            if (progress.totalRunDistanceKm >= DISTANCE_MASTER_KM) result += BadgeType.DISTANCE_MASTER
            if (progress.totalCaloriesBurned >= CALORIE_CRUSHER_KCAL) result += BadgeType.CALORIE_CRUSHER
            if (progress.totalHydrationMl >= HYDRATION_HERO_ML) result += BadgeType.HYDRATION_HERO
            if ((progress.avgSleepHoursLast30Days ?: 0.0) >= SLEEP_CHAMPION_HOURS) result += BadgeType.SLEEP_CHAMPION
            if ((progress.avgMoodLast30Days ?: 0.0) >= POSITIVE_MIND_SCORE) result += BadgeType.POSITIVE_MIND
            if (progress.distinctExerciseDays >= CONSISTENCY_KING_DAYS) result += BadgeType.CONSISTENCY_KING

            val weightGoal = goals[ActivityGoalType.WEIGHT] ?: ActivityGoalType.WEIGHT.defaultValue
            if (progress.todayWeightKg != null && progress.todayWeightKg < weightGoal) {
                result += BadgeType.WEIGHT_GOAL
            }
        }

        if (checkAllRounder(goals)) {
            result += BadgeType.ALL_ROUNDER
        }

        return result
    }

    // A day counts if it independently hit every one of the same 4 daily goals the
    // dashboard cards themselves check (Distance/Calories/Hydration/Sleep) - matches
    // DashboardPage1Fragment's goal-reached comparisons exactly. Aggregated per day
    // from the raw-log endpoints, same as the metric detail charts.
    private suspend fun checkAllRounder(goals: Map<ActivityGoalType, Double>): Boolean {
        val distanceGoal = goals[ActivityGoalType.DISTANCE] ?: ActivityGoalType.DISTANCE.defaultValue
        val caloriesGoal = goals[ActivityGoalType.CALORIES] ?: ActivityGoalType.CALORIES.defaultValue
        val sleepGoal = goals[ActivityGoalType.SLEEP] ?: ActivityGoalType.SLEEP.defaultValue
        val hydrationGoal = goals[ActivityGoalType.HYDRATION] ?: ActivityGoalType.HYDRATION.defaultValue

        return try {
            val exerciseByDay = apiService.getExerciseLogs(ALL_ROUNDER_LOOKBACK_DAYS).body().orEmpty()
                .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
            val hydrationByDay = apiService.getHydrationLogs(ALL_ROUNDER_LOOKBACK_DAYS).body().orEmpty()
                .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
            val sleepByDay = apiService.getSleepLogs(ALL_ROUNDER_LOOKBACK_DAYS).body().orEmpty()
                .groupBy { LocalDateTime.parse(it.endTime).toLocalDate() }

            // Only days with exercise can qualify, so iterate those.
            exerciseByDay.any { (date, dayLogs) ->
                dayLogs.sumOf { it.distanceKm ?: 0.0 } >= distanceGoal &&
                    dayLogs.sumOf { it.caloriesBurnedKcal } >= caloriesGoal &&
                    hydrationByDay[date].orEmpty().sumOf { it.volumeMl } >= hydrationGoal &&
                    sleepByDay[date].orEmpty().sumOf { it.durationMinutes } / 60.0 >= sleepGoal
            }
        } catch (e: Exception) {
            false
        }
    }
}
