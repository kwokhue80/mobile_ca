package sg.edu.nus.iss.client.dashboard.goals

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

/** Activity-scoped store of the user's current goal per [ActivityGoalType], shared between
 *  the Set Goals flow and the metric detail / exercise days pages that display goal lines. */
class UserGoalsViewModel : ViewModel() {

    private val _goals = MutableStateFlow(
        ActivityGoalType.entries.associateWith { it.defaultValue }
    )
    val goals: StateFlow<Map<ActivityGoalType, Double>> = _goals.asStateFlow()

    fun getGoal(type: ActivityGoalType): Double = _goals.value[type] ?: type.defaultValue

    fun setGoal(type: ActivityGoalType, value: Double) {
        _goals.value = _goals.value.toMutableMap().apply { put(type, value) }
    }
}
