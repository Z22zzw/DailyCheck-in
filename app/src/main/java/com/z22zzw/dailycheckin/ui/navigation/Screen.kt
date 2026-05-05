package com.z22zzw.dailycheckin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Note
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object CheckIn : Screen("checkin", "打卡", Icons.Default.CheckCircle)
    data object Project : Screen("project", "项目", Icons.Default.Folder)
    data object Note : Screen("note", "随笔", Icons.Default.Note)
    data object Ai : Screen("ai", "AI", Icons.Default.Chat)
}

val bottomNavItems = listOf(Screen.CheckIn, Screen.Project, Screen.Note, Screen.Ai)
