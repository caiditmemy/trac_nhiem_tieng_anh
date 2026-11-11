package vn.edu.qnu.st.nambede.data

import kotlinx.serialization.Serializable

// JSON bodies of the API Gateway (/api/...). Field names follow the services' camelCase JSON.
// Unknown fields are ignored, so the services can add fields without breaking the app.

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String = "ACTIVE",
    val accountType: String = "FREE",
)

@Serializable
data class LoginResponse(val accessToken: String, val user: User)

@Serializable
data class ClassInfo(
    val id: Long,
    val name: String,
    val code: String = "",
    val description: String? = null,
    val teacherName: String? = null,
    val studentCount: Int = 0,
)

@Serializable
data class JoinClassRequest(val code: String)

@Serializable
data class ExerciseSummary(
    val id: Long,
    val classId: Long,
    val className: String? = null,
    val title: String,
    val description: String? = null,
    val dueAt: String,
    val overdue: Boolean,
    val questionCount: Int,
    /** NOT_STARTED, IN_PROGRESS or SUBMITTED */
    val myStatus: String? = null,
    val myScore: Double? = null,
)

/** A question of an exercise attempt or of a practice round: answers stay null until it is submitted. */
@Serializable
data class Question(
    val id: Long,
    val position: Int,
    val content: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val chosenOption: String? = null,
    val correctOption: String? = null,
    val explanation: String? = null,
    val correct: Boolean? = null,
) {
    fun option(letter: String): String = when (letter) {
        "A" -> optionA
        "B" -> optionB
        "C" -> optionC
        else -> optionD
    }
}

@Serializable
data class Submission(
    val id: Long,
    val exerciseId: Long,
    val exerciseTitle: String,
    val exerciseDescription: String? = null,
    val dueAt: String,
    val status: String,
    val submittedAt: String? = null,
    val score: Double? = null,
    val correctCount: Int? = null,
    val questionCount: Int,
    val questions: List<Question>,
)

@Serializable
data class AnswerRequest(val exerciseQuestionId: Long, val chosenOption: String)

@Serializable
data class Streak(val currentStreak: Int, val longestStreak: Int, val activeToday: Boolean = false)

@Serializable
data class RankingEntry(val rank: Int, val studentId: Long, val fullName: String? = null, val points: Int)

@Serializable
data class PointEntry(val points: Int, val reason: String, val detail: String? = null, val createdAt: String)

@Serializable
data class RankingMe(val rank: Int? = null, val weekPoints: Int, val totalPoints: Int, val recent: List<PointEntry> = emptyList())

@Serializable
data class WeeklyRanking(val weekStart: String, val weekEnd: String, val entries: List<RankingEntry>, val me: RankingMe? = null)

@Serializable
data class UpdateNameRequest(val fullName: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** Error body shared by every NamBeDe service: {"status", "error", "message", "path"} (FastAPI defaults use "detail"). */
@Serializable
data class ErrorBody(val message: String? = null, val detail: String? = null)
