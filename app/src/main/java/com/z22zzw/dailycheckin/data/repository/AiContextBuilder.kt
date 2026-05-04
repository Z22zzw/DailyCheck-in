package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.firstOrNull

class AiContextBuilder(
    private val checkInRecordDao: CheckInRecordDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val aiMessageDao: AiMessageDao
) {
    suspend fun buildContext(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("分析") || lower.contains("建议") || lower.contains("计划") -> buildFullContext()
            lower.contains("习惯") || lower.contains("打卡") || lower.contains("坚持") -> buildCheckInContext()
            lower.contains("项目") || lower.contains("进度") || lower.contains("截止") -> buildProjectContext()
            else -> buildChatHistoryContext()
        }
    }

    private suspend fun buildFullContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 用户个人画像 ===")
        val profile = noteDao.getByType("profile").firstOrNull()
        if (profile != null && profile.isNotEmpty()) {
            sb.appendLine(profile[0].content)
        }
        sb.appendLine()
        sb.appendLine(buildCheckInStats())
        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildCheckInContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 用户习惯数据 ===")
        sb.appendLine(buildCheckInStats())
        return sb.toString()
    }

    private suspend fun buildProjectContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 项目进度数据 ===")
        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildChatHistoryContext(): String {
        val messages = aiMessageDao.getRecent(20).firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.appendLine("=== 最近对话 ===")
        messages.forEach { msg ->
            sb.appendLine("[${msg.role}]: ${msg.content.take(200)}")
        }
        return sb.toString()
    }

    suspend fun buildReportContext(): String {
        val sb = StringBuilder()
        sb.appendLine("请基于以下数据生成本周总结：")
        sb.appendLine()
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        sb.appendLine("--- 打卡数据 (${weekAgo.format(fmt)} ~ ${today.format(fmt)}) ---")
        val records = checkInRecordDao.getRecordsInRange(weekAgo.format(fmt), today.format(fmt))
        sb.appendLine("本周共打卡 ${records.size} 次")
        sb.appendLine()
        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildCheckInStats(): String {
        val sb = StringBuilder()
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val records = checkInRecordDao.getRecordsInRange(thirtyDaysAgo.format(fmt), today.format(fmt))
        sb.appendLine("最近30天打卡 ${records.size} 次")
        val byDate = records.groupBy { it.date }
        sb.appendLine("打卡天数: ${byDate.size}/30")
        return sb.toString()
    }

    private suspend fun buildProjectStats(): String {
        val sb = StringBuilder()
        val projects = projectDao.getActiveProjects().firstOrNull() ?: emptyList()
        projects.forEach { project ->
            val total = taskDao.countByProject(project.id)
            val done = taskDao.countDoneByProject(project.id)
            val pct = if (total > 0) (done * 100 / total) else 0
            val deadline = project.deadline?.let { d ->
                val remaining = (d - System.currentTimeMillis()) / (24 * 3600 * 1000)
                " (剩余${remaining}天)"
            } ?: ""
            sb.appendLine("- ${project.name}: ${done}/${total} ($pct%)${deadline}")
        }
        return sb.toString()
    }
}
