package com.z22zzw.dailycheckin.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.di.getApiConfig
import com.z22zzw.dailycheckin.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AiChatScreen(viewModel: AiChatViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    // 首次打开检查：如果没有 API Key 则自动弹出设置
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val (_, key, _) = getApiConfig(context)
        if (key.isBlank()) showSettings = true
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 助手", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Gray400)
            }
        }

        uiState.onboardingQuestion?.let { question ->
            Text(
                question,
                Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Blue50, RoundedCornerShape(12.dp)).padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Blue500
            )
        }

        uiState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(Modifier.weight(1f), state = listState) {
            items(uiState.messages) { message ->
                val isUser = message.role == "user"
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Text(
                        message.content,
                        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(12.dp)).background(if (isUser) Gray50 else Blue50).padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isUser) {
                        TextButton(onClick = { viewModel.saveMessageToNote(message.content) }) {
                            Text("存入笔记", style = MaterialTheme.typography.labelSmall, color = Gray400)
                        }
                    }
                }
            }
            if (uiState.isLoading) {
                item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.generateReport("本周") }, Modifier.weight(1f)) {
                Text("📊 生成周报", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { viewModel.generateReport("学习计划") }, Modifier.weight(1f)) {
                Text("📋 制定计划", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("问 DeepSeek 任何问题...") },
                modifier = Modifier.weight(1f), singleLine = false, maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" }
            }, colors = ButtonDefaults.buttonColors(containerColor = Blue500)) {
                Text("发送")
            }
        }
    }

    if (showSettings) {
        AiSettingsDialog(onDismiss = { showSettings = false })
    }
}
