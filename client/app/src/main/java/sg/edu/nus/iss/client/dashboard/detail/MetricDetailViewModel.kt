package sg.edu.nus.iss.client.dashboard.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar
import sg.edu.nus.iss.client.dashboard.detail.model.MetricDetailUiState
import sg.edu.nus.iss.client.dashboard.detail.model.MetricSummaryRow
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.random.Random

class MetricDetailViewModel(private val metricType: MetricType) : ViewModel() {

    private enum class MonthAnchorMode { ROLLING, CALENDAR }

    companion object {
        private const val SIX_MONTH_COUNT = 6L
        private const val WEEKLY_ROWS_COUNT = 5
        private const val MONTHLY_ROWS_COUNT = 7
    }

    private val today: LocalDate = LocalDate.now()
    private var referenceDate: LocalDate = today
    private var timeRange: TimeRange = TimeRange.DAY
    private var monthAnchorMode: MonthAnchorMode = MonthAnchorMode.ROLLING
    private var currentGoal: Double = metricType.defaultGoal

    private val _uiState = MutableStateFlow(MetricDetailUiState())
    val uiState: StateFlow<MetricDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setGoal(value: Double) {
        if (currentGoal == value) return
        currentGoal = value
        refresh()
    }

    fun selectTimeRange(range: TimeRange) {
        timeRange = range
        referenceDate = today
        monthAnchorMode = MonthAnchorMode.ROLLING
        refresh()
    }

