// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.badges.model

import androidx.annotation.DrawableRes
import sg.edu.nus.iss.client.R

/** One achievement per dashboard metric card, plus a 9th combined "All-Rounder"
 *  achievement, filling out the 3x3 badge grid. [iconRes] is a user-supplied
 *  image unique to each achievement (local demo project, not for distribution). */
enum class BadgeType(
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int
) {
    DISTANCE_MASTER(
        title = "Marathoner",
        description = "Run a total of 1,000 km",
        iconRes = R.drawable.badge_marathoner
    ),
    STEP_CHAMPION(
        title = "Step Champion",
        description = "Take a total of 500,000 steps",
        iconRes = R.drawable.badge_step_champion
    ),
    CALORIE_CRUSHER(
        title = "Calorie Crusher",
        description = "Burn a total of 50,000 Cal",
        iconRes = R.drawable.badge_calorie_crusher
    ),
    HYDRATION_HERO(
        title = "Hydration Hero",
        description = "Drink a total of 100,000 ml of water",
        iconRes = R.drawable.badge_hydration_hero
    ),
    SLEEP_CHAMPION(
        title = "Well Rested",
        description = "Average 8h of sleep for 30 days",
        iconRes = R.drawable.badge_well_rested
    ),
    WEIGHT_GOAL(
        title = "Goal Getter",
        description = "Reach your weight goal",
        iconRes = R.drawable.badge_goal_getter
    ),
    CONSISTENCY_KING(
        title = "Consistency King",
        description = "Exercise on 100 different days",
        iconRes = R.drawable.badge_consistency_king
    ),
    POSITIVE_MIND(
        title = "Positive Mind",
        description = "Average a mood of 7/10 for a month",
        iconRes = R.drawable.badge_positive_mind
    ),
    ALL_ROUNDER(
        title = "All-Rounder",
        description = "Hit your daily goal in every category on the same day",
        iconRes = R.drawable.badge_all_rounder
    )
}
