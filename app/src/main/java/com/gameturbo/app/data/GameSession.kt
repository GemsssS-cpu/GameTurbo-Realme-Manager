package com.gameturbo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameName: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val startTemp: Float,
    val maxTemp: Float,
    val endTemp: Float,
    val batteryDrain: Int,
    val thermalWarnings: Int,
    val profileUsed: String
)
