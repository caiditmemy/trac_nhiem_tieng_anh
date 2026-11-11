package vn.edu.qnu.st.nambede.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.ApiException
import vn.edu.qnu.st.nambede.data.ChangePasswordRequest
import vn.edu.qnu.st.nambede.data.SessionStore
import vn.edu.qnu.st.nambede.data.UpdateNameRequest
import vn.edu.qnu.st.nambede.data.User
import vn.edu.qnu.st.nambede.data.errorText
import vn.edu.qnu.st.nambede.ui.LocalSnackbar
import vn.edu.qnu.st.nambede.ui.common.StatusLabel
import vn.edu.qnu.st.nambede.ui.theme.Amber

/** Own account through Auth Service: PUT /users/me and /users/me/password, like the web's "Tài khoản của tôi". */
class ProfileViewModel(private val client: ApiClient, private val session: SessionStore) : ViewModel() {
    var savingName by mutableStateOf(false)
        private set
    var nameError by mutableStateOf<String?>(null)
        private set
    var savingPassword by mutableStateOf(false)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set

    fun rename(fullName: String, onDone: () -> Unit) {
        if (fullName.isBlank()) {
            nameError = "Nhập họ và tên."
            return
        }
        savingName = true
        nameError = null
        viewModelScope.launch {
            try {
                session.update(client.call { updateMe(UpdateNameRequest(fullName.trim())) })
                onDone()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                nameError = errorText(failure)
            } finally {
                savingName = false
            }
        }
    }

    fun changePassword(current: String, next: String, confirm: String, onDone: () -> Unit) {
        val problem = passwordProblem(current, next, confirm)
        if (problem != null) {
            passwordError = problem
            return
        }
        savingPassword = true
        passwordError = null
        viewModelScope.launch {
            try {
                client.call { changePassword(ChangePasswordRequest(current, next)) }
                onDone()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                val reused = failure is ApiException && failure.status == 400 && failure.message.orEmpty().contains("different", ignoreCase = true)
                passwordError = if (reused) "Mật khẩu mới phải khác mật khẩu hiện tại." else errorText(failure, mapOf(403 to "Mật khẩu hiện tại không đúng."))
            } finally {
                savingPassword = false
            }
        }
    }

    fun logout() = session.clear()
}

/** Same checks as the web form, before asking Auth Service. */
fun passwordProblem(current: String, next: String, confirm: String): String? = when {
    current.isEmpty() -> "Nhập mật khẩu hiện tại."
    next.length < 6 -> "Mật khẩu mới phải có ít nhất 6 ký tự."
    next != confirm -> "Nhập lại mật khẩu mới chưa khớp."
    next == current -> "Mật khẩu mới phải khác mật khẩu hiện tại."
    else -> null
}

/** "Nguyễn Văn An" gives "VA", like the web avatars. */
fun initialsOf(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.takeLast(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
fun ProfileRoute(container: AppContainer, user: User) {
    val viewModel = viewModel { ProfileViewModel(container.client, container.session) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(user.fullName) { mutableStateOf(user.fullName) }
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    ProfileContent(
        user = user,
        name = name,
        onNameChange = { name = it.take(255) },
        savingName = viewModel.savingName,
        nameError = viewModel.nameError,
        onSaveName = { viewModel.rename(name) { scope.launch { snackbar.showSnackbar("Đã lưu họ và tên") } } },
        current = current,
        next = next,
        confirm = confirm,
        onCurrentChange = { current = it },
        onNextChange = { next = it.take(72) },
        onConfirmChange = { confirm = it.take(72) },
        savingPassword = viewModel.savingPassword,
        passwordError = viewModel.passwordError,
        onChangePassword = {
            viewModel.changePassword(current, next, confirm) {
                current = ""
                next = ""
                confirm = ""
                scope.launch { snackbar.showSnackbar("Đã đổi mật khẩu. Lần đăng nhập sau dùng mật khẩu mới.") }
            }
        },
        onLogout = { confirmLogout = true },
    )

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Đăng xuất?") },
            text = { Text("Bạn sẽ cần đăng nhập lại để tiếp tục học.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                }) { Text("Đăng xuất") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Huỷ") } },
        )
    }
}

@Composable
fun ProfileContent(
    user: User,
    name: String,
    onNameChange: (String) -> Unit,
    savingName: Boolean,
    nameError: String?,
    onSaveName: () -> Unit,
    current: String,
    next: String,
    confirm: String,
    onCurrentChange: (String) -> Unit,
    onNextChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    savingPassword: Boolean,
    passwordError: String?,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(56.dp)) {
                    Text(initialsOf(user.fullName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusLabel("Sinh viên", MaterialTheme.colorScheme.primary)
                        StatusLabel(user.accountType, if (user.accountType == "VIP") Amber else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Email, vai trò và gói tài khoản do quản trị viên quản lý.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Họ và tên", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = name, onValueChange = onNameChange, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (nameError != null) Text(nameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onSaveName, enabled = !savingName && name.trim() != user.fullName) { Text(if (savingName) "Đang lưu…" else "Lưu họ và tên") }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Đổi mật khẩu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Mật khẩu mới có ít nhất 6 ký tự. Phiên đăng nhập hiện tại vẫn giữ nguyên.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PasswordField("Mật khẩu hiện tại", current, onCurrentChange)
                PasswordField("Mật khẩu mới", next, onNextChange)
                PasswordField("Nhập lại mật khẩu mới", confirm, onConfirmChange)
                if (passwordError != null) Text(passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onChangePassword, enabled = !savingPassword) { Text(if (savingPassword) "Đang đổi…" else "Đổi mật khẩu") }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Đăng xuất")
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}
