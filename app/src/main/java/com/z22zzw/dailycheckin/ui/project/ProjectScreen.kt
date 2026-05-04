package com.z22zzw.dailycheckin.ui.project

import androidx.compose.foundation.background
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProjectScreen(
    onProjectClick: (Long) -> Unit = {},
    viewModel: ProjectViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("我的项目", style = MaterialTheme.typography.titleLarge)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Text(
                "${uiState.projects.size} 个进行中",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.projects, key = { it.project.id }) { item ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.project.name, style = MaterialTheme.typography.titleMedium)
                                if (item.project.deadline != null) {
                                    val daysLeft = (item.project.deadline - System.currentTimeMillis()) / (24 * 3600 * 1000)
                                    Text(
                                        "${daysLeft}天后截止",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Orange500,
                                        modifier = Modifier.background(Orange50, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
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

                            if (item.tasks.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                item.tasks.take(4).forEach { task ->
                                    Text(
                                        (if (task.status == "done") "✅ " else "⬜ ") + task.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.status == "done") Gray400 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
