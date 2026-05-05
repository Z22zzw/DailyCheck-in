package com.z22zzw.dailycheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.z22zzw.dailycheckin.ui.navigation.AppNavigation
import com.z22zzw.dailycheckin.ui.theme.DailyCheckInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyCheckInTheme {
                AppNavigation()
            }
        }
    }
}
