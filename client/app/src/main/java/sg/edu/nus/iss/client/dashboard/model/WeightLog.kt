package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class WeightLog(
    val id: Long,
    val weightKg: Double,
    val loggedAt: LocalDateTime,
    val createdAt: LocalDateTime? = null
)
