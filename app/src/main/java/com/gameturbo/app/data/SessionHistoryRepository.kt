package com.gameturbo.app.data

import kotlinx.coroutines.flow.Flow

class SessionHistoryRepository(private val sessionDao: SessionDao) {

    val allSessions: Flow<List<GameSession>> = sessionDao.getAllSessions()
    val sessionCount: Flow<Int> = sessionDao.getSessionCount()

    fun getSessionsByPackage(packageName: String): Flow<List<GameSession>> =
        sessionDao.getSessionsByPackage(packageName)

    suspend fun recordSession(session: GameSession) {
        sessionDao.insertSession(session)
    }
}
