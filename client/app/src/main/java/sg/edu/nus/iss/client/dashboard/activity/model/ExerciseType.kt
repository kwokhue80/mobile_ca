// Author: HuaYuan Xie
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
    CYCLE("Cycle", R.drawable.ic_activity_cycle, "#8B61FF", "#E7DFFF"),
    YOGA("Yoga", R.drawable.ic_activity_yoga, "#D6488F", "#FCE4F0");

    val accentColor: Int get() = Color.parseColor(accentColorHex)
    val accentBackground: Int get() = Color.parseColor(accentBackgroundHex)

    // The backend's ExerciseType enum uses a much larger, gerund-form vocabulary
    // (WALKING, RUNNING, ...) shared with the "Add" sheet's exercise form; map this
    // enum's 5 values onto their backend equivalents for the "Add Activity" flow.
    val backendExerciseType: String
        get() = when (this) {
            WALK -> "WALKING"
            RUN -> "RUNNING"
            SWIM -> "SWIMMING"
            HIKE -> "HIKING"
            CYCLE -> "CYCLING"
            YOGA -> "YOGA"
        }

    companion object {
        // Looks up the ExerciseType for a saved ActivityRecord.type (its displayName).
        fun fromDisplayName(displayName: String): ExerciseType? =
            entries.firstOrNull { it.displayName == displayName }

        // Icon for a saved ActivityRecord.type, falling back to a generic icon for types
        // that predate this enum or don't match any entry.
        @DrawableRes
        fun iconResFor(displayName: String): Int =
            fromDisplayName(displayName)?.iconRes ?: R.drawable.ic_activity_default

        // Reverse of backendExerciseType, for mapping GET /api/wellness/exercise-logs
        // results back into this enum. Returns null for backend types with no
        // equivalent here (e.g. TENNIS) - callers fall back to a generic display.
        fun fromBackendExerciseType(value: String): ExerciseType? =
            entries.firstOrNull { it.backendExerciseType == value }
    }

}
