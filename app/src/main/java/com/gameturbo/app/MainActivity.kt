package com.gameturbo.app

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gameturbo.app.profiles.PerformanceProfile
import com.gameturbo.app.profiles.ProfileManager
import com.gameturbo.app.service.GamingModeService
import com.gameturbo.app.ui.GameTurboNavigation
import com.gameturbo.app.ui.theme.GameTurboTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GameTurboApp

        requestNotificationPermission()
        requestDndPermission()

        setContent {
            GameTurboTheme {
                val thermalInfo by app.thermalManager.thermalInfo.collectAsState()
                val memoryInfo by app.memoryMonitor.memoryInfo.collectAsState()
                val isGamingActive by GamingModeService.isActive.collectAsState()
                val currentGame by GamingModeService.currentGame.collectAsState()
                var currentProfile by remember { mutableStateOf(ProfileManager.getCurrentProfile()) }

                LaunchedEffect(Unit) {
                    while (true) {
                        app.thermalManager.update()
                        app.memoryMonitor.update()
                        app.batteryMonitor.update(this@MainActivity)
                        delay(5000L)
                    }
                }

                GameTurboNavigation(
                    thermalInfo = thermalInfo,
                    memoryInfo = memoryInfo,
                    isGamingModeActive = isGamingActive,
                    currentGame = currentGame,
                    currentProfile = currentProfile,
                    gameRepository = app.gameRepository,
                    sessionRepository = app.sessionRepository,
                    onStartGamingMode = {
                        GamingModeService.start(this@MainActivity)
                    },
                    onStopGamingMode = {
                        GamingModeService.stop(this@MainActivity)
                    },
                    onProfileSelected = { profile ->
                        ProfileManager.setProfile(profile)
                        currentProfile = profile
                    }
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun requestDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }
}
