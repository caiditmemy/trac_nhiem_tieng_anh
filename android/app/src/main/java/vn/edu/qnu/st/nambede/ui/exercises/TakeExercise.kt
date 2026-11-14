package vn.edu.qnu.st.nambede.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.data.AnswerRequest
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.Submission
import vn.edu.qnu.st.nambede.data.errorText
import vn.edu.qnu.st.nambede.ui.LocalSnackbar
import vn.edu.qnu.st.nambede.ui.common.ErrorView
import vn.edu.qnu.st.nambede.ui.common.LoadingView
import vn.edu.qnu.st.nambede.ui.common.QuestionCard
import vn.edu.qnu.st.nambede.ui.common.ScoreCard
import vn.edu.qnu.st.nambede.ui.common.formatDateTime
import vn.edu.qnu.st.nambede.ui.common.formatScore
import vn.edu.qnu.st.nambede.ui.common.isPast
import vn.edu.qnu.st.nambede.ui.common.timeLeft
import vn.edu.qnu.st.nambede.ui.theme.Rose

data class TakeExerciseState(
    val submission: Submission? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
    /** One-off message for the snackbar */
    val message: String? = null,
)

/** One attempt per exercise: answers are saved when picked (one after another, so the last click wins), then submitted once. */
class TakeExerciseViewModel(private val client: ApiClient, private val exerciseId: Long) : ViewModel() {
    private val _state = MutableStateFlow(TakeExerciseState())
    val state: StateFlow<TakeExerciseState> = _state.asStateFlow()
    private val saving = Mutex()

    init {
        load()
    }

    /** POST /exercises/{id}/start opens the attempt (Exercise Service asks Class Service whether the student is in the class). */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val submission = client.call { startExercise(exerciseId) }
                _state.update { it.copy(submission = submission, loading = false) }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                val text = errorText(
                    failure,
                    mapOf(
                        403 to "Bạn không ở trong lớp của bài tập này.",
                        404 to "Không tìm thấy bài tập.",
                        409 to "Bài tập đã quá hạn nộp, bạn không thể bắt đầu làm nữa.",
                    ),
                )
                _state.update { it.copy(loading = false, error = text) }
            }
        }
    }

    fun pick(questionId: Long, option: String) {
        val current = _state.value.submission ?: return
        if (current.status == "SUBMITTED" || isPast(current.dueAt)) return
        _state.update { state ->
            state.copy(submission = current.copy(questions = current.questions.map { if (it.id == questionId) it.copy(chosenOption = option) else it }))
        }
        viewModelScope.launch {
            saving.withLock {
                try {
                    client.call { answer(current.id, AnswerRequest(questionId, option)) }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    _state.update { it.copy(message = errorText(failure, mapOf(409 to "Bài đã nộp hoặc đã quá hạn nộp."))) }
                    load() // show what the server really saved
                }
            }
        }
    }

    fun submit() {
        val current = _state.value.submission ?: return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                saving.withLock { } // wait until the last answers are saved
                val result = client.call { submit(current.id) }
                _state.update { it.copy(submission = result, submitting = false, message = "Đã nộp bài: ${formatScore(result.score ?: 0.0)} / 10 điểm") }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _state.update { it.copy(submitting = false, message = errorText(failure, mapOf(409 to "Bài đã nộp hoặc đã quá hạn nộp."))) }
                load()
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}

@Composable
fun TakeExerciseRoute(container: AppContainer, exerciseId: Long) {
    val viewModel = viewModel(key = "exercise-$exerciseId") { TakeExerciseViewModel(container.client, exerciseId) }
    val state by viewModel.state.collectAsState()
    val snackbar = LocalSnackbar.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    TakeExerciseContent(state, onPick = viewModel::pick, onSubmit = viewModel::submit, onRetry = viewModel::load)
}

@Composable
fun TakeExerciseContent(state: TakeExerciseState, onPick: (Long, String) -> Unit, onSubmit: () -> Unit, onRetry: () -> Unit, now: Instant = Instant.now()) {
    val submission = state.submission
    when {
        submission == null && state.error != null -> ErrorView(state.error, onRetry = onRetry)
        submission == null -> LoadingView()
        submission.status == "SUBMITTED" -> ExerciseResult(submission)
        else -> ExerciseInProgress(submission, state.submitting, onPick, onSubmit, now)
    }
}

@Composable
private fun ExerciseResult(submission: Submission) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(submission.exerciseTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            submission.submittedAt?.let { Text("Đã nộp lúc ${formatDateTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { ScoreCard(submission.score ?: 0.0, submission.correctCount ?: 0, submission.questionCount) }
        items(submission.questions, key = { it.id }) { question -> QuestionCard(question, question.chosenOption, onPick = null) }
    }
}

@Composable
private fun ExerciseInProgress(submission: Submission, submitting: Boolean, onPick: (Long, String) -> Unit, onSubmit: () -> Unit, now: Instant) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    val closed = isPast(submission.dueAt, now)
    val answered = submission.questions.count { it.chosenOption != null }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(submission.exerciseTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Hạn nộp ${formatDateTime(submission.dueAt)} · ${timeLeft(submission.dueAt, now)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                submission.exerciseDescription?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
            }
            item {
                if (closed) {
                    Surface(color = Rose.copy(alpha = 0.1f), contentColor = Rose, shape = RoundedCornerShape(12.dp)) {
                        Text("Đã quá hạn nộp bài. Bài làm dở không được chấm điểm và không sửa được nữa.", modifier = Modifier.padding(12.dp).fillMaxWidth())
                    }
                } else {
                    Text("Đáp án được lưu ngay khi bạn chọn. Có thể thoát ra và làm tiếp trước hạn nộp.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(submission.questions, key = { it.id }) { question ->
                QuestionCard(question, question.chosenOption, onPick = { onPick(question.id, it) }, enabled = !closed && !submitting)
            }
        }
        Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Đã trả lời $answered / ${submission.questionCount} câu", style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(progress = { answered / maxOf(1, submission.questionCount).toFloat() }, modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.8f))
                }
                if (!closed) {
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { confirm = true }, enabled = !submitting) {
                        if (submitting) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp) else Text("Nộp bài")
                    }
                }
            }
        }
    }
    if (confirm) {
        val blank = submission.questionCount - answered
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Nộp bài?") },
            text = {
                Text(
                    if (blank > 0) "Bạn còn $blank câu chưa trả lời, các câu này sẽ tính là sai. Mỗi bài chỉ nộp được một lần."
                    else "Mỗi bài chỉ nộp được một lần. Sau khi nộp bạn sẽ thấy điểm, đáp án đúng và giải thích."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onSubmit()
                }) { Text("Nộp bài") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Huỷ") } },
        )
    }
}
