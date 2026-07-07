package sg.edu.nus.iss.client.dashboard.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.network.UserGoalUpsertRequest

/** Activity-scoped store of the user's current goal per [ActivityGoalType], shared between
 *  the Set Goals flow and the metric detail / exercise days pages that display goal lines.
 *  Backed by the `/api/user/goals` endpoint; falls back to [ActivityGoalType.defaultValue]
 *  until the authenticated user's saved goals are loaded (or if loading fails). */
class UserGoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application)

    private val _goals = MutableStateFlow(
        ActivityGoalType.entries.associateWith { it.defaultValue }
    )
    val goals: StateFlow<Map<ActivityGoalType, Double>> = _goals.asStateFlow()

    init {
        loadGoals()
    }

    fun loadGoals() {
        viewModelScope.launch {
            try {
                val response = apiService.getUserGoals()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val savedGoals = body.mapNotNull { goal ->
                        ActivityGoalType.fromBackendGoalType(goal.goalType)?.let { it to goal.targetValue }
                    }
                    _goals.value = _goals.value + savedGoals
                }
            } catch (e: Exception) {
                // Keep the default values if the saved goals can't be fetched (e.g. offline).
            }
        }
    }

    fun getGoal(type: ActivityGoalType): Double = _goals.value[type] ?: type.defaultValue

    fun setGoal(type: ActivityGoalType, value: Double) {
        _goals.value = _goals.value.toMutableMap().apply { put(type, value) }
        viewModelScope.launch {
            try {
                apiService.updateUserGoal(type.backendGoalType, UserGoalUpsertRequest(value))
            } catch (e: Exception) {
                // Local value is already updated optimistically; a later loadGoals() call
                // will reconcile with the server if this request silently failed.
            }
        }
    }
}
