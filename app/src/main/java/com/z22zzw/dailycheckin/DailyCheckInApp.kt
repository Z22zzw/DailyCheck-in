package com.z22zzw.dailycheckin

import android.app.Application
import com.z22zzw.dailycheckin.di.databaseModule
import com.z22zzw.dailycheckin.di.networkModule
import com.z22zzw.dailycheckin.di.repositoryModule
import com.z22zzw.dailycheckin.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DailyCheckInApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DailyCheckInApp)
            modules(databaseModule, networkModule, repositoryModule, viewModelModule)
        }
    }
}
