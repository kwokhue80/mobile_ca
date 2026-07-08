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
import sg.edu.nus.iss.client.dashboard.detail.model.MetricDetailUiState
import sg.edu.nus.iss.client.dashboard.detail.model.MetricSummaryRow
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import sg.edu.nus.iss.client.network.HourlyWellnessResponse
import sg.edu.nus.iss.client.network.RetrofitClient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MetricDetailViewModel(application: Application, private val metricType: MetricType) :
    AndroidViewModel(application) {

    private enum class MonthAnchorMode { ROLLING, CALENDAR }

    companion object {
        private const val SIX_MONTH_COUNT = 6L
        private const val WEEKLY_ROWS_COUNT = 5
        private const val MONTHLY_ROWS_COUNT = 7

        // How far back the raw-log fetch reaches. Covers the deepest view (6-Month)
        // plus back-navigation headroom; days outside this window chart as 0.
        private const val HISTORY_DAYS = 400
    }

    private val apiService = RetrofitClient.getApiService(application)

    private val today: LocalDate = LocalDate.now()
    private var referenceDate: LocalDate = today
    private var timeRange: TimeRange = TimeRange.DAY
    private var monthAnchorMode: MonthAnchorMode = MonthAnchorMode.ROLLING
    private var currentGoal: Double = metricType.defaultGoal

    private val _uiState = MutableStateFlow(MetricDetailUiState())
    val uiState: StateFlow<MetricDetailUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

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
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            ensureDailyValuesLoaded()
            val newState = when (timeRange) {
                TimeRange.DAY -> buildDayState()
                TimeRange.WEEK -> buildWeekState()
                TimeRange.MONTH -> buildMonthState()
                TimeRange.SIX_MONTH -> buildSixMonthState()
            }
            _uiState.value = newState
        }
    }

    // Distance/Calories/Hydration select from the hourly buckets for the Day view;
    // the other metrics' Day view is a single daily value from dailyValues.
    private fun selectHourlyValue(entry: HourlyWellnessResponse?): Double = when (metricType) {
        MetricType.DISTANCE -> entry?.distanceKm ?: 0.0
        MetricType.CALORIES -> entry?.caloriesBurnedKcal?.toDouble() ?: 0.0
        MetricType.HYDRATION -> entry?.waterMl?.toDouble() ?: 0.0
        MetricType.SLEEP, MetricType.WEIGHT, MetricType.FOOD_INTAKE -> 0.0
    }

    // One value per day for this metric, aggregated client-side from the metric's own
    // raw-log endpoint (loaded once per screen): sums for distance/calories/hydration/
    // food, hours for sleep (attributed to the wake-up date), day-average for weight.
    private var dailyValues: Map<LocalDate, Double> = emptyMap()

    // Day-averaged sleep quality (1-5), only populated for the Sleep metric.
    private var dailySleepQuality: Map<LocalDate, Double> = emptyMap()

    private var dailyValuesLoaded = false

    private suspend fun ensureDailyValuesLoaded() {
        if (dailyValuesLoaded) return
        try {
            dailyValues = when (metricType) {
                MetricType.DISTANCE -> apiService.getExerciseLogs(HISTORY_DAYS).body().orEmpty()
                    .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                    .mapValues { (_, logs) -> logs.sumOf { it.distanceKm ?: 0.0 } }

                MetricType.CALORIES -> apiService.getExerciseLogs(HISTORY_DAYS).body().orEmpty()
                    .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                    .mapValues { (_, logs) -> logs.sumOf { it.caloriesBurnedKcal.toDouble() } }

                MetricType.HYDRATION -> apiService.getHydrationLogs(HISTORY_DAYS).body().orEmpty()
                    .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                    .mapValues { (_, logs) -> logs.sumOf { it.volumeMl.toDouble() } }

                MetricType.SLEEP -> {
                    val logs = apiService.getSleepLogs(HISTORY_DAYS).body().orEmpty()
                    val byWakeDate = logs.groupBy { LocalDateTime.parse(it.endTime).toLocalDate() }
                    dailySleepQuality = byWakeDate.mapNotNull { (date, dayLogs) ->
                        val scores = dayLogs.mapNotNull { it.sleepQualityScore }
                        if (scores.isEmpty()) null else date to scores.average()
                    }.toMap()
                    byWakeDate.mapValues { (_, dayLogs) -> dayLogs.sumOf { it.durationMinutes } / 60.0 }
                }

                MetricType.WEIGHT -> apiService.getWeightLogs(HISTORY_DAYS).body().orEmpty()
                    .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                    .mapValues { (_, logs) -> logs.map { it.weightKg }.average() }

                MetricType.FOOD_INTAKE -> apiService.getFoodLogs(HISTORY_DAYS).body().orEmpty()
                    .groupBy { LocalDateTime.parse(it.loggedAt).toLocalDate() }
                    .mapValues { (_, logs) -> logs.sumOf { it.caloriesKcal.toDouble() } }
            }
            dailyValuesLoaded = true
        } catch (e: Exception) {
            // Keep empty maps (e.g. offline); charts render as no-data instead of crashing.
        }
    }

    private fun valueOn(date: LocalDate): Double = dailyValues[date] ?: 0.0

    private suspend fun fetchHourlySummary(date: LocalDate): List<HourlyWellnessResponse> {
        return try {
            val response = apiService.getHourlySummary(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            response.body().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun averageSleepQuality(start: LocalDate, end: LocalDate): Double? {
        if (metricType != MetricType.SLEEP) return null
        val scores = dailySleepQuality.filterKeys { !it.isBefore(start) && !it.isAfter(end) }.values
        return if (scores.isEmpty()) null else scores.average()
    }

    private suspend fun buildDayState(): MetricDetailUiState {
        val goal = currentGoal
        val periodLabel = if (referenceDate == today) "Today" else formatDayLabel(referenceDate)
        val canNext = referenceDate.isBefore(today)

        if (metricType == MetricType.SLEEP || metricType == MetricType.WEIGHT) {
            val total = valueOn(referenceDate)
            return MetricDetailUiState(
                timeRange = TimeRange.DAY,
                periodLabel = periodLabel,
                totalValue = total,
                goalValue = goal,
                subtitle = buildGoalSubtitle(total, goal),
                bars = emptyList(),
                selectedBarIndex = null,
                summaryRows = emptyList(),
                canGoNext = canNext,
                sleepQualityScore = dailySleepQuality[referenceDate]
            )
        }

        if (metricType == MetricType.FOOD_INTAKE) {
            val total = valueOn(referenceDate)
            val bars = listOf(
                MetricBar(
                    axisLabel = "Today",
                    value = total,
                    rangeLabel = formatDayLabel(referenceDate),
                    meetsGoal = total >= goal
                )
            )
            return MetricDetailUiState(
                timeRange = TimeRange.DAY,
                periodLabel = periodLabel,
                totalValue = total,
                goalValue = goal,
                chartGoalValue = goal,
                subtitle = buildGoalSubtitle(total, goal),
                bars = bars,
                selectedBarIndex = null,
                summaryRows = emptyList(),
                canGoNext = canNext
            )
        }

        val hourly = fetchHourlySummary(referenceDate)
        val bars = (0 until 24).map { hour ->
            val value = selectHourlyValue(hourly.getOrNull(hour))
            MetricBar(
                axisLabel = formatHourShort(hour),
                value = value,
                rangeLabel = "${formatHourLong(hour)}–${formatHourLong((hour + 1) % 24)}",
                meetsGoal = false
            )
        }
        val total = bars.sumOf { it.value }
        return MetricDetailUiState(
            timeRange = TimeRange.DAY,
            periodLabel = periodLabel,
            totalValue = total,
            goalValue = goal,
            subtitle = buildGoalSubtitle(total, goal),
            bars = bars,
            selectedBarIndex = null,
            summaryRows = emptyList(),
            canGoNext = canNext
        )
    }

    private fun buildWeekState(): MetricDetailUiState {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val dailyGoal = currentGoal
        val bars = (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val value = if (date.isAfter(today)) 0.0 else valueOn(date)
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
            canGoNext = weekEnd.isBefore(today),
            sleepQualityScore = averageSleepQuality(weekStart, weekEnd)
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

        val dailyGoal = currentGoal
        val totalDaysInWindow = ChronoUnit.DAYS.between(monthStart, monthEnd).toInt() + 1
        val bars = (0 until totalDaysInWindow).map { i ->
            val date = monthStart.plusDays(i.toLong())
            val value = if (date.isAfter(today)) 0.0 else valueOn(date)
            MetricBar(
                axisLabel = date.dayOfMonth.toString(),
                value = value,
                rangeLabel = formatFullDayLabel(date),
                meetsGoal = value >= dailyGoal
            )
        }
        val total = bars.sumOf { it.value }
        // Days with no logged data (value 0) are excluded from the average rather than
        // dragging it down as if the user scored a real zero that day.
        val nonZeroDays = bars.count { it.value > 0.0 }
        val average = if (nonZeroDays > 0) total / nonZeroDays else 0.0
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
            canGoPrevious = true,
            sleepQualityScore = averageSleepQuality(monthStart, monthEnd)
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
            val daysInMonth = cursor.lengthOfMonth()
            var monthTotal = 0.0
            var monthDaysCounted = 0
            for (dayOffset in 0 until daysInMonth) {
                val date = cursor.plusDays(dayOffset.toLong())
                if (date.isAfter(today)) continue
                val value = valueOn(date)
                // Days with no logged data (value 0) are excluded from the average.
                if (value <= 0.0) continue
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
            canGoNext = periodEnd.isBefore(today),
            sleepQualityScore = averageSleepQuality(periodStart, today)
        )
    }

    private fun buildWeeklyAverageRows(anchorDate: LocalDate): List<MetricSummaryRow> {
        val currentWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until WEEKLY_ROWS_COUNT).map { weeksAgo ->
            val weekStart = currentWeekStart.minusWeeks(weeksAgo.toLong())
            val weekEnd = weekStart.plusDays(6)
            // Days with no logged data (value 0) are excluded from the average.
            val nonZeroValues = (0 until 7).mapNotNull { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                if (date.isAfter(today)) null else valueOn(date).takeIf { it > 0.0 }
            }
            val label = if (weeksAgo == 0) "This week" else "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}"
            val average = if (nonZeroValues.isNotEmpty()) nonZeroValues.average() else 0.0
            MetricSummaryRow(label = label, value = average, isCurrentPeriod = weeksAgo == 0)
        }
    }

    private fun buildMonthlyAverageRows(anchorDate: LocalDate): List<MetricSummaryRow> {
        return (0 until MONTHLY_ROWS_COUNT).map { monthsAgo ->
            val monthDate = anchorDate.minusMonths(monthsAgo.toLong())
            val daysInMonth = monthDate.lengthOfMonth()
            // Days with no logged data (value 0) are excluded from the average.
            val nonZeroValues = (0 until daysInMonth).mapNotNull { dayOffset ->
                val date = monthDate.withDayOfMonth(1).plusDays(dayOffset.toLong())
                if (date.isAfter(today)) null else valueOn(date).takeIf { it > 0.0 }
            }
            val label = if (monthsAgo == 0) {
                "This month"
            } else {
                monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            }
            val average = if (nonZeroValues.isNotEmpty()) nonZeroValues.average() else 0.0
            MetricSummaryRow(label = label, value = average, isCurrentPeriod = monthsAgo == 0)
        }
    }

    private fun buildAverageSubtitle(average: Double, goal: Double, total: Double): String {
        val totalLabel = "${metricType.formatValue(total)} ${metricType.unit}"
        val totalAction = if (metricType == MetricType.FOOD_INTAKE) "consumed" else "covered"
        return if (average >= goal) {
            "You hit your goal. You $totalAction a total of $totalLabel."
        } else {
            "You didn't hit your goal. You $totalAction a total of $totalLabel."
        }
    }

    private fun buildDaysMetSubtitle(daysMet: Int, total: Double): String {
        val totalLabel = "${metricType.formatValue(total)} ${metricType.unit}"
        val totalAction = if (metricType == MetricType.FOOD_INTAKE) "consumed" else "took"
        return "You hit your goal on $daysMet days. You $totalAction a total of $totalLabel."
    }

    private fun buildGoalSubtitle(total: Double, goal: Double): String = if (total >= goal) {
        "You hit your goal!"
    } else {
        "You're ${metricType.formatValue(goal - total)} ${metricType.unit} away from hitting your goal."
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
