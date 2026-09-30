package com.example.android_student.data.api

import com.example.android_student.data.model.LoginRequest
import com.example.android_student.data.model.LoginResponse
import com.example.android_student.data.model.Question
import com.example.android_student.data.model.Subject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("subjects/")
    suspend fun getSubjects(@Header("Authorization") token: String): Response<List<Subject>>

    @GET("questions/")
    suspend fun getQuestions(
        @Header("Authorization") token: String,
        @Query("subject_id") subjectId: Int
    ): Response<List<Question>>
}

object RetrofitClient {
    // Địa chỉ 127.0.0.1 sử dụng khi cắm cáp USB và đã chạy lệnh adb reverse tcp:8000 tcp:8000
    private const val BASE_URL = "http://127.0.0.1:8000/api/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
} 