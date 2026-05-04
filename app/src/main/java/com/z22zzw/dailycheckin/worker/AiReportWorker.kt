package com.z22zzw.dailycheckin.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.Message
import com.z22zzw.dailycheckin.data.db.AppDatabase
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.repository.AiContextBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AiReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString("report_type") ?: "每日"
        val db = androidx.room.Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "daily_checkin.db"
        ).build()

        return try {
            val contextBuilder = AiContextBuilder(
                db.checkInRecordDao(), db.projectDao(), db.taskDao(),
                db.noteDao(), db.aiMessageDao()
            )
            val api = buildApi()

            val reportContext = contextBuilder.buildReportContext()
            val request = DeepSeekRequest(
                messages = listOf(
                    Message("system", "你是用户的效率助手。基于数据生成总结。\n$reportContext"),
                    Message("user", "请生成${reportType}总结报告")
                )
            )

            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                db.noteDao().insert(NoteEntity(title = "${reportType}报告 · $today", content = content, type = "ai_report"))
                db.close()
                showNotification("AI ${reportType}报告已生成", content.take(100) + "...")
                Result.success()
            } else {
                db.close()
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try { db.close() } catch (_: Exception) {}
            Result.retry()
        }
    }

    private fun buildApi(): DeepSeekApi {
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("deepseek_api_key", "") ?: ""
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(OkHttpClient.Builder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer $apiKey").build())
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(DeepSeekApi::class.java)
    }

    private fun showNotification(title: String, content: String) {
        val channelId = "ai_reports"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AI 报告", NotificationManager.IMPORTANCE_DEFAULT)
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        }
        NotificationManagerCompat.from(applicationContext).notify(1001,
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
        )
    }
}
