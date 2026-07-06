package sg.edu.nus.iss.client.dashboard.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ActivityDurationViewModel : ViewModel() {

    companion object {
        private const val DEFAULT_DURATION_MINUTES = 30L
    }

    private val initialStart: LocalTime = LocalTime.now().withSecond(0).withNano(0)

    private val _startTime = MutableStateFlow(initialStart)
    val startTime: StateFlow<LocalTime> = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow(initialStart.plusMinutes(DEFAULT_DURATION_MINUTES))
    val endTime: StateFlow<LocalTime> = _endTime.asStateFlow()

    val durationMinutes: StateFlow<Int> = combine(_startTime, _endTime) { start, end ->
        ChronoUnit.MINUTES.between(start, end).toInt().coerceAtLeast(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_DURATION_MINUTES.toInt())

    fun setStartTime(time: LocalTime) {
        _startTime.value = time
        if (!_endTime.value.isAfter(time)) {
            _endTime.value = time.plusMinutes(DEFAULT_DURATION_MINUTES)
        }
    }

    fun setEndTime(time: LocalTime) {
        if (time.isAfter(_startTime.value)) {
            _endTime.value = time
        }
    }
}
