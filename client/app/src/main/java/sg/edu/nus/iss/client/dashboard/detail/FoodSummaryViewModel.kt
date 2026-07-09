// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** One logged meal, ready for display (meal type already prettified). */
data class FoodEntry(
    val mealType: String,
    val foodName: String,
    val caloriesKcal: Int,
    val loggedAt: LocalDateTime
)

data class FoodSummaryUiState(
    val timeRange: TimeRange = TimeRange.DAY,
    val periodLabel: String = "Today",
    // Day view: that day's meals + total. No chart/axis by design.
    val dayMeals: List<FoodEntry> = emptyList(),
    val dayTotalKcal: Int = 0,
    // Week/Month views: one bar per day (kcal). Week always has 7 Mon..Sun bars.
    val bars: List<MetricBar> = emptyList(),
    val selectedBarIndex: Int? = null,
    // Meals of the tapped bar's day, shown under the chart.
    val selectedDateLabel: String? = null,
    val selectedMeals: List<FoodEntry> = emptyList(),
    val canGoNext: Boolean = false
)

/** Backs the Food Summary detail screen. D/W/M only: Day is a plain meal list,
 *  Week/Month are per-day kcal bar charts where tapping a bar reveals that day's
 *  meals. All data comes from GET /api/wellness/food-logs (last 180 days). */
class FoodSummaryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val HISTORY_DAYS = 180

        fun displayMealType(raw: String): String =
            raw.lowercase().split("_").joinToString(" ") { part ->
                part.replaceFirstChar { it.uppercase() }
            }
    }

    private val apiService = RetrofitClient.getApiService(application)

    private val today: LocalDate = LocalDate.now()
    private var referenceDate: LocalDate = today
    private var timeRange: TimeRange = TimeRange.DAY

    private var logsByDate: Map<LocalDate, List<FoodEntry>> = emptyMap()

    private val _uiState = MutableStateFlow(FoodSummaryUiState())
    val uiState: StateFlow<FoodSummaryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val response = apiService.getFoodLogs(HISTORY_DAYS)
                val logs = response.body()
                if (response.isSuccessful && logs != null) {
                    logsByDate = logs
                        .mapNotNull { log ->
                            runCatching {
                                FoodEntry(
                                    mealType = displayMealType(log.mealType),
                                    foodName = log.foodName,
                                    caloriesKcal = log.caloriesKcal,
                                    loggedAt = LocalDateTime.parse(log.loggedAt)
                                )
                            }.getOrNull()
                        }
                        .groupBy { it.loggedAt.toLocalDate() }
                        .mapValues { (_, entries) -> entries.sortedBy { it.loggedAt } }
                }
            } catch (e: Exception) {
                // Keep whatever was last loaded (e.g. offline); the UI still rebuilds.
            }
            rebuild()
        }
    }

    fun selectTimeRange(range: TimeRange) {
        timeRange = range
        referenceDate = today
        rebuild()
    }

    /** Calendar picker: jump the current view (D/W/M) to the period containing
     *  the picked date. Future dates clamp to today. */
    fun jumpToDate(date: LocalDate) {
        referenceDate = if (date.isAfter(today)) today else date
        rebuild()
    }

    fun selectBar(index: Int) {
        val bars = _uiState.value.bars
        if (index !in bars.indices) return
        val date = dateForBarIndex(index) ?: return
        _uiState.value = _uiState.value.copy(
            selectedBarIndex = index,
            selectedDateLabel = formatFullDayLabel(date),
            selectedMeals = logsByDate[date].orEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedBarIndex = null,
            selectedDateLabel = null,
            selectedMeals = emptyList()
        )
    }

    fun goToPreviousPeriod() {
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.minusDays(1)
            TimeRange.WEEK -> referenceDate.minusWeeks(1)
            else -> referenceDate.minusMonths(1)
        }
        rebuild()
    }

    fun goToNextPeriod() {
        if (!_uiState.value.canGoNext) return
        referenceDate = when (timeRange) {
            TimeRange.DAY -> referenceDate.plusDays(1)
            TimeRange.WEEK -> referenceDate.plusWeeks(1)
            else -> referenceDate.plusMonths(1)
        }
        rebuild()
    }

    private fun rebuild() {
        _uiState.value = when (timeRange) {
            TimeRange.WEEK -> buildWeekState()
            TimeRange.MONTH -> buildMonthState()
            else -> buildDayState()
        }
    }

    private fun buildDayState(): FoodSummaryUiState {
        val meals = logsByDate[referenceDate].orEmpty()
        return FoodSummaryUiState(
            timeRange = TimeRange.DAY,
            periodLabel = if (referenceDate == today) "Today" else formatDayLabel(referenceDate),
            dayMeals = meals,
            dayTotalKcal = meals.sumOf { it.caloriesKcal },
            canGoNext = referenceDate.isBefore(today)
        )
    }

    private fun buildWeekState(): FoodSummaryUiState {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val bars = (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val kcal = if (date.isAfter(today)) 0 else logsByDate[date].orEmpty().sumOf { it.caloriesKcal }
            MetricBar(
                axisLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                value = kcal.toDouble(),
                rangeLabel = formatDayLabel(date),
                meetsGoal = false
            )
        }
        return FoodSummaryUiState(
            timeRange = TimeRange.WEEK,
            periodLabel = "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}",
            bars = bars,
            canGoNext = weekEnd.isBefore(today)
        )
    }

    private fun buildMonthState(): FoodSummaryUiState {
        val month = YearMonth.from(referenceDate)
        val bars = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            val kcal = if (date.isAfter(today)) 0 else logsByDate[date].orEmpty().sumOf { it.caloriesKcal }
            MetricBar(
                axisLabel = day.toString(),
                value = kcal.toDouble(),
                rangeLabel = formatFullDayLabel(date),
                meetsGoal = false
            )
        }
        return FoodSummaryUiState(
            timeRange = TimeRange.MONTH,
            periodLabel = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            bars = bars,
            canGoNext = month.isBefore(YearMonth.from(today))
        )
    }

    private fun dateForBarIndex(index: Int): LocalDate? = when (timeRange) {
        TimeRange.WEEK -> referenceDate
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusDays(index.toLong())
        TimeRange.MONTH -> {
            val month = YearMonth.from(referenceDate)
            if (index + 1 <= month.lengthOfMonth()) month.atDay(index + 1) else null
        }
        else -> null
    }

    private fun formatDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    private fun formatFullDayLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    private fun formatShortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
