package sg.edu.nus.iss.client.dashboard.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import sg.edu.nus.iss.client.network.RetrofitClient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil

data class MoodNode(val label: String, val value: Double)

data class MentalHealthUiState(
    val timeRange: TimeRange = TimeRange.DAY,
    val periodLabel: String = "",
    val points: List<MetricBar> = emptyList(),
    val dayMoodValue: Double? = null,
    val moodNodes: List<MoodNode> = emptyList(),
    val summaryText: String = "",
    val canGoNext: Boolean = false,
    val selectedBarIndex: Int? = null
)

/** Mood is tracked on a fixed 1 (very bad) .. 10 (excellent) scale; there is no
 *  user-configurable goal for it, unlike the other metrics. Day view shows a single
 *  mood icon/word (no chart); Week/Month show a trend chart plus 3 mood-icon
 *  checkpoints (early/mid/late) summarizing the trend at a glance. All real backend
 *  data: raw mood entries from GET /api/wellness/mood-logs, day-averaged here. */
class MentalHealthDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application)

    private val today: LocalDate = LocalDate.now()
    private var referenceDate: LocalDate = today
    private var timeRange: TimeRange = TimeRange.DAY

    private val _uiState = MutableStateFlow(MentalHealthUiState())
    val uiState: StateFlow<MentalHealthUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun selectTimeRange(range: TimeRange) {
        timeRange = range
        referenceDate = today
        refresh()
    }

    fun jumpToDate(date: LocalDate) {
        referenceDate = if (date.isAfter(today)) today else date
        refresh()
    }

    fun goToPreviousPeriod() {
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.minusDays(1)
            TimeRange.WEEK -> referenceDate.minusWeeks(1)
            else -> referenceDate.minusMonths(1)
        }
        refresh()
    }

    fun goToNextPeriod() {
        if (!_uiState.value.canGoNext) return
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.plusDays(1)
            TimeRange.WEEK -> referenceDate.plusWeeks(1)
            else -> referenceDate.plusMonths(1)
        }
        refresh()
    }

    fun selectBar(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBarIndex = index)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedBarIndex = null)
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            ensureMoodLoaded()
            val newState = when (timeRange) {
                TimeRange.DAY -> buildDayState()
                TimeRange.WEEK -> buildWeekState()
                else -> buildMonthState()
            }
            _uiState.value = newState
        }
    }

    // Average mood (1-10) per day, computed once per screen from the raw mood-logs
    // feed; multiple entries on the same day are averaged.
    private var dailyMood: Map<LocalDate, Double> = emptyMap()
    private var moodLoaded = false

    private suspend fun ensureMoodLoaded() {
        if (moodLoaded) return
        try {
            dailyMood = apiService.getMoodLogs(HISTORY_DAYS).body().orEmpty()
                .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                .mapValues { (_, logs) -> logs.map { it.moodRating.toDouble() }.average() }
            moodLoaded = true
        } catch (e: Exception) {
            // Keep whatever was last loaded (e.g. offline); the charts render as no-data.
        }
    }

    private fun moodOn(date: LocalDate): Double? = dailyMood[date]

    private fun buildDayState(): MentalHealthUiState {
        val value = moodOn(referenceDate)
        val periodLabel = if (referenceDate == today) "Today" else formatDayLabel(referenceDate)
        val summaryText = if (value != null) {
            "Today's mood has been ${moodCategory(value)}, averaging ${formatMood(value)}/10."
        } else {
            "No mood data has been logged for this day yet."
        }
        return MentalHealthUiState(
            timeRange = TimeRange.DAY,
            periodLabel = periodLabel,
            dayMoodValue = value,
            summaryText = summaryText,
            canGoNext = referenceDate.isBefore(today)
        )
    }

    private fun buildWeekState(): MentalHealthUiState {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        // Always emit all 7 days (Mon-Sun) so the chart's x-axis spans the full week;
        // days with no logged mood get a 0.0 filler, which MetricLineChartConfigurator
        // skips when drawing the line, and which buildMoodNodes/buildWeekSummary below
        // exclude from their averages.
        val points = (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val mood = if (date.isAfter(today)) 0.0 else moodOn(date) ?: 0.0
            MetricBar(
                axisLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                value = mood,
                rangeLabel = formatDayLabel(date),
                meetsGoal = false
            )
        }
        return MentalHealthUiState(
            timeRange = TimeRange.WEEK,
            periodLabel = "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}",
            points = points,
            moodNodes = buildMoodNodes(points),
            summaryText = buildWeekSummary(points),
            canGoNext = weekEnd.isBefore(today)
        )
    }

    private fun buildMonthState(): MentalHealthUiState {
        val monthEnd = referenceDate
        val monthStart = monthEnd.minusMonths(1)
        val totalDays = ChronoUnit.DAYS.between(monthStart, monthEnd).toInt() + 1
        // Same "always emit every day" approach as buildWeekState, so the chart's
        // x-axis spans the full month regardless of gaps in logged data.
        val points = (0 until totalDays).map { i ->
            val date = monthStart.plusDays(i.toLong())
            val mood = if (date.isAfter(today)) 0.0 else moodOn(date) ?: 0.0
            MetricBar(
                axisLabel = date.dayOfMonth.toString(),
                value = mood,
                rangeLabel = formatFullDayLabel(date),
                meetsGoal = false
            )
        }
        return MentalHealthUiState(
            timeRange = TimeRange.MONTH,
            periodLabel = "${formatShortDate(monthStart)} – ${formatShortDate(monthEnd)}",
            points = points,
            moodNodes = buildMoodNodes(points),
            summaryText = buildMonthSummary(points),
            canGoNext = monthEnd.isBefore(today)
        )
    }

    /** Splits the period into (up to) 3 roughly-equal chunks and averages each,
     *  giving an early/mid/late mood-icon snapshot of how the trend moved. Days with
     *  no logged data (value 0) are excluded from each chunk's average; a chunk with
     *  no real data at all is dropped rather than shown as a false 0. */
    private fun buildMoodNodes(points: List<MetricBar>): List<MoodNode> {
        if (points.isEmpty()) return emptyList()
        val chunkSize = ceil(points.size / 3.0).toInt().coerceAtLeast(1)
        return points.chunked(chunkSize).take(3).mapNotNull { chunk ->
            val realValues = chunk.map { it.value }.filter { it > 0.0 }
            if (realValues.isEmpty()) return@mapNotNull null
            val label = if (chunk.size == 1) chunk.first().axisLabel else "${chunk.first().axisLabel}–${chunk.last().axisLabel}"
            MoodNode(label = label, value = round1(realValues.average()))
        }
    }

    private fun round1(value: Double): Double = Math.round(value * 10) / 10.0

    private fun trendPhrase(first: Double, last: Double, periodNoun: String): String = when {
        last - first >= 1.0 -> "improved as $periodNoun went on"
        first - last >= 1.0 -> "dipped as $periodNoun went on"
        else -> "stayed fairly steady throughout $periodNoun"
    }

    private fun formatMood(value: Double): String = "%.1f".format(value)

    private fun buildWeekSummary(points: List<MetricBar>): String {
        val realPoints = points.filter { it.value > 0.0 }
        if (realPoints.isEmpty()) return "No mood data has been logged for this week yet."
        val average = realPoints.map { it.value }.average()
        val best = realPoints.maxBy { it.value }
        val worst = realPoints.minBy { it.value }
        val trend = trendPhrase(realPoints.first().value, realPoints.last().value, "the week")
        return "This week your mood has been ${moodCategory(average)}, averaging ${formatMood(average)}/10. " +
            "It $trend, with ${best.axisLabel} your best day (${formatMood(best.value)}/10) and ${worst.axisLabel} your toughest (${formatMood(worst.value)}/10)."
    }

    private fun buildMonthSummary(points: List<MetricBar>): String {
        val realPoints = points.filter { it.value > 0.0 }
        if (realPoints.isEmpty()) return "No mood data has been logged for this month yet."
        val average = realPoints.map { it.value }.average()
        val best = realPoints.maxBy { it.value }
        val worst = realPoints.minBy { it.value }
        val trend = trendPhrase(realPoints.first().value, realPoints.last().value, "the month")
        return "This month your mood averaged ${formatMood(average)}/10 across ${realPoints.size} days tracked, and has been ${moodCategory(average)} overall. " +
            "It $trend, with day ${best.axisLabel} your best (${formatMood(best.value)}/10) and day ${worst.axisLabel} your lowest (${formatMood(worst.value)}/10)."
    }

    private fun formatDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    private fun formatFullDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    private fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    companion object {
        // How far back the raw mood-log fetch reaches; month back-navigation headroom.
        private const val HISTORY_DAYS = 400

        /** 1 = Very Bad, 10 = Excellent; the buckets in between are ours to define. */
        fun moodCategory(value: Double): String = when {
            value < 2.0 -> "Very Bad"
            value < 4.0 -> "Bad"
            value < 6.0 -> "Okay"
            value < 8.0 -> "Good"
            else -> "Excellent"
        }

        fun moodEmoji(value: Double): String = when {
            value < 2.0 -> "😢"
            value < 4.0 -> "🙁"
            value < 6.0 -> "😐"
            value < 8.0 -> "🙂"
            else -> "😄"
        }
    }
}
