package com.z22zzw.dailycheckin.ui.note

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val filterTypes = listOf("all" to "全部", "manual" to "我的", "ai_report" to "AI报告")

    // 编辑状态
    if (uiState.editingNoteId != null) {
        val editingNote = if (uiState.editingNoteId == 0L || uiState.editingNoteId == null) null
            else uiState.notes.find { it.id == uiState.editingNoteId }
        NoteEditScreen(
            note = editingNote,
            onBack = { viewModel.dismissEdit() }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth()) {
                filterTypes.forEach { (type, label) ->
                    FilterChip(
                        selected = uiState.filterType == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue500)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    uiState.notes
                        .filter { if (uiState.filterType == "all") true else it.type == uiState.filterType },
                    key = { it.id }
                ) { note ->
                    val leftBarColor = when (note.type) {
                        "ai_report" -> Green500
                        "profile" -> Purple500
                        else -> Gray400
                    }
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .combinedClickable(
                                onClick = { viewModel.startEditNote(note) },
                                onLongClick = { viewModel.showDeleteConfirm(note) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row {
                            Spacer(Modifier.width(3.dp).height(60.dp).background(leftBarColor))
                            Column(Modifier.padding(14.dp)) {
                                if (note.type != "manual") {
                                    val (bg, fg) = when (note.type) {
                                        "ai_report" -> Green50 to Green500
                                        "profile" -> Purple50 to Purple500
                                        else -> Green50 to Green500
                                    }
                                    Text(
                                        when (note.type) {
                                            "ai_report" -> "AI 报告"
                                            "profile" -> "个人画像"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fg,
                                        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                                Text(
                                    note.content.take(80) + if (note.content.length > 80) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray400,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (uiState.notes.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("还没有笔记，点击右下角 + 新建", color = Gray400, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // FAB 新建笔记
        FloatingActionButton(
            onClick = { viewModel.startNewNote() },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Blue500
        ) {
            Text("+", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }

    // 删除确认
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除笔记") },
            text = { Text("确定删除「${uiState.showDeleteConfirm!!.title}」吗？") },
            confirmButton = {
                Button(onClick = { viewModel.deleteNote(uiState.showDeleteConfirm!!.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("删除")
                }
            },
            dismissButton = { OutlinedButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("取消") } }
        )
    }
}
