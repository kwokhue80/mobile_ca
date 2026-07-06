package sg.edu.nus.iss.client.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("api/dashboard/daily")
    suspend fun getDailyDashboard(
        @Query("date") date: String     // Format: "yyyy-MM-dd"
    ): Response<DashboardDailyResponse>

    @GET("api/wellness/recommendations/latest")
    suspend fun getLatestRecommendation(): Response<RecommendationResponse>
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
    val moodRating: Int? = null,                // Consolidated mood and stress
)

data class RecommendationResponse(val recommendation: String, val generatedAt: String)

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
    val sleepMinutes: Int? = null,
    val sleepQualityScore: Int? = null,
    val moodScore: Int? = null,
    val weightKg: Double? = null
)