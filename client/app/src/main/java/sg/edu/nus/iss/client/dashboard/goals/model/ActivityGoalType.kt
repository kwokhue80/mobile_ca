// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.goals.model

import androidx.annotation.DrawableRes
import sg.edu.nus.iss.client.R

enum class ActivityGoalType(
    val displayName: String,
    val unitLabel: String,
    @DrawableRes val iconRes: Int,
    val defaultValue: Double,
    val step: Double,
    val minValue: Double,
    val maxValue: Double,
    val decimalPlaces: Int
) {
    DISTANCE(
        displayName = "Distance",
        unitLabel = "km per day",
        iconRes = R.drawable.distance_icon,
        defaultValue = 8.0,
        step = 0.5,
        minValue = 0.0,
        maxValue = 50.0,
        decimalPlaces = 1
    ),
    CALORIES(
        displayName = "Calories",
        unitLabel = "Cal per day",
        iconRes = R.drawable.calories_icon,
        defaultValue = 500.0,
        step = 50.0,
        minValue = 0.0,
        maxValue = 5000.0,
        decimalPlaces = 0
    ),
    SLEEP(
        displayName = "Sleep",
        unitLabel = "hours per day",
        iconRes = R.drawable.sleep_icon,
        defaultValue = 8.0,
        step = 0.5,
        minValue = 0.0,
        maxValue = 16.0,
        decimalPlaces = 1
    ),
    HYDRATION(
        displayName = "Hydration",
        unitLabel = "ml per day",
        iconRes = R.drawable.hydration_icon,
        defaultValue = 1500.0,
        step = 250.0,
        minValue = 0.0,
        maxValue = 5000.0,
        decimalPlaces = 0
    ),
    WEIGHT(
        displayName = "Weight",
        unitLabel = "kg",
        iconRes = R.drawable.weight_icon,
        defaultValue = 70.0,
        step = 0.5,
        minValue = 20.0,
        maxValue = 300.0,
        decimalPlaces = 1
    ),
    EXERCISE_DAYS(
        displayName = "Exercise Days",
        unitLabel = "Days per week",
        iconRes = R.drawable.exercise_days_icon,
        defaultValue = 4.0,
        step = 1.0,
        minValue = 0.0,
        maxValue = 7.0,
        decimalPlaces = 0
    );

    fun formatValue(value: Double): String = if (decimalPlaces == 0) {
        "%,.0f".format(value)
    } else {
        "%.${decimalPlaces}f".format(value)
    }

    // Backend's GoalType enum has no "_DAYS" suffix on EXERCISE; every other name matches as-is.
    val backendGoalType: String
        get() = if (this == EXERCISE_DAYS) "EXERCISE" else name

    companion object {
        fun fromBackendGoalType(value: String): ActivityGoalType? =
            entries.firstOrNull { it.backendGoalType == value }
    }
}
