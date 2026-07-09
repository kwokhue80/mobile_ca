// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail.model

data class MetricDetailUiState(
    val timeRange: TimeRange = TimeRange.DAY,
    val periodLabel: String = "",
    val totalValue: Double = 0.0,
    val goalValue: Double = 0.0,
    val chartGoalValue: Double = 0.0,
    val subtitle: String = "",
    val bars: List<MetricBar> = emptyList(),
    val selectedBarIndex: Int? = null,
    val summaryRows: List<MetricSummaryRow> = emptyList(),
    val canGoNext: Boolean = false,
    val canGoPrevious: Boolean = true,
    // Sleep quality (1-10, real backend data), averaged over whatever date range this
    // state represents. Only populated when metricType == SLEEP.
    val sleepQualityScore: Double? = null
)
