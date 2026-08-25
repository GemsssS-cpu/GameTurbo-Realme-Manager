package com.gameturbo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.gameturbo.app.MainActivity
import com.gameturbo.app.R
import com.gameturbo.app.monitor.ThermalManager
import com.gameturbo.app.monitor.MemoryMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private lateinit var thermalManager: ThermalManager
    private lateinit var memoryMonitor: MemoryMonitor
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1002

        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        thermalManager = ThermalManager(this)
        memoryMonitor = MemoryMonitor(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        _isActive.value = true

        if (Settings.canDrawOverlays(this)) {
            showOverlay()
            startUpdating()
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        updateJob?.cancel()
        serviceScope.cancel()
        _isActive.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Tap to return to Game Turbo")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(200, 20, 20, 30))
            setPadding(16, 12, 16, 12)
            setOnTouchListener(OverlayTouchListener())
        }

        val tempText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            tag = "temp"
        }

        val batteryText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            tag = "battery"
        }

        val ramText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            tag = "ram"
        }

        val fpsText = TextView(this).apply {
            setTextColor(Color.GRAY)
            textSize = 12f
            text = "FPS: ${getString(R.string.fps_unavailable)}"
        }

        val statusText = TextView(this).apply {
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 11f
            tag = "status"
        }

        container.addView(tempText)
        container.addView(batteryText)
        container.addView(ramText)
        container.addView(fpsText)
        container.addView(statusText)

        overlayView = container

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }

        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun updateOverlay() {
        val view = overlayView ?: return

        val thermalInfo = thermalManager.update()
        val memInfo = memoryMonitor.update()

        view.findViewWithTag<TextView>("temp")?.let {
            it.text = "Temp: ${thermalInfo.batteryTemperature.toInt()}°C"
            it.setTextColor(getThermalColor(thermalInfo.thermalLevel.name))
        }

        view.findViewWithTag<TextView>("battery")?.let {
            it.text = "Battery: ${thermalInfo.batteryPercentage}% ${if (thermalInfo.isCharging) "⚡" else ""}"
        }

        view.findViewWithTag<TextView>("ram")?.let {
            it.text = "RAM: ${memInfo.usedRamMB}/${memInfo.totalRamMB}MB (${memInfo.usagePercentage}%)"
        }

        view.findViewWithTag<TextView>("status")?.let {
            val profileName = com.gameturbo.app.profiles.ProfileManager.getCurrentProfile().displayName
            it.text = "Mode: $profileName"
        }
    }

    private fun getThermalColor(level: String): Int {
        return when (level) {
            "COOL" -> Color.parseColor("#4CAF50")
            "NORMAL" -> Color.parseColor("#FFEB3B")
            "WARM" -> Color.parseColor("#FF9800")
            "HOT" -> Color.parseColor("#F44336")
            else -> Color.WHITE
        }
    }

    private fun startUpdating() {
        updateJob = serviceScope.launch {
            while (isActive) {
                updateOverlay()
                delay(3000L)
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    private inner class OverlayTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val params = v.layoutParams as WindowManager.LayoutParams
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val params = v.layoutParams as WindowManager.LayoutParams
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(v, params)
                    return true
                }
            }
            return false
        }
    }
}
