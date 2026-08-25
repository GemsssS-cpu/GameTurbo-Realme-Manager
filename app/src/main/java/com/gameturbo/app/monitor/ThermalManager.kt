package com.gameturbo.app.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThermalLevel {
    COOL, NORMAL, WARM, HOT
}

enum class SystemThermalStatus {
    NONE, LOW, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNSUPPORTED
}

data class ThermalInfo(
    val batteryTemperature: Float = 0f,
    val thermalLevel: ThermalLevel = ThermalLevel.COOL,
    val systemThermalStatus: SystemThermalStatus = SystemThermalStatus.UNSUPPORTED,
    val thermalStatusSource: String = "BatteryManager (estimated)",
    val batteryPercentage: Int = 0,
    val isCharging: Boolean = false,
    val chargingMethod: String = "Unknown"
)

class ThermalManager(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _thermalInfo = MutableStateFlow(ThermalInfo())
    val thermalInfo: StateFlow<ThermalInfo> = _thermalInfo.asStateFlow()

    private val _thermalWarnings = MutableStateFlow(0)
    val thermalWarnings: StateFlow<Int> = _thermalWarnings.asStateFlow()

    private var maxTempInSession: Float = 0f

    fun getMaxTempInSession(): Float = maxTempInSession

    fun resetSessionTracking() {
        maxTempInSession = 0f
        _thermalWarnings.value = 0
    }

    fun update(): ThermalInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val tempCelsius = batteryIntent?.let {
            it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        } ?: 0f

        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargingMethod = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }

        val thermalLevel = classifyTemperature(tempCelsius)

        val systemThermal = getSystemThermalStatus()

        val info = ThermalInfo(
            batteryTemperature = tempCelsius,
            thermalLevel = thermalLevel,
            systemThermalStatus = systemThermal,
            thermalStatusSource = if (systemThermal != SystemThermalStatus.UNSUPPORTED)
                "PowerManager API" else "BatteryManager (estimated)",
            batteryPercentage = batteryLevel,
            isCharging = isCharging,
            chargingMethod = chargingMethod
        )

        _thermalInfo.value = info

        if (tempCelsius > maxTempInSession) {
            maxTempInSession = tempCelsius
        }

        if (thermalLevel == ThermalLevel.HOT) {
            _thermalWarnings.value = _thermalWarnings.value + 1
        }

        return info
    }

    private fun classifyTemperature(tempCelsius: Float): ThermalLevel {
        return when {
            tempCelsius < 33f -> ThermalLevel.COOL
            tempCelsius < 38f -> ThermalLevel.NORMAL
            tempCelsius < 42f -> ThermalLevel.WARM
            else -> ThermalLevel.HOT
        }
    }

    private fun getSystemThermalStatus(): SystemThermalStatus {
        if (powerManager == null) return SystemThermalStatus.UNSUPPORTED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return SystemThermalStatus.UNSUPPORTED

        return try {
            when (powerManager!!.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> SystemThermalStatus.NONE
                PowerManager.THERMAL_STATUS_LIGHT -> SystemThermalStatus.LOW
                PowerManager.THERMAL_STATUS_MODERATE -> SystemThermalStatus.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> SystemThermalStatus.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> SystemThermalStatus.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> SystemThermalStatus.EMERGENCY
                PowerManager.THERMAL_STATUS_SHUTDOWN -> SystemThermalStatus.SHUTDOWN
                else -> SystemThermalStatus.UNSUPPORTED
            }
        } catch (e: Exception) {
            SystemThermalStatus.UNSUPPORTED
        }
    }

    fun shouldWarnCharging(): Boolean {
        val info = _thermalInfo.value
        return info.isCharging && info.batteryTemperature >= 40f
    }

    fun shouldStronglyWarnCharging(): Boolean {
        val info = _thermalInfo.value
        return info.isCharging && info.batteryTemperature >= 44f
    }

    fun shouldAutoSwitchToThermalSaver(): Boolean {
        val info = _thermalInfo.value
        return info.systemThermalStatus == SystemThermalStatus.SEVERE ||
            info.systemThermalStatus == SystemThermalStatus.CRITICAL ||
            info.systemThermalStatus == SystemThermalStatus.EMERGENCY ||
            info.batteryTemperature >= 44f
    }
}
