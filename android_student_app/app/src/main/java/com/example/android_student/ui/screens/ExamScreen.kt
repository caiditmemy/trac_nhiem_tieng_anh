package com.example.android_student.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_student.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    examViewModel: ExamViewModel,
    onSubmitSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val questions = examViewModel.questions
    val subject = examViewModel.currentSubject
    var currentIndex by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val minutes = examViewModel.remainingSeconds / 60
    val seconds = examViewModel.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Làm bài thi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (examViewModel.isSimulationMode) {
                        Text(
                            text = "⏱ $timeFormatted",
                            fontWeight = FontWeight.Bold,
                            color = if (examViewModel.remainingSeconds < 120) Color.Red else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp),
                            fontSize = 17.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Text("Câu trước")
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Nộp bài")
                    }

                    Button(
                        onClick = { if (currentIndex < questions.size - 1) currentIndex++ },
                        enabled = currentIndex < questions.size - 1
                    ) {
                        Text("Câu sau")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Không có câu hỏi nào cho môn học này!")
            }
        } else {
            val q = questions[currentIndex]
            val currentChosen = examViewModel.selectedAnswers[currentIndex]
            val isAnswered = currentChosen != null
            val isPractice = !examViewModel.isSimulationMode

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Câu ${currentIndex + 1} / ${questions.size}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = q.questionText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Danh sách 4 đáp án A, B, C, D
                listOf(
                    "A" to q.optionA,
                    "B" to q.optionB,
                    "C" to q.optionC,
                    "D" to q.optionD
                ).forEach { (optKey, optText) ->
                    val isSelected = currentChosen == optKey
                    val isCorrect = (q.correctAnswer ?: "A").equals(optKey, ignoreCase = true)

                    val containerColor = when {
                        !isPractice -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        isAnswered && isCorrect -> Color(0xFFD4EDDA) // Xanh lá khi làm đúng
                        isAnswered && isSelected && !isCorrect -> Color(0xFFF8D7DA) // Đỏ khi chọn sai
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                if (!isPractice || !isAnswered) {
                                    examViewModel.selectedAnswers[currentIndex] = optKey
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!isPractice || !isAnswered) {
                                        examViewModel.selectedAnswers[currentIndex] = optKey
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$optKey. $optText", fontSize = 15.sp)
                        }
                    }
                }

                // Hiện giải thích tức thì ở chế độ Luyện tập tự do
                if (isPractice && isAnswered) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("💡 Giải thích chi tiết:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(q.explanation ?: "Đáp án đúng là ${q.correctAnswer}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Xác nhận nộp bài") },
                text = { Text("Bạn đã làm ${examViewModel.selectedAnswers.size}/${questions.size} câu. Bạn có muốn nộp bài ngay?") },
                confirmButton = {
                    Button(onClick = {
                        showConfirmDialog = false
                        examViewModel.submitExam(onSubmitSuccess)
                    }) {
                        Text("Nộp bài")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showConfirmDialog = false }) {
                        Text("Làm tiếp")
                    }
                }
            )
        }
    }
}