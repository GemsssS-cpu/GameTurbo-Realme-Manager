package com.gameturbo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.data.GameSession
import com.gameturbo.app.data.SessionHistoryRepository
import com.gameturbo.app.ui.theme.*
import com.gameturbo.app.util.toFormattedDate
import com.gameturbo.app.util.toFormattedDuration

@Composable
fun HistoryScreen(
    repository: SessionHistoryRepository,
    modifier: Modifier = Modifier
) {
    val sessions by repository.allSessions.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Session History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${sessions.size} session(s) recorded",
            fontSize = 13.sp,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No gaming sessions recorded yet",
                        fontSize = 16.sp,
                        color = DarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start Gaming Mode and play a game to record sessions",
                        fontSize = 12.sp,
                        color = DarkTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: GameSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.gameName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = session.profileUsed,
                fontSize = 11.sp,
                color = AccentBlue,
                modifier = Modifier
                    .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = session.startTime.toFormattedDate(),
            fontSize = 11.sp,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SessionStat("Duration", (session.endTime - session.startTime).toFormattedDuration())
            SessionStat("Start", "${session.startTemp.toInt()}°C")
            SessionStat("Max", "${session.maxTemp.toInt()}°C")
            SessionStat("End", "${session.endTemp.toInt()}°C")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SessionStat("Battery Drain", "${session.batteryDrain}%")
            SessionStat("Thermal Warnings", session.thermalWarnings.toString())
        }
    }
}

@Composable
private fun SessionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = DarkTextSecondary
        )
    }
}
