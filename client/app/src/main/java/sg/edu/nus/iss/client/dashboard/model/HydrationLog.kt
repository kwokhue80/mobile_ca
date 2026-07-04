package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class HydrationLog(
    val id: Long,
    val loggedAt: LocalDateTime,
    val volumeMl: Int,
    val createdAt: LocalDateTime? = null
)
