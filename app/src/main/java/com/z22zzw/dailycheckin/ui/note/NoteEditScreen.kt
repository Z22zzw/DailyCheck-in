package com.z22zzw.dailycheckin.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: NoteEntity? = null,
    onBack: () -> Unit,
    viewModel: NoteViewModel = koinViewModel()
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note != null) "编辑笔记" else "新建笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showPreview = !showPreview }) {
                        Text(if (showPreview) "编辑" else "预览")
                    }
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            viewModel.saveNote(title, content)
                            onBack()
                        }
                    }) { Text("保存") }
                }
            )
        }
    ) { padding ->
        if (showPreview) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(title.ifBlank { "无标题" }, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                MarkdownContent(content)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容 (Markdown)") },
                    placeholder = { Text("# 标题\n\n- 列表项\n- [ ] 待办事项\n\n正文内容...") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String) {
    val lines = markdown.split("\n")
    Column {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("- [ ] ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Spacer(Modifier.width(4.dp))
                    Text(line.removePrefix("- [ ] "))
                }
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Checkbox(checked = true, onCheckedChange = {})
                    Spacer(Modifier.width(4.dp))
                    Text(line.removePrefix("- [x] ").removePrefix("- [X] "))
                }
                line.startsWith("- ") -> Text(
                    "  • ${line.removePrefix("- ")}",
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                line.isBlank() -> Spacer(Modifier.height(8.dp))
                line.startsWith("---") -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                else -> Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
