// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ExerciseViewMode { WEEK, MONTH }

data class ExerciseDayBar(
    val label: String,
    val hasExercise: Boolean,
    val isToday: Boolean
)

data class CalendarDay(
    val date: LocalDate?,
    val hasExercise: Boolean,
    val isToday: Boolean
)

data class ExerciseWeekSummaryRow(
    val label: String,
    val daysExercised: Int,
    val goalDays: Int,
    val weekStart: LocalDate,
    val isCurrentWeek: Boolean
)

data class ExerciseDaysUiState(
    val mode: ExerciseViewMode = ExerciseViewMode.WEEK,
    val weekPeriodLabel: String = "",
    val weekSummaryBig: String = "",
    val weekSummarySuffix: String = "",
    val weekSummarySubtitle: String = "",
    val weekBars: List<ExerciseDayBar> = emptyList(),
    val weekActivities: List<ActivityRecord> = emptyList(),
    val canGoNextWeek: Boolean = false,
    val monthPeriodLabel: String = "",
    val monthSummaryBig: String = "",
    val monthSummarySuffix: String = "",
    val monthSummarySubtitle: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val weekSummaryRows: List<ExerciseWeekSummaryRow> = emptyList(),
    val canGoNextMonth: Boolean = false
)

class ExerciseDaysViewModel : ViewModel() {

    companion object {
        // Sunday-first weeks, matching the reference design (S M T W T F S).
        private fun weekStartOfDate(date: LocalDate): LocalDate {
            val daysSinceSunday = date.dayOfWeek.value % 7
            return date.minusDays(daysSinceSunday.toLong())
        }

        /** Distinct days this week (Sunday-start, containing [referenceDate]) that have
         *  at least one exercise record. Shared by the Exercise Days summary card
         *  (DashboardPage2Fragment) and this ViewModel's own week view. */
        fun daysExercisedThisWeek(records: List<ActivityRecord>, referenceDate: LocalDate = LocalDate.now()): Int {
            val exerciseDates = records.map { it.timestamp.toLocalDate() }.toSet()
            val weekStart = weekStartOfDate(referenceDate)
            return (0 until 7).count { i -> exerciseDates.contains(weekStart.plusDays(i.toLong())) }
        }
    }

    // Sunday-first weeks, matching the reference design (S M T W T F S).
    private val today: LocalDate = LocalDate.now()
    private var mode = ExerciseViewMode.WEEK
    private var weekReferenceDate = today
    private var monthReferenceDate = today
    private var records: List<ActivityRecord> = emptyList()
    private var weeklyGoalDays: Int = 4

    private val _uiState = MutableStateFlow(ExerciseDaysUiState())
    val uiState: StateFlow<ExerciseDaysUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setRecords(newRecords: List<ActivityRecord>) {
        records = newRecords
        refresh()
    }

    fun setWeeklyGoal(days: Int) {
        if (weeklyGoalDays == days) return
        weeklyGoalDays = days
        refresh()
    }

    fun selectMode(newMode: ExerciseViewMode) {
        mode = newMode
        refresh()
    }

    fun goToPreviousWeek() {
        weekReferenceDate = weekReferenceDate.minusWeeks(1)
        refresh()
    }

    fun goToNextWeek() {
        if (!canGoNextWeek()) return
        weekReferenceDate = weekReferenceDate.plusWeeks(1)
        refresh()
    }

    fun goToPreviousMonth() {
        monthReferenceDate = monthReferenceDate.minusMonths(1)
        refresh()
    }

    fun goToNextMonth() {
        if (!canGoNextMonth()) return
        monthReferenceDate = monthReferenceDate.plusMonths(1)
        refresh()
    }

    fun jumpToWeek(weekStart: LocalDate) {
        weekReferenceDate = weekStart
        mode = ExerciseViewMode.WEEK
        refresh()
    }

    fun selectWeekDate(date: LocalDate) {
        weekReferenceDate = date
        refresh()
    }

    fun selectMonthDate(date: LocalDate) {
        monthReferenceDate = date
        refresh()
    }

    private fun weekStartOf(date: LocalDate): LocalDate = weekStartOfDate(date)

    private fun currentWeekStart(): LocalDate = weekStartOf(weekReferenceDate)

