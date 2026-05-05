package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.Message
import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

class AiRepository(
    private val api: DeepSeekApi,
    private val aiMessageDao: AiMessageDao,
    private val contextBuilder: AiContextBuilder,
    private val apiKeyProvider: () -> String?,
    private val modelProvider: () -> String = { "deepseek-v4-pro" }
) {
    companion object {
        private const val SYSTEM_PROMPT = "你是用户的效率助手，根据提供的客观数据回答。没有数据时，直接说不知道，不要瞎猜。"
    }

    fun getMessages(): Flow<List<AiMessageEntity>> = aiMessageDao.getAllAsc()

    suspend fun sendMessage(content: String): Result<String> {
        val key = apiKeyProvider() ?: return Result.failure(Exception("未配置 API Key"))
        val context = contextBuilder.buildContext(content)

        val messages = mutableListOf<Message>()
        messages.add(Message("system", "$SYSTEM_PROMPT\n\n当前数据：\n$context"))
        messages.add(Message("user", content))

        try {
            val request = DeepSeekRequest(model = modelProvider(), messages = messages)
            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "AI 返回为空"
                aiMessageDao.insert(AiMessageEntity(role = "user", content = content))
                aiMessageDao.insert(AiMessageEntity(role = "assistant", content = reply))
                return Result.success(reply)
            } else {
                return Result.failure(Exception("API 错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun generateReport(reportType: String): Result<String> {
        val key = apiKeyProvider() ?: return Result.failure(Exception("未配置 API Key"))
        val context = contextBuilder.buildReportContext()

        try {
            val messages = listOf(
                Message("system", "$SYSTEM_PROMPT\n\n$context"),
                Message("user", "请生成${reportType}总结报告")
            )
            val request = DeepSeekRequest(model = modelProvider(), messages = messages)
            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                return Result.success(reply)
            } else {
                return Result.failure(Exception("API 错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
