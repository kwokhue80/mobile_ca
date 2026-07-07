package sg.edu.nus.iss.client.dashboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import sg.edu.nus.iss.client.MainActivity
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.util.SessionManager

class RecommendationPollWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recommendation-poll-worker"
        const val CHANNEL_ID = "wellness_recommendations"
    }

    override suspend fun doWork(): Result {
        // Use app-context dependencies because worker may run when UI is not active.
        val sessionManager = SessionManager(applicationContext)
        val authApiService = RetrofitClient.getApiService(applicationContext)

        // Retry only when network/API call itself fails.
        val response = runCatching { authApiService.getLatestRecommendation() }.getOrNull()
            ?: return Result.retry()

        if (!response.isSuccessful) {
            // Non-2xx responses are treated as handled to avoid aggressive retries.
            return Result.success()
        }

        val payload = response.body() ?: return Result.success()
        // Shared signature logic prevents duplicate notifications.
        val isNewRecommendation = sessionManager.upsertRecommendationAndDetectNew(
            recommendationText = payload.recommendation,
            generatedAt = payload.generatedAt
        )

        if (isNewRecommendation) {
            showSystemNotification(
                recommendation = payload.recommendation,
                unreadCount = sessionManager.getUnreadRecommendationCount()
            )
        }

        return Result.success()
    }

    private fun showSystemNotification(recommendation: String, unreadCount: Int) {
        // Ensure channel exists before posting any notification.
        createNotificationChannel()

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_bell)
            .setContentTitle("New wellness recommendation")
            .setContentText(recommendation)
            .setStyle(NotificationCompat.BigTextStyle().bigText(recommendation))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setNumber(unreadCount)
            .build()

        // Notification posting requires runtime permission on Android 13+.
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return
        }

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }

    private fun createNotificationChannel() {
        // Channels are required only on Android 8.0+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wellness recommendations",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when a new personalized recommendation is available"
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
