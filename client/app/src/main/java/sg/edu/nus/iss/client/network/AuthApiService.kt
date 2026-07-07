package sg.edu.nus.iss.client.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord

interface AuthApiService {

    // User registration
    @POST("api/auth/register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse>

    // User login
    @POST("api/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    // User logout
    @POST("api/auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    @POST("api/wellness/records")
    suspend fun saveRecord(
        @Body wellnessRecord: WellnessRecord
    ): Response<Void>

    // Testing convenience: wipes today's sleep/hydration/weight/mood/exercise records
    // and the aggregated daily summary for the current user. Goals and profile are
    // untouched. Called on every login so each test session starts from a clean slate.
    @POST("api/wellness/reset-today")
    suspend fun resetToday(): Response<Void>

    @GET("api/dashboard/daily")
    suspend fun getDailyDashboard(
        @Query("date") date: String     // Format: "yyyy-MM-dd"
    ): Response<DashboardDailyResponse>

    @GET("api/wellness/recommendations/latest")
    suspend fun getLatestRecommendation(): Response<RecommendationResponse>

    // Fetch current user's profile (name/DOB/gender/height); fields are null until first saved
    @GET("api/user-profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    // Create or update the current user's profile
    @PUT("api/user-profile")
    suspend fun updateUserProfile(
        @Body request: UserProfileUpdateRequest
    ): Response<UserProfileResponse>

    // Fetch all of the current user's saved goals
    @GET("api/user-goals")
    suspend fun getUserGoals(): Response<List<UserGoalResponse>>

    // Create or update the current user's goal for a given goal type (e.g. "DISTANCE", "EXERCISE")
    @PUT("api/user-goals/{goalType}")
    suspend fun updateUserGoal(
        @Path("goalType") goalType: String,
        @Body request: UserGoalUpsertRequest
    ): Response<UserGoalResponse>

    // Fetch a date range of daily summaries (used for sleep quality history)
    @GET("api/dashboard/range")
    suspend fun getDashboardRange(
        @Query("startDate") startDate: String,  // Format: "yyyy-MM-dd"
        @Query("endDate") endDate: String       // Format: "yyyy-MM-dd"
    ): Response<List<DailyWellnessSummary>>

    // Fetch structured exercise sessions (duration/distance/calories/start-end time)
    @GET("api/wellness/exercise-logs")
    suspend fun getExerciseLogs(
        @Query("days") days: Int = 30
    ): Response<List<ExerciseLogResponse>>
}

data class LogoutResponse(val token: String?)

data class RegisterRequest(val emailAddress: String, val password: String)
data class RegisterResponse(val token: String)

data class  LoginRequest(val emailAddress: String, val password: String)
data class LoginResponse(val token: String)

data class WellnessRecord(
    val recordDate: String,                     // Format: "yyyy-MM-dd HH:mm:ss"
    val sleepMinutes: Int? = null,              // Sleep duration in minutes
    val sleepQualityRating: Int? = null,        // Sleep quality rating (1-5)
    val mealType: String? = null,               // Meal type (e.g., breakfast, lunch, dinner)
    val mealDescription: String? = null,        // Meal description
    val mealCaloriesKcal: Int? = null,          // Meal calories in kilocalories
    val weightKg: Double? = null,               // Weight in kilograms
    val waterIntakeMl: Int? = null,             // Water intake in milliliters
    val exerciseType: String? = null,           // Exercise activity description
    val exerciseDurationMinutes: Int? = null,   // Exercise duration in minutes
    val exerciseDistanceKm: Double? = null,     // Exercise distance in kilometers
    val exerciseCaloriesBurnedKcal: Int? = null,// Exercise calories burned
    val exerciseStartTime: String? = null,      // Format: "yyyy-MM-dd HH:mm:ss"
    val exerciseEndTime: String? = null,        // Format: "yyyy-MM-dd HH:mm:ss"
    val moodRating: Int? = null,                // Consolidated mood and stress
)

data class RecommendationResponse(val recommendation: String, val generatedAt: String)

data class UserProfileResponse(
    val userId: String,
    val emailAddress: String,
    val fullName: String?,
    val dateOfBirth: String?,   // Format: "yyyy-MM-dd"
    val gender: String?,
    val heightCm: Double?
)

data class UserProfileUpdateRequest(
    val fullName: String,
    val dateOfBirth: String,   // Format: "yyyy-MM-dd"
    val gender: String,
    val heightCm: Double
)

data class UserGoalResponse(
    val goalType: String,
    val targetValue: Double,
    val unit: String
)

data class UserGoalUpsertRequest(val targetValue: Double)

data class DashboardDailyResponse(
    val dailyWellnessSummary: DailyWellnessSummary,
    val activityRecords: List<ActivityRecord>
)

data class DailyWellnessSummary(
    val id: Long? = null,
    val summaryDate: String,
    val totalWaterMl: Int,
    val totalCaloriesIntake: Int,
    val totalCaloriesBurned: Int,
    val totalExerciseMinutes: Int,
    val totalDistanceKm: Double = 0.0,
    val sleepMinutes: Int? = null,
    val sleepQualityScore: Int? = null,
    val moodScore: Int? = null,
    val weightKg: Double? = null
)

data class ExerciseLogResponse(
    val id: Long,
    val exerciseType: String,
    val durationMinutes: Int,
    val distanceKm: Double? = null,
    val caloriesBurnedKcal: Int,
    val startTime: String? = null,   // ISO-8601, e.g. "2026-07-07T14:30:00" (no @JsonFormat on the backend field)
    val endTime: String? = null,     // ISO-8601
    val loggedAt: String              // ISO-8601
)