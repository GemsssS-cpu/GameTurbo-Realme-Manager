package com.gameturbo.app.monitor

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class ProcessHelper(context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val myPackageName = context.packageName

    fun killBackgroundProcesses(excludePackages: Set<String> = emptySet()): Int {
        var killedCount = 0
        val runningProcesses = activityManager?.runningAppProcesses ?: return 0

        for (processInfo in runningProcesses) {
            val pkgName = processInfo.processName.substringBefore(":")
            if (pkgName == myPackageName) continue
            if (excludePackages.contains(pkgName)) continue
            if (processInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                try {
                    activityManager?.killBackgroundProcesses(pkgName)
                    killedCount++
                } catch (e: Exception) {
                    // Permission denied or process already dead
                }
            }
        }
        return killedCount
    }

    fun getRunningProcessCount(): Int {
        return activityManager?.runningAppProcesses?.size ?: 0
    }

    fun getForegroundAppPackage(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val runningProcesses = activityManager?.runningAppProcesses ?: return null
        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return processInfo.processName.substringBefore(":")
            }
        }
        return null
    }
}
