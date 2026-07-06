package sg.edu.nus.iss.client.dashboard.detail.model

import android.graphics.Color
import androidx.annotation.DrawableRes
import sg.edu.nus.iss.client.R

enum class MetricType(
    val displayName: String,
    val unit: String,
    val defaultGoal: Double,
    @DrawableRes val iconRes: Int,
    val chartColorHex: String,
    val chartGoalMetColorHex: String,
    val decimalPlaces: Int
) {
    DISTANCE(
        displayName = "Distance",
        unit = "km",
        defaultGoal = 8.05,
        iconRes = R.drawable.distance_icon,
        chartColorHex = "#27837B",
        chartGoalMetColorHex = "#A6E2DD",
        decimalPlaces = 2
    ),
    CALORIES(
        displayName = "Calories",
        unit = "Cal",
        defaultGoal = 500.0,
        iconRes = R.drawable.calories_icon,
        chartColorHex = "#27837B",
        chartGoalMetColorHex = "#A6E2DD",
        decimalPlaces = 0
    ),
    HYDRATION(
        displayName = "Hydration",
        unit = "ml",
        defaultGoal = 1500.0,
        iconRes = R.drawable.hydration_icon,
        chartColorHex = "#52ADFF",
        chartGoalMetColorHex = "#B8C0F0",
        decimalPlaces = 0
    ),
    SLEEP(
        displayName = "Sleep",
        unit = "h",
        defaultGoal = 8.0,
        iconRes = R.drawable.sleep_icon,
        chartColorHex = "#8B61FF",
        chartGoalMetColorHex = "#C9A6E2",
        decimalPlaces = 1
    ),
    WEIGHT(
        displayName = "Weight",
        unit = "kg",
        defaultGoal = 65.0,
        iconRes = R.drawable.weight_icon,
        chartColorHex = "#27837B",
        chartGoalMetColorHex = "#A6E2DD",
        decimalPlaces = 1
    );

    val chartColor: Int get() = Color.parseColor(chartColorHex)
    val chartGoalMetColor: Int get() = Color.parseColor(chartGoalMetColorHex)

    fun formatValue(value: Double): String = if (decimalPlaces == 0) {
        "%,.0f".format(value)
    } else {
        "%.${decimalPlaces}f".format(value)
    }
}
