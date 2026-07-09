// Author: Amelia Wong
package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class MoodLog(
    val id: Long,
    val loggedAt: LocalDateTime,
    val moodRating: Int,
    val notes: String? = null,
    val createdAt: LocalDateTime? = null
)
