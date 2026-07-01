package sg.edu.nus.iss.client.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord

class DashboardViewModel : ViewModel() {

    private val _activityRecords = MutableStateFlow(
        listOf(
            ActivityRecord(id = "1", type = "Walk", date = "Today, 9:39 AM", durationMinutes = 30),
            ActivityRecord(id = "2", type = "Run", date = "Yesterday, 7:15 AM", durationMinutes = 25),
            ActivityRecord(id = "3", type = "Swim", date = "Yesterday, 6:00 PM", durationMinutes = 40)
        )
    )
    val activityRecords: StateFlow<List<ActivityRecord>> = _activityRecords.asStateFlow()

    fun removeRecord(id: String) {
        _activityRecords.value = _activityRecords.value.filterNot { it.id == id }
    }
}
