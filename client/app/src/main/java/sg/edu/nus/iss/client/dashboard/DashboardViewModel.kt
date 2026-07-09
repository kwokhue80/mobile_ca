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
 *  "Activity Tracked" list, the History screen, and the Exercise Days detail screen.
 *  Backed by `/api/wellness/daily-summary` (today's summary, server-computed "today")
 *  and `/api/wellness/exercise-logs` (full exercise history, not just the last 7 days,
 *  so week/month navigation and day-counting work correctly beyond the current week),
 *  refreshed after every Add-sheet / Add-Activity save via [refreshToday]. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        // Limit to 30 days
        private const val ACTIVITY_HISTORY_DAYS = 180
    }

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
                val logsResponse = apiService.getExerciseLogs(ACTIVITY_HISTORY_DAYS)
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
        // Optimistically remove from the in-memory list immediately, then delete on
        // the backend (which also reverses the log's contribution to that day's
        // distance/calories/exercise-minutes totals) and re-sync everything - if the
        // delete fails, refreshToday() below will restore the record on next load.
        _activityRecords.value = _activityRecords.value.filterNot { it.id == id }
        viewModelScope.launch {
            try {
                apiService.deleteExerciseLog(id.toLong())
            } catch (e: Exception) {
                // Ignore; refreshToday() below will re-sync from the backend either way.
            }
            refreshToday()
        }
    }

    private fun toActivityRecord(log: ExerciseLogResponse): ActivityRecord {
        val exerciseType = ExerciseType.fromBackendExerciseType(log.exerciseType)
        val displayType = exerciseType?.displayName
            ?: log.exerciseType.lowercase().replaceFirstChar { it.uppercase() }
            
        val raw = log.startTime ?: log.loggedAt
        val sgtZone = java.time.ZoneId.of("Asia/Singapore")
        
        val timestamp = runCatching {
            // If the server sends an offset (like Z or +08:00), parse and convert to SGT
            java.time.OffsetDateTime.parse(raw).atZoneSameInstant(sgtZone).toLocalDateTime()
        }.getOrElse {
            runCatching {
                // If no offset, assume SGT as the server is in Singapore
                LocalDateTime.parse(raw.replace(" ", "T")).atZone(sgtZone).toLocalDateTime()
            }.getOrElse {
                runCatching {
                    LocalDateTime.parse(raw, recordDateFormatter).atZone(sgtZone).toLocalDateTime()
                }.getOrDefault(LocalDateTime.now())
            }
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
