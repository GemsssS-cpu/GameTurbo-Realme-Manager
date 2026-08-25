package com.gameturbo.app.profiles

enum class PerformanceProfile(
    val displayName: String,
    val description: String,
    val cleanupProcesses: Boolean,
    val pollingIntervalMs: Long,
    val silenceNotifications: Boolean,
    val keepScreenAwake: Boolean,
    val autoThermalSaver: Boolean
) {
    BALANCED(
        displayName = "Balanced",
        description = "Normal background behavior. Good balance of performance and battery.",
        cleanupProcesses = false,
        pollingIntervalMs = 5000L,
        silenceNotifications = false,
        keepScreenAwake = true,
        autoThermalSaver = true
    ),
    PERFORMANCE(
        displayName = "Performance",
        description = "Minimize background interference for maximum gaming performance.",
        cleanupProcesses = true,
        pollingIntervalMs = 2000L,
        silenceNotifications = true,
        keepScreenAwake = true,
        autoThermalSaver = true
    ),
    COOL_GAMING(
        displayName = "Cool Gaming",
        description = "Prioritize lower temperature and stable frame times over peak performance.",
        cleanupProcesses = true,
        pollingIntervalMs = 10000L,
        silenceNotifications = true,
        keepScreenAwake = true,
        autoThermalSaver = false
    );
}

object ProfileManager {
    private var currentProfile: PerformanceProfile = PerformanceProfile.BALANCED

    fun getCurrentProfile(): PerformanceProfile = currentProfile

    fun setProfile(profile: PerformanceProfile) {
        currentProfile = profile
    }
}
