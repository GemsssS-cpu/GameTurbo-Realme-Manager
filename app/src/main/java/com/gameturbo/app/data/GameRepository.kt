package com.gameturbo.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GameInfo(
    val packageName: String,
    val name: String
)

class GameRepository(context: Context) {

    private val prefs = context.getSharedPreferences("gameturbo_prefs", Context.MODE_PRIVATE)
    private val packageManager = context.packageManager
    private val json = Json { ignoreUnknownKeys = true }

    private val _selectedGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val selectedGames: StateFlow<List<GameInfo>> = _selectedGames.asStateFlow()

    private val _allApps = MutableStateFlow<List<GameInfo>>(emptyList())
    val allApps: StateFlow<List<GameInfo>> = _allApps.asStateFlow()

    private val _gamesOnly = MutableStateFlow<List<GameInfo>>(emptyList())
    val gamesOnly: StateFlow<List<GameInfo>> = _gamesOnly.asStateFlow()

    init {
        loadSelectedGames()
    }

    private fun loadSelectedGames() {
        val jsonStr = prefs.getString("selected_games", "[]") ?: "[]"
        try {
            _selectedGames.value = json.decodeFromString<List<GameInfo>>(jsonStr)
        } catch (e: Exception) {
            _selectedGames.value = emptyList()
        }
    }

    private fun saveSelectedGames() {
        val jsonStr = json.encodeToString(_selectedGames.value)
        prefs.edit().putString("selected_games", jsonStr).apply()
    }

    suspend fun scanInstalledApps() = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val allAppsList = packageManager.queryIntentActivities(mainIntent, 0).mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg != "com.gameturbo.app") {
                GameInfo(
                    packageName = pkg,
                    name = resolveInfo.loadLabel(packageManager).toString()
                )
            } else null
        }.sortedBy { it.name }

        val gamesList = allAppsList.filter { info ->
            try {
                val appInfo = packageManager.getApplicationInfo(info.packageName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                    (appInfo.category == ApplicationInfo.CATEGORY_GAME)
            } catch (e: Exception) {
                false
            }
        }

        _allApps.value = allAppsList
        _gamesOnly.value = gamesList
    }

    fun toggleGameSelection(game: GameInfo) {
        val current = _selectedGames.value.toMutableList()
        val existing = current.indexOfFirst { it.packageName == game.packageName }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(game)
        }
        _selectedGames.value = current
        saveSelectedGames()
    }

    fun isGameSelected(packageName: String): Boolean {
        return _selectedGames.value.any { it.packageName == packageName }
    }

    fun launchGame(context: Context, packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun getGameIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    fun getSelectedGamePackages(): Set<String> {
        return _selectedGames.value.map { it.packageName }.toSet()
    }
}
