package com.z22zzw.dailycheckin.di

import androidx.room.Room
import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.db.AppDatabase
import com.z22zzw.dailycheckin.data.repository.AiContextBuilder
import com.z22zzw.dailycheckin.data.repository.AiRepository
import com.z22zzw.dailycheckin.data.repository.CheckInRepository
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import com.z22zzw.dailycheckin.data.repository.ProjectRepository
import com.z22zzw.dailycheckin.ui.ai.AiChatViewModel
import com.z22zzw.dailycheckin.ui.checkin.CheckInViewModel
import com.z22zzw.dailycheckin.ui.note.NoteViewModel
import com.z22zzw.dailycheckin.ui.project.ProjectViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "daily_checkin.db"
        ).build()
    }

    single { get<AppDatabase>().habitDao() }
    single { get<AppDatabase>().checkInRecordDao() }
    single { get<AppDatabase>().projectDao() }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().aiMessageDao() }
}

val networkModule = module {
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }
}

val repositoryModule = module {
    single { CheckInRepository(get(), get()) }
    single { ProjectRepository(get(), get()) }
    single { NoteRepository(get()) }
    single { AiContextBuilder(get(), get(), get(), get(), get()) }
    single { AiRepository(get(), get(), get(), getApiKeyProvider()) }
}

val viewModelModule = module {
    viewModel { CheckInViewModel(get()) }
    viewModel { ProjectViewModel(get()) }
    viewModel { NoteViewModel(get()) }
    viewModel { AiChatViewModel(get(), get()) }
}

import android.content.Context

private var _apiKey: String? = null

private fun getApiKey(): String = _apiKey ?: ""

fun setApiKey(key: String) { _apiKey = key }

fun persistApiKey(context: Context, key: String) {
    _apiKey = key
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit().putString("deepseek_api_key", key).apply()
}

private fun getApiKeyProvider(): () -> String? = { _apiKey }
