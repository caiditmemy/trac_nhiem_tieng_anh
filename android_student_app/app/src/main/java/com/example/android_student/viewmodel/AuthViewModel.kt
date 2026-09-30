package com.example.android_student.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_student.data.api.RetrofitClient
import com.example.android_student.data.model.LoginRequest
import com.example.android_student.data.model.LoginResponse
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var loginState by mutableStateOf<LoginResponse?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun login(username: String, pass: String, onSuccess: () -> Unit) {
        if (username.isBlank() || pass.isBlank()) {
            errorMessage = "Vui lòng nhập tài khoản và mật khẩu"
            return
        }
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.login(LoginRequest(username.trim(), pass.trim()))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    if (body.role == "STUDENT") {
                        loginState = body
                        onSuccess()
                    } else {
                        errorMessage = "Tài khoản này là Quản trị/Giáo viên. Vui lòng đăng nhập trên Web!"
                    }
                } else {
                    errorMessage = "Sai tên đăng nhập hoặc mật khẩu!"
                }
            } catch (e: Exception) {
                errorMessage = "Không thể kết nối đến máy chủ Django! Hãy đảm bảo server đang chạy."
            } finally {
                isLoading = false
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        loginState = null
        onLogout()
    }
}