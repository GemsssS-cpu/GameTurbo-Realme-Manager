package com.gameturbo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: GameSession)

    @Query("SELECT * FROM game_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions WHERE packageName = :pkg ORDER BY startTime DESC")
    fun getSessionsByPackage(pkg: String): Flow<List<GameSession>>

    @Query("SELECT COUNT(*) FROM game_sessions")
    fun getSessionCount(): Flow<Int>
}
