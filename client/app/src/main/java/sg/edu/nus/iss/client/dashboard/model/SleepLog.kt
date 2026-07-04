package sg.edu.nus.iss.client.dashboard.model

import java.time.LocalDateTime

data class SleepLog(
    val id: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val sleepQualityScore: Int? = null,
    val createdAt: LocalDateTime? = null
) {
    val durationMinutes: Int
        get() = java.time.Duration.between(startTime, endTime).toMinutes().toInt().coerceAtLeast(0)
}
