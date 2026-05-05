package com.z22zzw.dailycheckin.ui.checkin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.Blue500
import com.z22zzw.dailycheckin.ui.theme.Gray400
import com.z22zzw.dailycheckin.ui.theme.Green500
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckInScreen(viewModel: CheckInViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("今日打卡", style = MaterialTheme.typography.titleLarge)
        Text(
            "${LocalDate.now()} · ${LocalDate.now().dayOfWeek}",
            style = MaterialTheme.typography.bodySmall,
            color = Gray400,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val done = uiState.habits.count { it.checkedInToday }
            val total = uiState.habits.size
            Text(
                "已完成 $done / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = if (done == total && total > 0) Green500 else Gray400,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.habits, key = { it.habit.id }) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!item.checkedInToday) viewModel.checkIn(item.habit.id)
                                },
                                onLongClick = {
                                    if (item.checkedInToday) viewModel.showUncheckConfirm(item.habit.id)
                                    else viewModel.showEditDialog(item.habit)
                                }
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.checkedInToday) Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.habit.name, style = MaterialTheme.typography.titleMedium)
                                Text("共 ${item.totalCount} 次", style = MaterialTheme.typography.bodySmall, color = Gray400)
                            }
                            if (item.checkedInToday) {
                                Text("✅ 已打卡", color = Green500, style = MaterialTheme.typography.bodySmall)
                            } else {
                                Button(
                                    onClick = { viewModel.checkIn(item.habit.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                                    shape = RoundedCornerShape(20.dp)
                                ) { Text("打卡") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+ 新建习惯") }
        }
    }

    // 新建对话框
    if (uiState.showAddDialog) {
        var habitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddDialog() },
            title = { Text("新建习惯") },
            text = {
                OutlinedTextField(value = habitName, onValueChange = { habitName = it }, label = { Text("习惯名称") }, singleLine = true)
            },
            confirmButton = { Button(onClick = { viewModel.addHabit(habitName) }) { Text("创建") } },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissAddDialog() }) { Text("取消") } }
        )
    }

    // 编辑对话框
    uiState.showEditDialog?.let { habit ->
        var editName by remember(habit.id) { mutableStateOf(habit.name) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditDialog() },
            title = { Text("编辑习惯") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("名称") }, singleLine = true)
                    OutlinedButton(
                        onClick = { viewModel.showDeleteConfirm(habit); viewModel.dismissEditDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("删除习惯") }
                }
            },
            confirmButton = { Button(onClick = { viewModel.updateHabit(editName) }) { Text("保存") } },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissEditDialog() }) { Text("取消") } }
        )
    }

    // 删除确认
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除习惯") },
            text = { Text("确定删除「${uiState.showDeleteConfirm!!.name}」吗？所有打卡记录也会被删除。") },
            confirmButton = {
                Button(onClick = { viewModel.deleteHabit() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("删除")
                }
            },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("取消") } }
        )
    }

    // 取消打卡确认
    if (uiState.showUncheckConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUncheckConfirm() },
            title = { Text("取消打卡") },
            text = { Text("确定取消今天的打卡记录吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.uncheckIn(uiState.showUncheckConfirm!!)
                    viewModel.dismissUncheckConfirm()
                }) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissUncheckConfirm() }) { Text("取消") }
            }
        )
    }
}
