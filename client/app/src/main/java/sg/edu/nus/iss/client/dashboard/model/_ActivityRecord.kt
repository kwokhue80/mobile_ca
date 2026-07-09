// Author: HuaYuan Xie, Amelia Wong
package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class ActivityRecord(
    val id: String,
    val type: String,
    val timestamp: LocalDateTime,
    val durationMinutes: Int,
    val distanceKm: Double = 0.0,
    val calories: Int = 0
)