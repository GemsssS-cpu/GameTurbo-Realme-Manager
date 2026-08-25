package com.gameturbo.app

import android.app.Application
import com.gameturbo.app.data.AppDatabase
import com.gameturbo.app.data.GameRepository
import com.gameturbo.app.data.SessionHistoryRepository
import com.gameturbo.app.monitor.ThermalManager
import com.gameturbo.app.monitor.BatteryMonitor
import com.gameturbo.app.monitor.MemoryMonitor

class GameTurboApp : Application() {

    lateinit var gameRepository: GameRepository
        private set
    lateinit var sessionRepository: SessionHistoryRepository
        private set
    lateinit var thermalManager: ThermalManager
        private set
    lateinit var batteryMonitor: BatteryMonitor
        private set
    lateinit var memoryMonitor: MemoryMonitor
        private set

    override fun onCreate() {
        super.onCreate()

        gameRepository = GameRepository(this)

        val db = AppDatabase.getInstance(this)
        sessionRepository = SessionHistoryRepository(db.sessionDao())

        thermalManager = ThermalManager(this)
        batteryMonitor = BatteryMonitor(this)
        memoryMonitor = MemoryMonitor(this)
    }
}
