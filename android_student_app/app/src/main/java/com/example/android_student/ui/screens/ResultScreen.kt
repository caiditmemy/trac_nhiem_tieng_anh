package com.example.android_student.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_student.viewmodel.ExamViewModel

@Composable
fun ResultScreen(
    examViewModel: ExamViewModel,
    onReview: () -> Unit,
    onBackHome: () -> Unit
) {
    val total = examViewModel.questions.size
    val correct = examViewModel.lastCorrectCount
    val score = examViewModel.lastScore
    val isPassed = examViewModel.isPassed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("KẾT QUẢ BÀI THI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (isPassed) "KẾT QUẢ: ĐẠT" else "KẾT QUẢ: CHƯA ĐẠT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text("Số câu đúng: $correct/$total", fontWeight = FontWeight.SemiBold)
                    Text("Số câu sai: ${total - correct}/$total", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedButton(
            onClick = onReview,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Xem lại bài làm")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBackHome,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Về trang chủ")
        }
    }
}