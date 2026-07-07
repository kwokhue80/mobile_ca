package sg.edu.nus.iss.client.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.network.DailyWellnessSummary
import sg.edu.nus.iss.client.network.ExerciseLogResponse
import sg.edu.nus.iss.client.network.RetrofitClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Shared source of "today's" wellness data for the dashboard cards, the Home
 *  "Activity Tracked" list, and the History screen. Backed by
 *  `/api/wellness/daily-summary` (today's summary, server-computed "today") and
 *  `/api/wellness/weekly-exercise` (last 7 days of exercise sessions), refreshed
 *  after every Add-sheet / Add-Activity save via [refreshToday]. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application)
    private val recordDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val _todaySummary = MutableStateFlow<DailyWellnessSummary?>(null)
    val todaySummary: StateFlow<DailyWellnessSummary?> = _todaySummary.asStateFlow()

    private val _activityRecords = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val activityRecords: StateFlow<List<ActivityRecord>> = _activityRecords.asStateFlow()

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            try {
                val response = apiService.getDailyWellnessSummary()
                if (response.isSuccessful) {
                    _todaySummary.value = response.body()
                }
            } catch (e: Exception) {
                // Keep the previous summary if the refresh fails (e.g. offline).
            }

            try {
                val logsResponse = apiService.getWeeklyExercise()
                val logs = logsResponse.body()
                if (logsResponse.isSuccessful && logs != null) {
                    _activityRecords.value = logs.map(::toActivityRecord)
                }
            } catch (e: Exception) {
                // Keep the previous list if the refresh fails (e.g. offline).
            }
        }
    }

    fun removeRecord(id: String) {
        // No backend delete endpoint exists yet for exercise logs; this only
        // affects the in-memory list until the next refreshToday() call.
        _activityRecords.value = _activityRecords.value.filterNot { it.id == id }
    }

    private fun toActivityRecord(log: ExerciseLogResponse): ActivityRecord {
        val exerciseType = ExerciseType.fromBackendExerciseType(log.exerciseType)
        val displayType = exerciseType?.displayName
            ?: log.exerciseType.lowercase().replaceFirstChar { it.uppercase() }
        val timestamp = runCatching {
            LocalDateTime.parse(log.startTime ?: log.loggedAt)
        }.getOrElse {
            runCatching { LocalDateTime.parse(log.loggedAt, recordDateFormatter) }.getOrDefault(LocalDateTime.now())
        }

        return ActivityRecord(
            id = log.id.toString(),
            type = displayType,
            timestamp = timestamp,
            durationMinutes = log.durationMinutes,
            distanceKm = log.distanceKm ?: 0.0,
            calories = log.caloriesBurnedKcal
        )
    }
}
