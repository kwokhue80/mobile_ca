package sg.edu.nus.iss.client.dashboard.detail.model

data class MetricBar(
    val axisLabel: String,
    val value: Double,
    val rangeLabel: String,
    val meetsGoal: Boolean
)
