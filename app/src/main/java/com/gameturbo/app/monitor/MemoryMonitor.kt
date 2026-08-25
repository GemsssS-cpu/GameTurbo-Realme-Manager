package com.gameturbo.app.monitor

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemoryInfo(
    val totalRamMB: Long = 0,
    val availableRamMB: Long = 0,
    val usedRamMB: Long = 0,
    val usagePercentage: Int = 0
)

class MemoryMonitor(context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _memoryInfo = MutableStateFlow(MemoryInfo())
    val memoryInfo: StateFlow<MemoryInfo> = _memoryInfo.asStateFlow()

    fun update(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalMB = memInfo.totalMem / (1024 * 1024)
        val availableMB = memInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - availableMB
        val usage = if (totalMB > 0) ((usedMB.toFloat() / totalMB) * 100).toInt() else 0

        val info = MemoryInfo(
            totalRamMB = totalMB,
            availableRamMB = availableMB,
            usedRamMB = usedMB,
            usagePercentage = usage
        )

        _memoryInfo.value = info
        return info
    }
}
