package com.gameturbo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import com.gameturbo.app.MainActivity
import com.gameturbo.app.R
import com.gameturbo.app.data.GameRepository
import com.gameturbo.app.data.SessionHistoryRepository
import com.gameturbo.app.data.GameSession
import com.gameturbo.app.monitor.ThermalManager
import com.gameturbo.app.monitor.BatteryMonitor
import com.gameturbo.app.monitor.MemoryMonitor
import com.gameturbo.app.monitor.ProcessHelper
import com.gameturbo.app.profiles.PerformanceProfile
import com.gameturbo.app.profiles.ProfileManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamingModeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var thermalManager: ThermalManager
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var memoryMonitor: MemoryMonitor
    private lateinit var processHelper: ProcessHelper
    private lateinit var gameRepository: GameRepository
    private lateinit var sessionRepository: SessionHistoryRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private var gameDetectionJob: Job? = null
    private var processCleanupJob: Job? = null

    private var currentGamePackage: String? = null
    private var sessionStartTime: Long = 0L
    private var sessionStartTemp: Float = 0f
    private var sessionStartBattery: Int = 0

    companion object {
        private const val CHANNEL_ID = "gaming_mode_channel"
        private const val NOTIFICATION_ID = 1001

        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

        private val _currentGame = MutableStateFlow<String?>(null)
        val currentGame: StateFlow<String?> = _currentGame.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GamingModeService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GamingModeService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        thermalManager = ThermalManager(this)
        batteryMonitor = BatteryMonitor(this)
        memoryMonitor = MemoryMonitor(this)
        processHelper = ProcessHelper(this)
        gameRepository = GameRepository(this)

        val db = AppDatabase.getInstance(this)
        sessionRepository = SessionHistoryRepository(db.sessionDao())

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Gaming Mode Active"))
        _isActive.value = true
        sessionStartTime = System.currentTimeMillis()

        val initial = thermalManager.update()
        sessionStartTemp = initial.batteryTemperature
        sessionStartBattery = initial.batteryPercentage

        applyProfile(ProfileManager.getCurrentProfile())
        startMonitoring()
        startGameDetection()

        if (ProfileManager.getCurrentProfile().cleanupProcesses) {
            startPeriodicCleanup()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        releaseWakeLock()
        restoreSettings()
        recordSession()
        _isActive.value = false
        _currentGame.value = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.gaming_mode_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.gaming_mode_desc)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.game_turbo_active))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun applyProfile(profile: PerformanceProfile) {
        if (profile.keepScreenAwake) {
            acquireWakeLock()
        }
        if (profile.silenceNotifications) {
            tryDndMode(true)
        }
    }

    private fun restoreSettings() {
        releaseWakeLock()
        tryDndMode(false)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK,
                "GameTurbo:GamingModeWakeLock"
            ).apply {
                acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            wakeLock = null
        }
    }

    private fun tryDndMode(enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return
            if (!notificationManager.isNotificationPolicyAccessGranted) return

            if (enable) {
                val policy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                    NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                    NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                )
                notificationManager.setNotificationPolicy(policy)
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            } else {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        } catch (e: Exception) {
            // DND permission not granted
        }
    }

    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            while (true) {
                val profile = ProfileManager.getCurrentProfile()

                val thermalInfo = thermalManager.update()
                batteryMonitor.update(this@GamingModeService)
                memoryMonitor.update()

                updateNotification(thermalInfo.batteryTemperature, thermalInfo.batteryPercentage)

                if (profile.autoThermalSaver && thermalManager.shouldAutoSwitchToThermalSaver()) {
                    ProfileManager.setProfile(PerformanceProfile.COOL_GAMING)
                    processHelper.killBackgroundProcesses(gameRepository.getSelectedGamePackages())
                }

                if (thermalManager.shouldStronglyWarnCharging()) {
                    showChargingWarning("WARNING: Device is very hot! Unplug charger immediately.")
                } else if (thermalManager.shouldWarnCharging()) {
                    showChargingWarning(getString(R.string.charging_warning))
                }

                delay(profile.pollingIntervalMs)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        processCleanupJob?.cancel()
        gameDetectionJob?.cancel()
    }

    private fun startGameDetection() {
        gameDetectionJob = serviceScope.launch {
            val selectedPackages = gameRepository.getSelectedGamePackages()
            if (selectedPackages.isEmpty()) return@launch

            while (true) {
                val foregroundPkg = processHelper.getForegroundAppPackage()
                val isGaming = foregroundPkg != null && selectedPackages.contains(foregroundPkg)

                if (isGaming && currentGamePackage != foregroundPkg) {
                    if (currentGamePackage != null) {
                        recordSession()
                    }
                    currentGamePackage = foregroundPkg
                    sessionStartTime = System.currentTimeMillis()
                    sessionStartTemp = thermalManager.thermalInfo.value.batteryTemperature
                    sessionStartBattery = thermalManager.thermalInfo.value.batteryPercentage
                    thermalManager.resetSessionTracking()
                    _currentGame.value = foregroundPkg
                } else if (!isGaming && currentGamePackage != null) {
                    recordSession()
                    currentGamePackage = null
                    _currentGame.value = null
                }

                delay(3000L)
            }
        }
    }

    private fun startPeriodicCleanup() {
        processCleanupJob = serviceScope.launch {
            while (true) {
                processHelper.killBackgroundProcesses(gameRepository.getSelectedGamePackages())
                delay(30000L)
            }
        }
    }

    private fun updateNotification(temp: Float, battery: Int) {
        val profile = ProfileManager.getCurrentProfile()
        val text = "${profile.displayName} | ${temp.toInt()}°C | ${battery}%"
        val notification = buildNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showChargingWarning(message: String) {
        val channel = NotificationChannel(
            "warning_channel",
            getString(R.string.warning_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, "warning_channel")
            .setContentTitle("Charging Warning")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }

    private fun recordSession() {
        if (sessionStartTime == 0L) return

        val thermalInfo = thermalManager.thermalInfo.value
        val pkg = currentGamePackage ?: return
        val gameName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        val session = GameSession(
            gameName = gameName,
            packageName = pkg,
            startTime = sessionStartTime,
            endTime = System.currentTimeMillis(),
            startTemp = sessionStartTemp,
            maxTemp = thermalManager.getMaxTempInSession(),
            endTemp = thermalInfo.batteryTemperature,
            batteryDrain = sessionStartBattery - thermalInfo.batteryPercentage,
            thermalWarnings = thermalManager.thermalWarnings.value,
            profileUsed = ProfileManager.getCurrentProfile().displayName
        )

        serviceScope.launch {
            sessionRepository.recordSession(session)
        }

        sessionStartTime = 0L
    }
}
