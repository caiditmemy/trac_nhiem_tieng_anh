package com.example.android_student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android_student.ui.screens.ExamScreen
import com.example.android_student.ui.screens.HistoryScreen
import com.example.android_student.ui.screens.LoginScreen
import com.example.android_student.ui.screens.ResultScreen
import com.example.android_student.ui.screens.SubjectSelectionScreen
import com.example.android_student.viewmodel.AuthViewModel
import com.example.android_student.viewmodel.ExamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val examViewModel: ExamViewModel = viewModel()

                NavHost(navController = navController, startDestination = "login") {
                    // M-01: Đăng nhập
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // M-02: Trang chủ chọn môn
                    composable("home") {
                        SubjectSelectionScreen(
                            authViewModel = authViewModel,
                            examViewModel = examViewModel,
                            onStartExam = { navController.navigate("exam") },
                            onOpenHistory = { navController.navigate("history") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    // M-03: Làm bài trắc nghiệm
                    composable("exam") {
                        ExamScreen(
                            examViewModel = examViewModel,
                            onSubmitSuccess = {
                                navController.navigate("result") {
                                    popUpTo("exam") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // M-04: Kết quả bài thi
                    composable("result") {
                        ResultScreen(
                            examViewModel = examViewModel,
                            onReview = { navController.navigate("exam") },
                            onBackHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    // M-05: Lịch sử làm bài
                    composable("history") {
                        HistoryScreen(
                            examViewModel = examViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}