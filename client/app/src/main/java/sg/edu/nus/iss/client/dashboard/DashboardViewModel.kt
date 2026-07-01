package sg.edu.nus.iss.client.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import java.time.LocalDate

class DashboardViewModel : ViewModel() {

    private val _activityRecords = MutableStateFlow(
        listOf(
            ActivityRecord(id = "1", type = "Walk", timestamp = LocalDate.now().atTime(9, 39), durationMinutes = 30),
            ActivityRecord(id = "2", type = "Run", timestamp = LocalDate.now().minusDays(1).atTime(7, 15), durationMinutes = 25),
            ActivityRecord(id = "3", type = "Swim", timestamp = LocalDate.now().minusDays(1).atTime(18, 0), durationMinutes = 40)
        )
    )
    val activityRecords: StateFlow<List<ActivityRecord>> = _activityRecords.asStateFlow()

    fun removeRecord(id: String) {
        _activityRecords.value = _activityRecords.value.filterNot { it.id == id }
    }

    fun addRecord(record: ActivityRecord) {
        _activityRecords.value = listOf(record) + _activityRecords.value
    }
}
