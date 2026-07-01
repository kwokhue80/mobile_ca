package sg.edu.nus.iss.client.dashboard.model

data class ActivityRecord(
    val id: String,
    val type: String,
    val date: String,
    val durationMinutes: Int
)
