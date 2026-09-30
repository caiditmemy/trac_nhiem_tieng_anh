package com.example.android_student.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_student.data.model.Subject
import com.example.android_student.viewmodel.AuthViewModel
import com.example.android_student.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSelectionScreen(
    authViewModel: AuthViewModel,
    examViewModel: ExamViewModel,
    onStartExam: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val token = authViewModel.loginState?.access ?: ""
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var showTimeSelectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        examViewModel.loadSubjects(token)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chào, ${authViewModel.loginState?.fullName ?: "Sinh viên"}", fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Lịch sử & Thống kê")
                    }
                    IconButton(onClick = { authViewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Đăng xuất")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Chọn môn ôn tập trắc nghiệm:", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(examViewModel.subjects) { subject ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clickable { selectedSubject = subject },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = subject.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            LinearProgressIndicator(
                                progress = { 0.4f }, // Tiến độ minh họa (hoặc gắn giá trị thật theo Room)
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Hộp thoại chọn chế độ làm bài
        selectedSubject?.let { sub ->
            AlertDialog(
                onDismissRequest = { selectedSubject = null },
                title = { Text(sub.name, fontWeight = FontWeight.Bold) },
                text = { Text("Vui lòng chọn hình thức bạn muốn thực hiện:") },
                confirmButton = {
                    Button(onClick = { showTimeSelectDialog = true }) {
                        Text("Thi thử (50 câu)")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        val chosen = selectedSubject!!
                        selectedSubject = null
                        examViewModel.startExam(chosen, token, isSimulation = false, onLoaded = onStartExam)
                    }) {
                        Text("Luyện tập tự do")
                    }
                }
            )
        }

        // Hộp thoại phụ chọn thời gian thi bấm giờ (15 phút hoặc 30 phút)
        if (showTimeSelectDialog && selectedSubject != null) {
            AlertDialog(
                onDismissRequest = { showTimeSelectDialog = false },
                title = { Text("Chọn thời gian thi bấm giờ") },
                text = { Text("Hệ thống sẽ bốc ngẫu nhiên 50 câu hỏi. Hết thời gian sẽ tự động nộp bài.") },
                confirmButton = {
                    Button(onClick = {
                        val chosen = selectedSubject!!
                        selectedSubject = null
                        showTimeSelectDialog = false
                        examViewModel.startExam(chosen, token, isSimulation = true, durationMinutes = 30, onLoaded = onStartExam)
                    }) {
                        Text("30 phút")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        val chosen = selectedSubject!!
                        selectedSubject = null
                        showTimeSelectDialog = false
                        examViewModel.startExam(chosen, token, isSimulation = true, durationMinutes = 15, onLoaded = onStartExam)
                    }) {
                        Text("15 phút")
                    }
                }
            )
        }
    }
}