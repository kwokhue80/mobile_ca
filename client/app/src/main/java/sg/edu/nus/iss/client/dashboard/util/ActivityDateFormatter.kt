package sg.edu.nus.iss.client.dashboard.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ActivityDateFormatter {

    fun formatCompact(timestamp: LocalDateTime): String {
        val time = formatTimeOnly(timestamp)
        val today = LocalDate.now()
        val date = timestamp.toLocalDate()
        val dayLabel = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        }
        return "$dayLabel, $time"
    }

    fun formatTimeOnly(timestamp: LocalDateTime): String =
        timestamp.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    fun formatTimeOnly(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    fun groupLabel(timestamp: LocalDateTime): String {
        val today = LocalDate.now()
        val date = timestamp.toLocalDate()
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date.isAfter(today.minusDays(7)) -> "This Week"
            date.isAfter(today.minusMonths(1)) -> "This Month"
            else -> "Earlier"
        }
    }
}
