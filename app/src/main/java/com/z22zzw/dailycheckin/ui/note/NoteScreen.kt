package com.z22zzw.dailycheckin.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteScreen(viewModel: NoteViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val filterTypes = listOf("all" to "全部", "manual" to "我的", "ai_report" to "AI报告")

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
                uiState.notes.filter { if (uiState.filterType == "all") true else it.type == uiState.filterType }
            ) { note ->
                val leftBarColor = when (note.type) {
                    "ai_report" -> Green500
                    "profile" -> Purple500
                    else -> Gray400
                }
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row {
                        Spacer(Modifier.width(3.dp).height(60.dp).background(leftBarColor))
                        Column(Modifier.padding(14.dp)) {
                            if (note.type != "manual") {
                                val badge = when (note.type) { "ai_report" -> "AI 报告"; "profile" -> "个人画像"; else -> "" }
                                val badgeColor = when (note.type) { "ai_report" -> Pair(Green50, Green500); "profile" -> Pair(Purple50, Purple500); else -> Pair(Green50, Green500) }
                                Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor.second,
                                    modifier = Modifier.background(badgeColor.first, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                            Text(note.content.take(60) + if (note.content.length > 60) "..." else "",
                                style = MaterialTheme.typography.bodySmall, color = Gray400, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
