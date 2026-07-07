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

    // A newly logged activity is assumed just-finished (end = now), not about to start
    // (start = now) - the latter would save a loggedAt/endTime that's still in the
    // future at save time, which excludes it from any "up to now" query (Activity
    // Tracked, Exercise Days, History) until real time catches up to it.
    private val initialEnd: LocalTime = LocalTime.now().withSecond(0).withNano(0)

    private val _endTime = MutableStateFlow(initialEnd)
    val endTime: StateFlow<LocalTime> = _endTime.asStateFlow()

    private val _startTime = MutableStateFlow(initialEnd.minusMinutes(DEFAULT_DURATION_MINUTES))
    val startTime: StateFlow<LocalTime> = _startTime.asStateFlow()

    val durationMinutes: StateFlow<Int> = combine(_startTime, _endTime) { start, end ->
        // LocalTime has no date, so a start/end pair straddling midnight (e.g.
        // start=23:41, end=00:11) looks numerically backwards; wrap it forward a
        // full day rather than clamping to 0.
        val raw = ChronoUnit.MINUTES.between(start, end)
        (if (raw < 0) raw + 24 * 60 else raw).toInt()
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
