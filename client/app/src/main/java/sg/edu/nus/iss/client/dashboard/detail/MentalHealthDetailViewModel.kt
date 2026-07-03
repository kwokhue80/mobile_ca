package sg.edu.nus.iss.client.dashboard.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

data class MoodNode(val label: String, val value: Double)

data class MentalHealthUiState(
    val timeRange: TimeRange = TimeRange.DAY,
    val periodLabel: String = "",
    val points: List<MetricBar> = emptyList(),
    val dayMoodValue: Double = 5.0,
    val moodNodes: List<MoodNode> = emptyList(),
    val summaryText: String = "",
    val canGoNext: Boolean = false
)

/** Mood is tracked on a fixed 1 (very bad) .. 10 (excellent) scale; there is no
 *  user-configurable goal for it, unlike the other metrics. Day view shows a single
 *  mood icon/word (no chart); Week/Month show a trend chart plus 3 mood-icon
 *  checkpoints (early/mid/late) summarizing the trend at a glance. */
class MentalHealthDetailViewModel : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private var referenceDate: LocalDate = today
    private var timeRange: TimeRange = TimeRange.DAY

    private val _uiState = MutableStateFlow(MentalHealthUiState())
    val uiState: StateFlow<MentalHealthUiState> = _uiState.asStateFlow()

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

    private fun refresh() {
        _uiState.value = when (timeRange) {
            TimeRange.DAY -> buildDayState()
            TimeRange.WEEK -> buildWeekState()
            else -> buildMonthState()
        }
    }

    private fun buildDayState(): MentalHealthUiState {
        val random = Random(referenceDate.toEpochDay())
        val value = round1((5.0 + (random.nextDouble() * 4.0 - 2.0)).coerceIn(1.0, 10.0))
        val periodLabel = if (referenceDate == today) "Today" else formatDayLabel(referenceDate)
        return MentalHealthUiState(
            timeRange = TimeRange.DAY,
            periodLabel = periodLabel,
            dayMoodValue = value,
            summaryText = "Today's mood has been ${moodCategory(value)}, averaging ${formatMood(value)}/10.",
            canGoNext = referenceDate.isBefore(today)
        )
    }

    private fun buildWeekState(): MentalHealthUiState {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val random = Random(weekStart.toEpochDay())
        val values = moodSequence(random, 7)
        val points = (0 until 7).mapNotNull { i ->
            val date = weekStart.plusDays(i.toLong())
            if (date.isAfter(today)) return@mapNotNull null
            MetricBar(
                axisLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                value = values[i],
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
        val random = Random(monthStart.toEpochDay())
        val totalDays = ChronoUnit.DAYS.between(monthStart, monthEnd).toInt() + 1
        val values = moodSequence(random, totalDays)
        val points = (0 until totalDays).map { i ->
            val date = monthStart.plusDays(i.toLong())
            MetricBar(
                axisLabel = date.dayOfMonth.toString(),
                value = values[i],
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
     *  giving an early/mid/late mood-icon snapshot of how the trend moved. */
    private fun buildMoodNodes(points: List<MetricBar>): List<MoodNode> {
        if (points.isEmpty()) return emptyList()
        val chunkSize = ceil(points.size / 3.0).toInt().coerceAtLeast(1)
        return points.chunked(chunkSize).take(3).map { chunk ->
            val average = chunk.map { it.value }.average()
            val label = if (chunk.size == 1) chunk.first().axisLabel else "${chunk.first().axisLabel}–${chunk.last().axisLabel}"
            MoodNode(label = label, value = round1(average))
        }
    }

    private fun moodSequence(random: Random, count: Int): List<Double> {
        var current = (5.0 + (random.nextDouble() * 4.0 - 2.0)).coerceIn(1.0, 10.0)
        val values = mutableListOf(round1(current))
        repeat(count - 1) {
            current = (current + (random.nextDouble() * 3.0 - 1.5)).coerceIn(1.0, 10.0)
            values.add(round1(current))
        }
        return values
    }

    private fun round1(value: Double): Double = Math.round(value * 10) / 10.0

    private fun trendPhrase(first: Double, last: Double, periodNoun: String): String = when {
        last - first >= 1.0 -> "improved as $periodNoun went on"
        first - last >= 1.0 -> "dipped as $periodNoun went on"
        else -> "stayed fairly steady throughout $periodNoun"
    }

    private fun formatMood(value: Double): String = "%.1f".format(value)

    private fun buildWeekSummary(points: List<MetricBar>): String {
        if (points.isEmpty()) return "No mood data has been logged for this week yet."
        val average = points.map { it.value }.average()
        val best = points.maxBy { it.value }
        val worst = points.minBy { it.value }
        val trend = trendPhrase(points.first().value, points.last().value, "the week")
        return "This week your mood has been ${moodCategory(average)}, averaging ${formatMood(average)}/10. " +
            "It $trend, with ${best.axisLabel} your best day (${formatMood(best.value)}/10) and ${worst.axisLabel} your toughest (${formatMood(worst.value)}/10)."
    }

    private fun buildMonthSummary(points: List<MetricBar>): String {
        if (points.isEmpty()) return "No mood data has been logged for this month yet."
        val average = points.map { it.value }.average()
        val best = points.maxBy { it.value }
        val worst = points.minBy { it.value }
        val trend = trendPhrase(points.first().value, points.last().value, "the month")
        return "This month your mood averaged ${formatMood(average)}/10 across ${points.size} days tracked, and has been ${moodCategory(average)} overall. " +
            "It $trend, with day ${best.axisLabel} your best (${formatMood(best.value)}/10) and day ${worst.axisLabel} your lowest (${formatMood(worst.value)}/10)."
    }

    private fun formatDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    private fun formatFullDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    private fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    companion object {
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
