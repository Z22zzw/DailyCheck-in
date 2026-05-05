package com.z22zzw.dailycheckin.data.repository

import com.google.gson.Gson
import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.Message
import com.z22zzw.dailycheckin.data.api.dto.StreamChunk
import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader

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

    fun sendMessageStream(content: String): Flow<String> = callbackFlow {
        val key = apiKeyProvider()
        if (key == null) {
            close(Exception("未配置 API Key"))
            return@callbackFlow
        }
        val context = contextBuilder.buildContext(content)

        val messages = listOf(
            Message("system", "$SYSTEM_PROMPT\n\n当前数据：\n$context"),
            Message("user", content)
        )
        val request = DeepSeekRequest(model = modelProvider(), messages = messages, stream = true)

        try {
            val response = api.chatCompletionStream(request)
            if (!response.isSuccessful) {
                close(Exception("API 错误: ${response.code()}"))
                return@callbackFlow
            }

            val body = response.body() ?: run {
                close(Exception("空响应"))
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var fullReply = ""

            reader.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val json = line.removePrefix("data: ")
                    if (json == "[DONE]") return@forEachLine
                    try {
                        val chunk = Gson().fromJson(json, StreamChunk::class.java)
                        val delta = chunk.choices?.firstOrNull()?.delta?.content ?: ""
                        if (delta.isNotEmpty()) {
                            fullReply += delta
                            trySend(delta)
                        }
                    } catch (_: Exception) { }
                }
            }

            // 保存到数据库
            aiMessageDao.insert(AiMessageEntity(role = "user", content = content))
            aiMessageDao.insert(AiMessageEntity(role = "assistant", content = fullReply))
            close()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose()
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
