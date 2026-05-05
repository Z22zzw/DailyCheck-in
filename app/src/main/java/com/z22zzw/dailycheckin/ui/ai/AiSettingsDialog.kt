package com.z22zzw.dailycheckin.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.di.getApiConfig
import com.z22zzw.dailycheckin.di.saveApiConfig

@Composable
fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (savedUrl, savedKey, savedModel) = remember { getApiConfig(context) }

    var url by remember { mutableStateOf(savedUrl) }
    var apiKey by remember { mutableStateOf(savedKey) }
    var model by remember { mutableStateOf(savedModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("API URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("deepseek-v4-pro[1m]") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                saveApiConfig(context, url, apiKey, model)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
