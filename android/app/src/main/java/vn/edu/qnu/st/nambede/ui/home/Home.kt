package vn.edu.qnu.st.nambede.ui.home

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.ClassInfo
import vn.edu.qnu.st.nambede.data.JoinClassRequest
import vn.edu.qnu.st.nambede.data.Streak
import vn.edu.qnu.st.nambede.data.WeeklyRanking
import vn.edu.qnu.st.nambede.data.User
import vn.edu.qnu.st.nambede.data.errorText
import vn.edu.qnu.st.nambede.ui.LocalSnackbar
import vn.edu.qnu.st.nambede.ui.common.EmptyView
import vn.edu.qnu.st.nambede.ui.common.ErrorView
import vn.edu.qnu.st.nambede.ui.common.LoadingView
import vn.edu.qnu.st.nambede.ui.common.LoadingViewModel
import vn.edu.qnu.st.nambede.ui.common.Remote
import vn.edu.qnu.st.nambede.ui.common.SectionTitle
import vn.edu.qnu.st.nambede.ui.common.StatCard
import vn.edu.qnu.st.nambede.ui.common.StatusLabel
import vn.edu.qnu.st.nambede.ui.theme.Amber
import vn.edu.qnu.st.nambede.ui.theme.Emerald
import vn.edu.qnu.st.nambede.ui.theme.Rose
import vn.edu.qnu.st.nambede.ui.theme.Sky

/** The student's overview: classes and exercises from Class and Exercise Service, streak and points from Learning Service. */
data class HomeData(
    val classes: List<ClassInfo>,
    val pendingExercises: Int,
    /** null while Learning Service cannot be reached */
    val streak: Int?,
    val points: Int?,
)

class HomeViewModel(private val client: ApiClient) : LoadingViewModel<HomeData>() {
    var joining by mutableStateOf(false)
        private set
    var joinError by mutableStateOf<String?>(null)
        private set

    override suspend fun fetch(): HomeData = coroutineScope {
        val classes = async { client.call { classes() } }
        val exercises = async { client.call { exercises() } }
        // Streak and points are optional: when Learning Service is down the rest still shows
        val streak = async { runCatching { client.call { streak() } }.getOrNull() }
        val ranking = async { runCatching { client.call { ranking(0) } }.getOrNull() }
        val pending = exercises.await().count { it.myStatus != "SUBMITTED" && !it.overdue }
        HomeData(classes.await(), pending, streak.await()?.currentStreak, ranking.await()?.me?.totalPoints)
    }

    fun join(code: String, onJoined: (ClassInfo) -> Unit) {
        if (code.isBlank()) {
            joinError = "Nhập mã lớp do giảng viên gửi."
            return
        }
        joining = true
        joinError = null
        viewModelScope.launch {
            try {
                onJoined(client.call { joinClass(JoinClassRequest(code.trim())) })
                load()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                joinError = errorText(failure, mapOf(404 to "Không tìm thấy lớp với mã này.", 409 to "Bạn đã ở trong lớp này rồi."))
            } finally {
                joining = false
            }
        }
    }

    fun dismissJoinError() {
        joinError = null
    }
}

@Composable
fun HomeRoute(container: AppContainer, user: User, onOpenExercises: () -> Unit) {
    val viewModel = viewModel { HomeViewModel(container.client) }
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsState()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var joinOpen by rememberSaveable { mutableStateOf(false) }

    HomeContent(
        user = user,
        state = state,
        onRefresh = viewModel::load,
        onOpenExercises = onOpenExercises,
        onJoinClick = {
            viewModel.dismissJoinError()
            joinOpen = true
        },
    )
    if (joinOpen) {
        JoinClassDialog(
            joining = viewModel.joining,
            error = viewModel.joinError,
            onJoin = { code ->
                viewModel.join(code) { joined ->
                    joinOpen = false
                    scope.launch { snackbar.showSnackbar("Đã tham gia lớp ${joined.name}") }
                }
            },
            onDismiss = { joinOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(user: User, state: Remote<HomeData>, onRefresh: () -> Unit, onOpenExercises: () -> Unit, onJoinClick: () -> Unit) {
    val data = state.data
    when {
        data == null && state.error != null -> ErrorView(state.error, onRetry = onRefresh)
        data == null -> LoadingView()
        else -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            val missing = "Chưa có dữ liệu"
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chào ${user.fullName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        StatusLabel(user.accountType, if (user.accountType == "VIP") Amber else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Lớp học", data.classes.size.toString(), "Lớp đã tham gia", Icons.Filled.AccountBox, Sky, Modifier.weight(1f))
                            StatCard("Bài tập chưa làm", data.pendingExercises.toString(), "Còn hạn nộp", Icons.AutoMirrored.Filled.List, Amber, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Chuỗi ngày học", data.streak?.let { "$it ngày" } ?: "—", if (data.streak == null) missing else "Ngày học liên tiếp", Icons.Filled.DateRange, Rose, Modifier.weight(1f))
                            StatCard("Điểm hoạt động", data.points?.toString() ?: "—", if (data.points == null) missing else "+20 mỗi bài nộp, +1 mỗi câu đúng", Icons.Filled.Star, Emerald, Modifier.weight(1f))
                        }
                    }
                }
                val pending = data.pendingExercises
                if (pending > 0) {
                    item {
                        FilledTonalButton(onClick = onOpenExercises, modifier = Modifier.fillMaxWidth()) {
                            Text("Làm bài tập ($pending bài đang chờ)")
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Lớp của tôi", Modifier.weight(1f))
                        TextButton(onClick = onJoinClick) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Tham gia lớp")
                        }
                    }
                }
                if (data.classes.isEmpty()) {
                    item { EmptyView("Bạn chưa tham gia lớp nào", "Nhập mã lớp gồm 6 ký tự do giảng viên gửi.") }
                } else {
                    items(data.classes, key = { it.id }) { item -> ClassRow(item) }
                }
                if (state.error != null) {
                    item { Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun ClassRow(item: ClassInfo) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Giảng viên: ${item.teacherName ?: "chưa lấy được tên"} · ${item.studentCount} sinh viên",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun JoinClassDialog(joining: Boolean, error: String?, onJoin: (String) -> Unit, onDismiss: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!joining) onDismiss() },
        title = { Text("Tham gia lớp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nhập mã lớp gồm 6 ký tự do giảng viên gửi. Không phân biệt chữ hoa, chữ thường.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(12) },
                    label = { Text("Mã lớp") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onJoin(code) }, enabled = !joining) { Text(if (joining) "Đang tham gia…" else "Tham gia") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !joining) { Text("Huỷ") } },
    )
}
