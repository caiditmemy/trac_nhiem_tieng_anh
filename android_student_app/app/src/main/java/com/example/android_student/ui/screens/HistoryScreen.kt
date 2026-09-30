package com.example.android_student.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_student.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(examViewModel: ExamViewModel, onBack: () -> Unit) {
    val histories by examViewModel.historyFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử / Thống kê thi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (histories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Chưa có lịch sử làm bài nào được lưu!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(histories) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.subjectName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Text("Chế độ: ${item.mode}", fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.score} đ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (item.isPassed) "Đỗ" else "Trượt",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                                Text("Đúng ${item.correctCount}/${item.totalCount}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}