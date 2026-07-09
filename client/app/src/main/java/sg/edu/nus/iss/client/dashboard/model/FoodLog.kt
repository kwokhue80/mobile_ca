// Author: Amelia Wong
package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class FoodLog(
    val id: Long,
    val mealType: String? = null,
    val foodName: String,
    val caloriesKcal: Int,
    val loggedAt: LocalDateTime,
    val createdAt: LocalDateTime? = null
)
