package com.example.android_student.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val role: String,
    @SerializedName("full_name") val fullName: String,
    val email: String
)