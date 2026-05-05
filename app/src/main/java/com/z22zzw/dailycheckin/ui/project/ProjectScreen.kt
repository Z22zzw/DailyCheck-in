package com.z22zzw.dailycheckin.ui.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    onProjectClick: (Long) -> Unit = {},
    viewModel: ProjectViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("我的项目", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = { viewModel.showCreateDialog() }, shape = RoundedCornerShape(12.dp)) {
                Text("+ 新建")
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Text(
                "${uiState.projects.size} 个进行中",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.projects, key = { it.project.id }) { item ->
                    val isExpanded = uiState.expandedProjectId == item.project.id

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.toggleExpand(item.project.id) },
                                onLongClick = {
                                    // 弹出简单菜单：长按选编辑或删除
                                    viewModel.showEditDialog(item.project)
                                }
                            ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.project.name, style = MaterialTheme.typography.titleMedium)
                                    if (item.project.deadline != null) {
                                        val daysLeft = maxOf(0L, (item.project.deadline - System.currentTimeMillis()) / (24 * 3600 * 1000))
                                        Text(
                                            if (daysLeft > 0) "${daysLeft}天后截止" else "已到期",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (daysLeft > 0) Orange500 else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.background(Orange50, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.showDeleteConfirm(item.project) }) {
                                    Text("🗑", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            val progress = if (item.totalTasks > 0) item.doneTasks.toFloat() / item.totalTasks else 0f
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.doneTasks}/${item.totalTasks} 完成", style = MaterialTheme.typography.bodySmall, color = Gray400)
                                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Green500)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (progress < 0.5f) Orange500 else Green500
                            )

                            if (isExpanded) {
                                Spacer(Modifier.height(12.dp))
                                item.tasks.forEach { task ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = task.status == "done",
                                                onCheckedChange = { viewModel.toggleTask(task) }
                                            )
                                            Text(task.title, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                                            Text("✕", style = MaterialTheme.typography.labelSmall, color = Gray400)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = uiState.newTaskTitle,
                                        onValueChange = { viewModel.setNewTaskTitle(it) },
                                        placeholder = { Text("新任务...") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = {
                                        if (uiState.newTaskTitle.isNotBlank())
                                            viewModel.addTask(item.project.id, uiState.newTaskTitle)
                                    }) { Text("添加") }
                                }
                            }
                        }
                    }
                }

                if (uiState.projects.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("还没有项目，点击上方 + 新建", color = Gray400, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // 新建项目弹窗
    if (uiState.showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateDialog() },
            title = { Text("新建项目") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("项目名称") }, singleLine = true)
                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("截止日期 (可选)") }, placeholder = { Text("如: 2026-12-31") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val dl = try { java.time.LocalDate.parse(deadline).toEpochDay() * 24 * 3600 * 1000 } catch (_: Exception) { null }
                    viewModel.createProject(name, dl)
                }) { Text("创建") }
            },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissCreateDialog() }) { Text("取消") } }
        )
    }

    // 编辑项目弹窗
    uiState.showEditDialog?.let { project ->
        var editName by remember(project.id) { mutableStateOf(project.name) }
        var editDeadline by remember(project.id) { mutableStateOf(project.deadline?.let { java.time.LocalDate.ofEpochDay(it / (24 * 3600 * 1000)).toString() } ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditDialog() },
            title = { Text("编辑/删除项目") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("名称") }, singleLine = true)
                    OutlinedTextField(value = editDeadline, onValueChange = { editDeadline = it }, label = { Text("截止日期") })
                    OutlinedButton(
                        onClick = { viewModel.dismissEditDialog(); viewModel.showDeleteConfirm(project) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("删除项目") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val dl = try { java.time.LocalDate.parse(editDeadline).toEpochDay() * 24 * 3600 * 1000 } catch (_: Exception) { project.deadline }
                    viewModel.updateProject(editName, dl)
                }) { Text("保存") }
            },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissEditDialog() }) { Text("取消") } }
        )
    }

    // 删除确认弹窗
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除项目") },
            text = { Text("确定删除「${uiState.showDeleteConfirm!!.name}」吗？所有子任务也会被删除。") },
            confirmButton = {
                Button(onClick = { viewModel.deleteProject() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("删除")
                }
            },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("取消") } }
        )
    }
}
