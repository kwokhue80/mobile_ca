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
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.backend.BackendApi
import sg.edu.nus.iss.client.backend.BackendConfig
import sg.edu.nus.iss.client.backend.BackendRepository
import sg.edu.nus.iss.client.network.AuthInterceptor
import sg.edu.nus.iss.client.util.SessionManager
import java.util.concurrent.TimeUnit

class RecommendationPollWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recommendation-poll-worker"
        const val INIT_WORK_NAME = "recommendation-initial-worker"
        const val CHANNEL_ID = "wellness_recommendations"
        const val KEY_FORCE_NOTIFY_ON_FIRST_FETCH = "force_notify_on_first_fetch"
    }

    override suspend fun doWork(): Result {
        // Use app-context dependencies because worker may run when UI is not active.
        val sessionManager = SessionManager.getInstance(applicationContext)
        val backendRepository = buildBackendRepository(sessionManager)
        val forceNotifyOnFirstFetch = inputData.getBoolean(KEY_FORCE_NOTIFY_ON_FIRST_FETCH, false)
        val hadExistingSignature = !sessionManager.getRecommendationSignature().isNullOrBlank()

        // Retry only when network/API call itself fails
        val payload = runCatching { backendRepository.getRecommendations() }.getOrNull()
            ?: return Result.retry()

        sessionManager.updateLastRecommendationFetchTime()

        // Shared signature logic prevent duplicate notifications
        val isNewRecommendation = sessionManager.upsertRecommendationAndDetectNew(
            recommendationText = payload.message,
            generatedAt = payload.generatedAt
        )

        val shouldNotifyInitial = forceNotifyOnFirstFetch && !hadExistingSignature

        if (isNewRecommendation || shouldNotifyInitial) {
            if (shouldNotifyInitial && !isNewRecommendation) {
                sessionManager.incrementUnreadRecommendationCount()
            }
            showSystemNotification(
                recommendation = payload.message,
                unreadCount = sessionManager.getUnreadRecommendationCount()
            )
        }

        return Result.success()
    }

    private fun buildBackendRepository(sessionManager: SessionManager): BackendRepository {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .writeTimeout(75, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BackendConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val backendApi = retrofit.create(BackendApi::class.java)
        return BackendRepository(backendApi)
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

        // Notification posting requires runtime permission on Android 13+
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
        // Channels are required only on Android 8.0+
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
