package sg.edu.nus.iss.client.dashboard.activity.model

import android.graphics.Color
import androidx.annotation.DrawableRes
import sg.edu.nus.iss.client.R

enum class ExerciseType(
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val accentColorHex: String,
    val accentBackgroundHex: String
) {
    WALK("Walk", R.drawable.ic_activity_walk, "#27837B", "#DCEDEA"),
    RUN("Run", R.drawable.ic_activity_run, "#E2622A", "#FBE3D6"),
    SWIM("Swim", R.drawable.ic_activity_swim, "#2F8FE0", "#DBEBFB"),
    HIKE("Hike", R.drawable.ic_activity_hike, "#5B7F32", "#E3EEDA"),
    CYCLE("Cycle", R.drawable.ic_activity_cycle, "#8B61FF", "#E7DFFF");

    val accentColor: Int get() = Color.parseColor(accentColorHex)
    val accentBackground: Int get() = Color.parseColor(accentBackgroundHex)

    companion object {
        // Looks up the ExerciseType for a saved ActivityRecord.type (its displayName).
        fun fromDisplayName(displayName: String): ExerciseType? =
            entries.firstOrNull { it.displayName == displayName }

        // Icon for a saved ActivityRecord.type, falling back to a generic icon for types
        // that predate this enum or don't match any entry.
        @DrawableRes
        fun iconResFor(displayName: String): Int =
            fromDisplayName(displayName)?.iconRes ?: R.drawable.ic_activity_default
    }

}