    fun selectBar(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBarIndex = index)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedBarIndex = null)
    }

    fun jumpToDate(date: LocalDate) {
        referenceDate = if (date.isAfter(today)) today else date
        if (timeRange == TimeRange.MONTH) {
            monthAnchorMode = MonthAnchorMode.CALENDAR
        }
        refresh()
    }

    fun goToPreviousPeriod() {
        if (!_uiState.value.canGoPrevious) return
        // Any backward navigation in Month mode leaves the initial rolling
        // "same day last month" window and switches to whole-calendar-month
        // browsing from then on.
        if (timeRange == TimeRange.MONTH) {
            monthAnchorMode = MonthAnchorMode.CALENDAR
        }
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.minusDays(1)
            TimeRange.WEEK -> referenceDate.minusWeeks(1)
            TimeRange.MONTH -> referenceDate.minusMonths(1)
            TimeRange.SIX_MONTH -> referenceDate.minusMonths(6)
        }
        refresh()
    }

    fun goToNextPeriod() {
        if (!_uiState.value.canGoNext) return
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.plusDays(1)
            TimeRange.WEEK -> referenceDate.plusWeeks(1)
            TimeRange.MONTH -> referenceDate.plusMonths(1)
            TimeRange.SIX_MONTH -> referenceDate.plusMonths(6)
        }
        refresh()
    }

    private fun refresh() {
        _uiState.value = when (timeRange) {
            TimeRange.DAY -> buildDayState()
            TimeRange.WEEK -> buildWeekState()
            TimeRange.MONTH -> buildMonthState()
            TimeRange.SIX_MONTH -> buildSixMonthState()
        }
    }

    private fun buildDayState(): MetricDetailUiState {
        val random = Random(referenceDate.toEpochDay())
        val bars = (0 until 24).map { hour ->
            val hasActivity = hour in 8..20 && random.nextInt(4) == 0
            val value = if (hasActivity) randomBarValue(random) else 0.0
            MetricBar(
                axisLabel = formatHourShort(hour),
                value = value,
                rangeLabel = "${formatHourLong(hour)}–${formatHourLong((hour + 1) % 24)}",
                meetsGoal = false
            )
        }
        val total = bars.sumOf { it.value }
        val goal = currentGoal
        val periodLabel = if (referenceDate == today) "Today" else formatDayLabel(referenceDate)
        return MetricDetailUiState(
            timeRange = TimeRange.DAY,
            periodLabel = periodLabel,
            totalValue = total,
            goalValue = goal,
            subtitle = buildGoalSubtitle(total, goal),
            bars = bars,
            selectedBarIndex = null,
            summaryRows = emptyList(),
            canGoNext = referenceDate.isBefore(today)
        )
    }

    private fun buildWeekState(): MetricDetailUiState {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val random = Random(weekStart.toEpochDay())
        val dailyGoal = currentGoal
        val bars = (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val value = if (date.isAfter(today)) 0.0 else randomBarValue(random, biasHigh = true)
            MetricBar(
                axisLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                value = value,
                rangeLabel = formatDayLabel(date),
                meetsGoal = value >= dailyGoal
            )
        }
        val total = bars.sumOf { it.value }
        val weeklyGoal = dailyGoal * 7
        val summaryRows = bars.mapIndexed { i, bar ->
            val date = weekStart.plusDays(i.toLong())
            MetricSummaryRow(
                label = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                value = bar.value,
                isCurrentPeriod = date == today
            )
        }
        return MetricDetailUiState(
            timeRange = TimeRange.WEEK,
            periodLabel = "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}",
            totalValue = total,
            goalValue = weeklyGoal,
            chartGoalValue = dailyGoal,
            subtitle = buildGoalSubtitle(total, weeklyGoal),
            bars = bars,
            selectedBarIndex = null,
            summaryRows = summaryRows,
            canGoNext = weekEnd.isBefore(today)
        )
    }

    private fun buildMonthState(): MetricDetailUiState {
        val monthStart: LocalDate
        val monthEnd: LocalDate
        val periodLabel: String
        val canNext: Boolean

        if (monthAnchorMode == MonthAnchorMode.CALENDAR) {
            monthStart = referenceDate.withDayOfMonth(1)
            monthEnd = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
            periodLabel = referenceDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            canNext = YearMonth.from(referenceDate).isBefore(YearMonth.from(today))
        } else {
            // Initial rolling window: "same day last month" to "same day this month".
            monthEnd = referenceDate
            monthStart = monthEnd.minusMonths(1)
            periodLabel = "${formatShortDate(monthStart)} – ${formatShortDate(monthEnd)}"
            canNext = monthEnd.isBefore(today)
        }

        val random = Random(monthStart.toEpochDay())
        val dailyGoal = currentGoal
        val totalDaysInWindow = ChronoUnit.DAYS.between(monthStart, monthEnd).toInt() + 1
        val bars = (0 until totalDaysInWindow).map { i ->
            val date = monthStart.plusDays(i.toLong())
            val value = if (date.isAfter(today)) 0.0 else randomBarValue(random, biasHigh = true)
            MetricBar(
                axisLabel = date.dayOfMonth.toString(),
                value = value,
                rangeLabel = formatFullDayLabel(date),
                meetsGoal = value >= dailyGoal
            )
        }
        val total = bars.sumOf { it.value }
        val average = total / bars.size
        return MetricDetailUiState(
            timeRange = TimeRange.MONTH,
            periodLabel = periodLabel,
            totalValue = average,
            goalValue = dailyGoal,
            chartGoalValue = dailyGoal,
            subtitle = buildAverageSubtitle(average, dailyGoal, total),
            bars = bars,
            selectedBarIndex = null,
            summaryRows = buildWeeklyAverageRows(monthEnd),
            canGoNext = canNext,
            canGoPrevious = true
        )
    }

    private fun buildSixMonthState(): MetricDetailUiState {
        val periodEnd = referenceDate
        val periodStart = periodEnd.minusMonths(SIX_MONTH_COUNT - 1).withDayOfMonth(1)
        val dailyGoal = currentGoal

        var totalAllDays = 0.0
        var totalDaysCounted = 0
        var daysGoalMet = 0
        val bars = mutableListOf<MetricBar>()
        var cursor = periodStart
        while (!cursor.isAfter(periodEnd)) {
            val monthRandom = Random(cursor.toEpochDay())
            val daysInMonth = cursor.lengthOfMonth()
            var monthTotal = 0.0
            var monthDaysCounted = 0
            for (dayOffset in 0 until daysInMonth) {
                val date = cursor.plusDays(dayOffset.toLong())
                if (date.isAfter(today)) continue
                val value = randomBarValue(monthRandom, biasHigh = true)
                monthTotal += value
                monthDaysCounted++
                if (value >= dailyGoal) daysGoalMet++
            }
            totalAllDays += monthTotal
            totalDaysCounted += monthDaysCounted
            val monthAverage = if (monthDaysCounted > 0) monthTotal / monthDaysCounted else 0.0
            bars.add(
                MetricBar(
                    axisLabel = formatMonthAbbrev(cursor),
                    value = monthAverage,
                    rangeLabel = cursor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    meetsGoal = monthAverage >= dailyGoal
                )
            )
            cursor = cursor.plusMonths(1)
        }
        val overallAverage = if (totalDaysCounted > 0) totalAllDays / totalDaysCounted else 0.0

        return MetricDetailUiState(
            timeRange = TimeRange.SIX_MONTH,
            periodLabel = "${formatMonthAbbrev(periodStart)} – ${formatMonthAbbrev(periodEnd)}",
            totalValue = overallAverage,
            goalValue = dailyGoal,
            chartGoalValue = dailyGoal,
            subtitle = buildDaysMetSubtitle(daysGoalMet, totalAllDays),
            bars = bars,
            selectedBarIndex = null,
            summaryRows = buildMonthlyAverageRows(periodEnd),
            canGoNext = periodEnd.isBefore(today)
        )
    }

    private fun buildWeeklyAverageRows(anchorDate: LocalDate): List<MetricSummaryRow> {
        val currentWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until WEEKLY_ROWS_COUNT).map { weeksAgo ->
            val weekStart = currentWeekStart.minusWeeks(weeksAgo.toLong())
            val weekEnd = weekStart.plusDays(6)
            val weekRandom = Random(weekStart.toEpochDay())
            val weekTotal = (0 until 7).sumOf { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                if (date.isAfter(today)) 0.0 else randomBarValue(weekRandom, biasHigh = true)
            }
            val label = if (weeksAgo == 0) "This week" else "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}"
            MetricSummaryRow(label = label, value = weekTotal / 7, isCurrentPeriod = weeksAgo == 0)
        }
    }

    private fun buildMonthlyAverageRows(anchorDate: LocalDate): List<MetricSummaryRow> {
        return (0 until MONTHLY_ROWS_COUNT).map { monthsAgo ->
            val monthDate = anchorDate.minusMonths(monthsAgo.toLong())
            val daysInMonth = monthDate.lengthOfMonth()
            val monthRandom = Random(monthDate.withDayOfMonth(1).toEpochDay())
            val monthTotal = (0 until daysInMonth).sumOf { randomBarValue(monthRandom, biasHigh = true) }
            val label = if (monthsAgo == 0) {
                "This month"
            } else {
                monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            }
            MetricSummaryRow(label = label, value = monthTotal / daysInMonth, isCurrentPeriod = monthsAgo == 0)
        }
    }

    private fun buildAverageSubtitle(average: Double, goal: Double, total: Double): String {
        val totalLabel = "${metricType.formatValue(total)} ${metricType.unit}"
        return if (average >= goal) {
            "You hit your goal. You covered a total of $totalLabel."
        } else {
            "You didn't hit your goal. You covered a total of $totalLabel."
        }
    }

    private fun buildDaysMetSubtitle(daysMet: Int, total: Double): String {
        val totalLabel = "${metricType.formatValue(total)} ${metricType.unit}"
        return "You hit your goal on $daysMet days. You took a total of $totalLabel."
    }

    private fun buildGoalSubtitle(total: Double, goal: Double): String = if (total >= goal) {
        "You hit your goal!"
    } else {
        "You're ${metricType.formatValue(goal - total)} ${metricType.unit} away from hitting your goal."
    }

    private fun randomBarValue(random: Random, biasHigh: Boolean = false): Double {
        // Weight and Sleep are bounded to realistic human ranges regardless of the
        // user's goal (unlike Distance/Calories/Hydration, whose mock trend is still
        // goal-scaled) - a goal-scaled formula could otherwise mock a 0kg weight or
        // an unrealistic sleep duration.
        val raw = when (metricType) {
            MetricType.WEIGHT -> random.nextDouble(40.0, 100.0)
            MetricType.SLEEP -> random.nextDouble(5.0, 12.0)
            else -> random.nextDouble() * (currentGoal * if (biasHigh) 1.3 else 0.4)
        }
        return if (metricType.decimalPlaces == 0) {
            Math.round(raw).toDouble()
        } else {
            Math.round(raw * 100) / 100.0
        }
    }

    private fun formatHourShort(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour $period"
    }

    private fun formatHourLong(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:00 %s", displayHour, period)
    }

    private fun formatDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    private fun formatFullDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    private fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    private fun formatMonthAbbrev(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
}
