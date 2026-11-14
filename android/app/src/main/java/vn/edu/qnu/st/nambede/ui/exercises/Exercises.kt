package vn.edu.qnu.st.nambede.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.ExerciseSummary
import vn.edu.qnu.st.nambede.ui.common.EmptyView
import vn.edu.qnu.st.nambede.ui.common.ErrorView
import vn.edu.qnu.st.nambede.ui.common.LoadingView
import vn.edu.qnu.st.nambede.ui.common.LoadingViewModel
import vn.edu.qnu.st.nambede.ui.common.Remote
import vn.edu.qnu.st.nambede.ui.common.SectionTitle
import vn.edu.qnu.st.nambede.ui.common.StatusLabel
import vn.edu.qnu.st.nambede.ui.common.formatDateTime
import vn.edu.qnu.st.nambede.ui.common.formatScore
import vn.edu.qnu.st.nambede.ui.common.parseInstant
import vn.edu.qnu.st.nambede.ui.common.scoreColor
import vn.edu.qnu.st.nambede.ui.common.timeLeft
import vn.edu.qnu.st.nambede.ui.theme.Amber
import vn.edu.qnu.st.nambede.ui.theme.Emerald
import vn.edu.qnu.st.nambede.ui.theme.Rose
import vn.edu.qnu.st.nambede.ui.theme.Sky

class ExercisesViewModel(private val client: ApiClient) : LoadingViewModel<List<ExerciseSummary>>() {
    /** Exercise Service lists the exercises of the student's classes (it asks Class Service which ones). */
    override suspend fun fetch(): List<ExerciseSummary> = client.call { exercises() }
}

@Composable
fun ExercisesRoute(container: AppContainer, onOpen: (Long) -> Unit) {
    val viewModel = viewModel { ExercisesViewModel(container.client) }
    // Reload when coming back from an exercise, so its status and score are up to date
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsState()
    ExercisesContent(state, onRefresh = viewModel::load, onOpen = onOpen)
}

/** Same groups as the web app: to do (soonest due first), submitted, overdue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesContent(state: Remote<List<ExerciseSummary>>, onRefresh: () -> Unit, onOpen: (Long) -> Unit, now: Instant = Instant.now()) {
    val exercises = state.data
    when {
        exercises == null && state.error != null -> ErrorView(state.error, onRetry = onRefresh)
        exercises == null -> LoadingView()
        else -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            val todo = exercises.filter { it.myStatus != "SUBMITTED" && !it.overdue }.sortedBy { it.dueAt }
            val done = exercises.filter { it.myStatus == "SUBMITTED" }
            val missed = exercises.filter { it.myStatus != "SUBMITTED" && it.overdue }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (exercises.isEmpty()) {
                    item { EmptyView("Chưa có bài tập nào", "Bài tập giảng viên giao cho lớp của bạn sẽ hiện ở đây.") }
                } else {
                    item { SectionTitle("Cần làm (${todo.size})") }
                    if (todo.isEmpty()) {
                        item { Text("Bạn đã làm hết bài tập đang mở.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(todo, key = { it.id }) { ExerciseCard(it, now, onOpen) }
                    if (done.isNotEmpty()) {
                        item { SectionTitle("Đã nộp (${done.size})") }
                        items(done, key = { it.id }) { ExerciseCard(it, now, onOpen) }
                    }
                    if (missed.isNotEmpty()) {
                        item { SectionTitle("Quá hạn (${missed.size})") }
                        items(missed, key = { it.id }) { ExerciseCard(it, now, onOpen) }
                    }
                }
                if (state.error != null) {
                    item { Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseSummary, now: Instant, onOpen: (Long) -> Unit) {
    val status = exercise.myStatus ?: "NOT_STARTED"
    val submitted = status == "SUBMITTED"
    val missed = !submitted && exercise.overdue
    // An overdue exercise that was never started cannot be opened any more
    val canOpen = !(missed && status == "NOT_STARTED")
    val (label, color) = when {
        missed -> "Quá hạn" to Rose
        status == "SUBMITTED" -> "Đã nộp" to Emerald
        status == "IN_PROGRESS" -> "Đang làm" to Sky
        else -> "Chưa làm" to Amber
    }
    OutlinedCard(onClick = { onOpen(exercise.id) }, enabled = canOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(exercise.className ?: "Lớp #${exercise.classId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                StatusLabel(label, color)
            }
            Spacer(Modifier.height(8.dp))
            Text("Hạn nộp ${formatDateTime(exercise.dueAt)} · ${exercise.questionCount} câu", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            val score = exercise.myScore
            if (submitted && score != null) {
                Text("${formatScore(score)} / 10 điểm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = scoreColor(score))
            } else {
                Text(
                    if (runCatching { parseInstant(exercise.dueAt) }.getOrNull()?.isAfter(now) == true) timeLeft(exercise.dueAt, now) else "Đã quá hạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (missed) Rose else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
