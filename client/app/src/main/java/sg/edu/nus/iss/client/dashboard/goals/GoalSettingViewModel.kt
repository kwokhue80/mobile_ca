// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.goals

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

class GoalSettingViewModel(
    private val activityGoalType: ActivityGoalType,
    initialValue: Double
) : ViewModel() {

    private val _value = MutableStateFlow(initialValue)
    val value: StateFlow<Double> = _value.asStateFlow()

    fun increment() {
        _value.value = (_value.value + activityGoalType.step).coerceAtMost(activityGoalType.maxValue)
    }

    fun decrement() {
        _value.value = (_value.value - activityGoalType.step).coerceAtLeast(activityGoalType.minValue)
    }
}
