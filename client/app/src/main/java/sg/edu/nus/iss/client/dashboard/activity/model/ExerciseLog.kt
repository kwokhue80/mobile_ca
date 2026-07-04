package sg.edu.nus.iss.client.dashboard.activity.model

import java.time.LocalDateTime

data class ExerciseLog(
    // exercise_logs.id (BIGINT)
    val id: Long,
    // exercise_logs.exercise_type (VARCHAR(100))
    val exerciseType: String,
    // exercise_logs.duration_minutes (INT)
    val durationMinutes: Int,
    // exercise_logs.distance_km (DECIMAL(6,2), nullable)
    val distanceKm: Double? = null,
    // exercise_logs.calories_burned_kcal (INT)
    val caloriesBurnedKcal: Int,
    // exercise_logs.logged_at (DATETIME)
    val loggedAt: LocalDateTime,
    // exercise_logs.created_at (DATETIME)
    val createdAt: LocalDateTime? = null
)
