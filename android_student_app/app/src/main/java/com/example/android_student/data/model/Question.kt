package com.example.android_student.data.model

import com.google.gson.annotations.SerializedName
data class Question(
    val id: Int,
    val subject: Int,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("option_a") val optionA: String,
    @SerializedName("option_b") val optionB: String,
    @SerializedName("option_c") val optionC: String,
    @SerializedName("option_d") val optionD: String,
    @SerializedName("correct_answer") val correctAnswer: String? = "A",
    @SerializedName("explanation") val explanation: String? = "Chưa có lời giải chi tiết cho câu hỏi này."
)

data class ExamHistoryItem(
    val subjectName: String,
    val score: Double,
    val correctCount: Int,
    val totalCount: Int,
    val date: String,
    val isTimed: Boolean
)