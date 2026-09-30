package com.example.android_student.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_student.data.api.RetrofitClient
import com.example.android_student.data.local.AppDatabase
import com.example.android_student.data.local.ExamHistoryEntity
import com.example.android_student.data.model.Question
import com.example.android_student.data.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val historyFlow = db.examHistoryDao().getAllHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var subjects by mutableStateOf<List<Subject>>(emptyList())
    var questions by mutableStateOf<List<Question>>(emptyList())
    var currentSubject by mutableStateOf<Subject?>(null)

    // Chế độ: true = Thi thử (50 câu bấm giờ), false = Luyện tập tự do
    var isSimulationMode by mutableStateOf(false)
    var examDurationMinutes by mutableIntStateOf(30)
    var remainingSeconds by mutableIntStateOf(0)

    // Đáp án sinh viên đã chọn (Index -> "A"/"B"/"C"/"D")
    val selectedAnswers = mutableStateMapOf<Int, String>()

    // Kết quả sau khi nộp
    var lastScore by mutableDoubleStateOf(0.0)
    var lastCorrectCount by mutableIntStateOf(0)
    var isPassed by mutableStateOf(false)

    private var timerJob: Job? = null

    fun loadSubjects(token: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getSubjects("Bearer $token")
                if (res.isSuccessful) {
                    subjects = res.body() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun startExam(
        subject: Subject,
        token: String,
        isSimulation: Boolean,
        durationMinutes: Int = 30,
        onLoaded: () -> Unit
    ) {
        currentSubject = subject
        isSimulationMode = isSimulation
        examDurationMinutes = durationMinutes
        selectedAnswers.clear()
        timerJob?.cancel()

        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getQuestions("Bearer $token", subject.id)
                if (res.isSuccessful) {
                    val rawList = res.body() ?: emptyList()
                    questions = if (isSimulation) {
                        // Rút ngẫu nhiên tối đa 50 câu
                        rawList.shuffled().take(50)
                    } else {
                        rawList
                    }

                    if (isSimulation) {
                        remainingSeconds = durationMinutes * 60
                        startTimer {
                            // Tự động thu bài khi hết giờ
                            submitExam(onFinished = onLoaded)
                        }
                    }
                    onLoaded()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startTimer(onTimeUp: () -> Unit) {
        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            onTimeUp()
        }
    }

    fun submitExam(onFinished: () -> Unit) {
        timerJob?.cancel()
        var correct = 0
        questions.forEachIndexed { index, q ->
            val ans = selectedAnswers[index]
            val correctAns = q.correctAnswer ?: "A"
            if (ans.equals(correctAns, ignoreCase = true)) {
                correct++
            }
        }
        lastCorrectCount = correct
        val calculatedScore = if (questions.isNotEmpty()) (correct.toDouble() / questions.size) * 10.0 else 0.0
        lastScore = Math.round(calculatedScore * 10.0) / 10.0
        isPassed = lastScore >= 5.0

        val dateFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        val historyEntity = ExamHistoryEntity(
            subjectName = currentSubject?.name ?: "Môn học",
            score = lastScore,
            correctCount = correct,
            totalCount = questions.size,
            date = dateFormat.format(Date()),
            isPassed = isPassed,
            mode = if (isSimulationMode) "Thi thử (${examDurationMinutes}p)" else "Luyện tập tự do"
        )

        viewModelScope.launch {
            db.examHistoryDao().insertHistory(historyEntity)
            onFinished()
        }
    }
}