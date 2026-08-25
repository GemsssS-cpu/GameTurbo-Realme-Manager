@file:OptIn(ExperimentalMaterial3Api::class)

package com.gameturbo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.data.GameInfo
import com.gameturbo.app.data.GameRepository
import com.gameturbo.app.ui.components.GameCard
import com.gameturbo.app.ui.theme.*

@Composable
fun GameLibraryScreen(
    gameRepository: GameRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedGames by gameRepository.selectedGames.collectAsState()
    val allApps by gameRepository.allApps.collectAsState()
    val gamesOnly by gameRepository.gamesOnly.collectAsState()

    var showAllApps by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val displayList = if (showAllApps) allApps else gamesOnly

    LaunchedEffect(Unit) {
        isLoading = true
        gameRepository.scanInstalledApps()
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Game Library",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${selectedGames.size} game(s) selected",
            fontSize = 13.sp,
            color = DarkTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showAllApps,
                onClick = { showAllApps = false },
                label = { Text("Games Only") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue
                )
            )
            FilterChip(
                selected = showAllApps,
                onClick = { showAllApps = true },
                label = { Text("All Apps") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (displayList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📱", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showAllApps) "No apps found" else "No games detected",
                        fontSize = 16.sp,
                        color = DarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try switching to All Apps to manually select games",
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
                items(displayList) { game ->
                    GameCard(
                        game = game,
                        isSelected = selectedGames.any { it.packageName == game.packageName },
                        onToggle = { gameRepository.toggleGameSelection(game) },
                        onLaunch = { gameRepository.launchGame(context, game.packageName) }
                    )
                }
            }
        }
    }
}
