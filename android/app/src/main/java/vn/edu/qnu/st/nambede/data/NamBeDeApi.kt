package vn.edu.qnu.st.nambede.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Every call goes to the API Gateway, exactly like the web app; the gateway forwards to the services. */
interface NamBeDeApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("users/me")
    suspend fun me(): User

    @PUT("users/me")
    suspend fun updateMe(@Body body: UpdateNameRequest): User

    /** 204 on success; a wrong current password answers 403 (not 401), so the session is kept. */
    @PUT("users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    /** Learning Service: days in a row with an exercise submitted. */
    @GET("streak")
    suspend fun streak(): Streak

    /** Learning Service: this week's ranking; `me.totalPoints` is the student's activity points (limit 0: only `me`). */
    @GET("ranking/weekly")
    suspend fun ranking(@Query("limit") limit: Int = 10): WeeklyRanking

    @GET("classes")
    suspend fun classes(): List<ClassInfo>

    @POST("classes/join")
    suspend fun joinClass(@Body body: JoinClassRequest): ClassInfo

    @GET("exercises")
    suspend fun exercises(): List<ExerciseSummary>

    @POST("exercises/{id}/start")
    suspend fun startExercise(@Path("id") exerciseId: Long): Submission

    @POST("submissions/{id}/answer")
    suspend fun answer(@Path("id") submissionId: Long, @Body body: AnswerRequest): Submission

    @POST("submissions/{id}/submit")
    suspend fun submit(@Path("id") submissionId: Long): Submission
}
