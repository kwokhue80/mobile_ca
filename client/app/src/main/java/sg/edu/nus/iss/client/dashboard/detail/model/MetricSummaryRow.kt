package sg.edu.nus.iss.client.dashboard.detail.model

data class MetricSummaryRow(
    val label: String,
    val value: Double,
    val isCurrentPeriod: Boolean
)
