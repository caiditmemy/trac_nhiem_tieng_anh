package vn.edu.qnu.st.nambede.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.BuildConfig
import vn.edu.qnu.st.nambede.R
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.LoginRequest
import vn.edu.qnu.st.nambede.data.SessionStore
import vn.edu.qnu.st.nambede.data.errorText

class LoginViewModel(private val client: ApiClient, private val session: SessionStore) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun login() {
        if (email.isBlank() || password.isEmpty()) {
            error = "Nhập email và mật khẩu."
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val response = client.call { login(LoginRequest(email.trim(), password)) }
                if (response.user.role == "STUDENT") {
                    session.save(response.accessToken, response.user)
                } else {
                    error = "Ứng dụng di động dành cho sinh viên. Giảng viên và quản trị viên dùng trang web."
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error = errorText(failure, mapOf(401 to "Email hoặc mật khẩu không đúng.", 403 to "Tài khoản đã bị khóa. Liên hệ quản trị viên."))
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun LoginRoute(container: AppContainer) {
    val viewModel = viewModel { LoginViewModel(container.client, container.session) }
    LoginContent(
        email = viewModel.email,
        password = viewModel.password,
        loading = viewModel.loading,
        error = viewModel.error,
        onEmailChange = { viewModel.email = it },
        onPasswordChange = { viewModel.password = it },
        onSubmit = viewModel::login,
    )
}

/** Sign in with the same account as on the web; the app only talks to the API Gateway. */
@Composable
fun LoginContent(
    email: String,
    password: String,
    loading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    gatewayUrl: String = BuildConfig.API_BASE_URL,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.qnu_logo), contentDescription = "Huy hiệu Trường Đại học Quy Nhơn", modifier = Modifier.size(104.dp))
            Spacer(Modifier.height(16.dp))
            Text("NamBeDe", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Trường Đại học Quy Nhơn", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedCard(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Đăng nhập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Dùng tài khoản sinh viên như trên trang web.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Mật khẩu") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Ẩn" else "Hiện") } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(if (loading) "Đang đăng nhập…" else "Đăng nhập")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Chưa có tài khoản? Sinh viên đăng ký trên trang web NamBeDe.\nMáy chủ: $gatewayUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
