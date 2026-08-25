package com.gameturbo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.profiles.PerformanceProfile
import com.gameturbo.app.profiles.ProfileManager
import com.gameturbo.app.ui.theme.*

@Composable
fun ProfilesScreen(
    currentProfile: PerformanceProfile,
    onProfileSelected: (PerformanceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Performance Profiles",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Choose a profile that fits your gaming style",
            fontSize = 13.sp,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        PerformanceProfile.entries.forEach { profile ->
            ProfileCard(
                profile = profile,
                isSelected = currentProfile == profile,
                onClick = { onProfileSelected(profile) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Profile Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Background Cleanup", if (currentProfile.cleanupProcesses) "Active" else "Off")
                DetailRow("Polling Interval", "${currentProfile.pollingIntervalMs / 1000}s")
                DetailRow("Notification Silencing", if (currentProfile.silenceNotifications) "On" else "Off")
                DetailRow("Keep Screen Awake", if (currentProfile.keepScreenAwake) "On" else "Off")
                DetailRow("Auto Thermal Saver", if (currentProfile.autoThermalSaver) "On" else "Off")
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: PerformanceProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentBlue else Color.Transparent
    val bgColor = if (isSelected) AccentBlue.copy(alpha = 0.1f) else DarkCard

    val icon = when (profile) {
        PerformanceProfile.BALANCED -> "⚖️"
        PerformanceProfile.PERFORMANCE -> "🚀"
        PerformanceProfile.COOL_GAMING -> "❄️"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profile.description,
                fontSize = 12.sp,
                color = DarkTextSecondary
            )
        }
        if (isSelected) {
            Text("✓", fontSize = 20.sp, color = AccentBlue)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = DarkTextSecondary)
        Text(text = value, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = colors,
        shape = shape
    ) {
        content()
    }
}