    private fun todayWeekStart(): LocalDate = weekStartOf(today)

    private fun canGoNextWeek(): Boolean = currentWeekStart().isBefore(todayWeekStart())

    private fun canGoNextMonth(): Boolean =
        YearMonth.from(monthReferenceDate).isBefore(YearMonth.from(today))

    private fun exerciseDatesSet(): Set<LocalDate> =
        records.map { it.timestamp.toLocalDate() }.toSet()

    private fun refresh() {
        val exerciseDates = exerciseDatesSet()
        val weekStart = currentWeekStart()
        val daysExercisedThisWeek = (0 until 7).count { i -> exerciseDates.contains(weekStart.plusDays(i.toLong())) }
        val isRealCurrentWeek = weekStart == todayWeekStart()

        val yearMonth = YearMonth.from(monthReferenceDate)
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val daysExercisedThisMonth = exerciseDates.count { !it.isBefore(monthStart) && !it.isAfter(monthEnd) }
        val isRealCurrentMonth = yearMonth == YearMonth.from(today)

        _uiState.value = ExerciseDaysUiState(
            mode = mode,
            weekPeriodLabel = if (isRealCurrentWeek) "This week" else
                "${formatShortDate(weekStart)} – ${formatShortDate(weekStart.plusDays(6))}",
            weekSummaryBig = "$daysExercisedThisWeek of $weeklyGoalDays",
            weekSummarySuffix = "exercise days",
            weekSummarySubtitle = "You exercised a total of $daysExercisedThisWeek days of the week.",
            weekBars = buildWeekBars(exerciseDates, weekStart),
            weekActivities = buildWeekActivities(weekStart),
            canGoNextWeek = canGoNextWeek(),
            monthPeriodLabel = if (isRealCurrentMonth) "This month" else
                yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            monthSummaryBig = "$daysExercisedThisMonth of ${yearMonth.lengthOfMonth()}",
            monthSummarySuffix = "exercise days",
            monthSummarySubtitle = "You exercised a total of $daysExercisedThisMonth days of the month.",
            calendarDays = buildCalendarDays(exerciseDates),
            weekSummaryRows = buildWeekSummaryRows(exerciseDates),
            canGoNextMonth = canGoNextMonth()
        )
    }

    private fun buildWeekBars(exerciseDates: Set<LocalDate>, weekStart: LocalDate): List<ExerciseDayBar> =
        (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            ExerciseDayBar(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                hasExercise = exerciseDates.contains(date),
                isToday = date == today
            )
        }

    private fun buildWeekActivities(weekStart: LocalDate): List<ActivityRecord> {
        val weekEnd = weekStart.plusDays(6)
        return records.filter {
            val date = it.timestamp.toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        }
    }

    private fun buildCalendarDays(exerciseDates: Set<LocalDate>): List<CalendarDay> {
        val yearMonth = YearMonth.from(monthReferenceDate)
        val firstOfMonth = yearMonth.atDay(1)
        // Sunday-first leading blanks (Sunday=7 in DayOfWeek.value -> 0 blanks).
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
        val days = mutableListOf<CalendarDay>()
        repeat(leadingBlanks) { days.add(CalendarDay(null, false, false)) }
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            days.add(CalendarDay(date, exerciseDates.contains(date), date == today))
        }
        return days
    }

    private fun buildWeekSummaryRows(exerciseDates: Set<LocalDate>): List<ExerciseWeekSummaryRow> {
        val currentWeekStart = todayWeekStart()
        return (0 until 4).map { weeksAgo ->
            val weekStart = currentWeekStart.minusWeeks(weeksAgo.toLong())
            val daysExercised = (0 until 7).count { i -> exerciseDates.contains(weekStart.plusDays(i.toLong())) }
            val label = if (weeksAgo == 0) {
                "This week"
            } else {
                "${formatShortDate(weekStart)} – ${formatShortDate(weekStart.plusDays(6))}"
            }
            ExerciseWeekSummaryRow(
                label = label,
                daysExercised = daysExercised,
                goalDays = weeklyGoalDays,
                weekStart = weekStart,
                isCurrentWeek = weeksAgo == 0
            )
        }
    }

    private fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
