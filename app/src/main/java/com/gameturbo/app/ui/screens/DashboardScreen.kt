package com.gameturbo.app.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.monitor.ThermalInfo
import com.gameturbo.app.monitor.ThermalLevel
import com.gameturbo.app.monitor.MemoryInfo
import com.gameturbo.app.profiles.PerformanceProfile
import com.gameturbo.app.profiles.ProfileManager
import com.gameturbo.app.service.GamingModeService
import com.gameturbo.app.service.OverlayService
import com.gameturbo.app.ui.components.*
import com.gameturbo.app.ui.theme.*

@Composable
fun DashboardScreen(
    thermalInfo: ThermalInfo,
    memoryInfo: MemoryInfo,
    isGamingModeActive: Boolean,
    currentGame: String?,
    currentProfile: PerformanceProfile,
    onStartGamingMode: () -> Unit,
    onStopGamingMode: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Game Turbo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isGamingModeActive) "Gaming Mode Active" else "Ready to Game",
            fontSize = 14.sp,
            color = if (isGamingModeActive) StatusActive else DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Temperature Gauge
        TemperatureGauge(
            temperature = thermalInfo.batteryTemperature,
            thermalLevel = thermalInfo.thermalLevel
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Source: ${thermalInfo.thermalStatusSource}",
            fontSize = 10.sp,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Thermal Badge
        ThermalBadge(thermalLevel = thermalInfo.thermalLevel)

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatTile(
                    title = "Battery",
                    value = "${thermalInfo.batteryPercentage}%",
                    icon = "🔋",
                    valueColor = when {
                        thermalInfo.batteryPercentage <= 20 -> ThermalHot
                        thermalInfo.batteryPercentage <= 50 -> ThermalWarm
                        else -> ThermalCool
                    },
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Charging",
                    value = if (thermalInfo.isCharging) thermalInfo.chargingMethod else "Not Charging",
                    icon = "⚡",
                    valueColor = if (thermalInfo.isCharging) ThermalWarm else DarkTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatTile(
                    title = "RAM Used",
                    value = "${memoryInfo.usedRamMB}/${memoryInfo.totalRamMB}MB",
                    icon = "💾",
                    valueColor = when {
                        memoryInfo.usagePercentage > 85 -> ThermalHot
                        memoryInfo.usagePercentage > 65 -> ThermalWarm
                        else -> ThermalCool
                    },
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Profile",
                    value = currentProfile.displayName,
                    icon = "⚙️",
                    valueColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatTile(
                    title = "Thermal",
                    value = thermalInfo.systemThermalStatus.name,
                    icon = "🌡️",
                    valueColor = when (thermalInfo.systemThermalStatus) {
                        com.gameturbo.app.monitor.SystemThermalStatus.NONE -> ThermalCool
                        com.gameturbo.app.monitor.SystemThermalStatus.LOW -> ThermalNormal
                        com.gameturbo.app.monitor.SystemThermalStatus.MODERATE -> ThermalWarm
                        else -> ThermalHot
                    },
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Current Game",
                    value = currentGame?.substringAfterLast('.') ?: "None",
                    icon = "🎮",
                    valueColor = if (currentGame != null) AccentGreen else DarkTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Charging Warning
        if (thermalInfo.isCharging && thermalInfo.batteryTemperature >= 40f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ThermalHot.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (thermalInfo.batteryTemperature >= 44f)
                            "Very hot! Unplug charger immediately."
                        else
                            "Unplug charger to reduce heat while gaming.",
                        fontSize = 13.sp,
                        color = ThermalHot
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Gaming Mode Toggle
        Button(
            onClick = {
                if (isGamingModeActive) {
                    onStopGamingMode()
                } else {
                    onStartGamingMode()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGamingModeActive) AccentRed else AccentGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isGamingModeActive) "Stop Gaming Mode" else "Start Gaming Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overlay Toggle
        if (isGamingModeActive && Settings.canDrawOverlays(context)) {
            OutlinedButton(
                onClick = {
                    if (com.gameturbo.app.service.OverlayService.isActive.value) {
                        OverlayService.stop(context)
                    } else {
                        OverlayService.start(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentBlue
                )
            ) {
                Text(
                    text = if (com.gameturbo.app.service.OverlayService.isActive.value)
                        "Hide Overlay" else "Show In-Game Overlay",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
