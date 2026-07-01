package sg.edu.nus.iss.client.dashboard.activity.model

import androidx.annotation.DrawableRes
import sg.edu.nus.iss.client.R

enum class ExerciseType(val displayName: String, @DrawableRes val iconRes: Int) {
    WALK("Walk", R.drawable.ic_activity_walk),
    RUN("Run", R.drawable.ic_activity_run),
    SWIM("Swim", R.drawable.ic_activity_swim)
}
