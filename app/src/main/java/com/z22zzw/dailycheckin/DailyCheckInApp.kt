package com.z22zzw.dailycheckin

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.z22zzw.dailycheckin.di.databaseModule
import com.z22zzw.dailycheckin.di.networkModule
import com.z22zzw.dailycheckin.di.repositoryModule
import com.z22zzw.dailycheckin.di.viewModelModule
import com.z22zzw.dailycheckin.worker.AiReportWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class DailyCheckInApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DailyCheckInApp)
            modules(databaseModule, networkModule, repositoryModule, viewModelModule)
        }
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val key = prefs.getString("deepseek_api_key", "") ?: ""
        if (key.isNotBlank()) {
            com.z22zzw.dailycheckin.di.setApiKey(key)
        }
        scheduleAiReports()
    }

    private fun scheduleAiReports() {
        val dailyRequest = PeriodicWorkRequestBuilder<AiReportWorker>(1, TimeUnit.DAYS)
            .setInputData(
                androidx.work.Data.Builder()
                    .putString("report_type", "每日")
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_ai_report",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }
}
