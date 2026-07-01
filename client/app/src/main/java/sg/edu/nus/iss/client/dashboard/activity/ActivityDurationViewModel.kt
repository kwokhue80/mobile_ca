package sg.edu.nus.iss.client.dashboard.activity

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityDurationViewModel : ViewModel() {

    companion object {
        private const val DEFAULT_DURATION_MINUTES = 30
        private const val STEP_MINUTES = 5
        private const val MIN_DURATION_MINUTES = 0
        private const val MAX_DURATION_MINUTES = 300
    }

    private val _durationMinutes = MutableStateFlow(DEFAULT_DURATION_MINUTES)
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    fun increment() {
        _durationMinutes.value = (_durationMinutes.value + STEP_MINUTES).coerceAtMost(MAX_DURATION_MINUTES)
    }

    fun decrement() {
        _durationMinutes.value = (_durationMinutes.value - STEP_MINUTES).coerceAtLeast(MIN_DURATION_MINUTES)
    }
}
