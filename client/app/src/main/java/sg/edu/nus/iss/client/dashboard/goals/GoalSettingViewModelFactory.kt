package sg.edu.nus.iss.client.dashboard.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

class GoalSettingViewModelFactory(private val activityGoalType: ActivityGoalType) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalSettingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoalSettingViewModel(activityGoalType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
