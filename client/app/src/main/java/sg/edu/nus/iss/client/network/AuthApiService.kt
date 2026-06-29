package sg.edu.nus.iss.client.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("api/wellness/records")
    suspend fun saveRecord(
        @Body wellnessRecord: WellnessRecord
    ): Response<Void>

    @GET("api/wellness/recommendations/latest")
    suspend fun getLatestRecommendation(): Response<RecommendationResponse>
}

data class  LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String)
data class WellnessRecord(
    val recordDate: String,
    val sleepHours: Double,
    val moodRating: Int,
    val exerciseActivity: String,
    val exerciseDurationMinutes: Int,
    val stressLevel: Int,
    val waterIntakeLiters: Double
)
data class RecommendationResponse(val recommendation: String, val generatedAt: String)