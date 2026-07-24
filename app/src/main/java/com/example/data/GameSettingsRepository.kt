package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.logic.AiDifficulty
import com.example.logic.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "neon_tictactoe_settings")

class GameSettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val AI_DIFFICULTY = stringPreferencesKey("ai_difficulty")
        val GAME_MODE = stringPreferencesKey("game_mode")
        val PLAYER_O_WINS = intPreferencesKey("player_o_wins")
        val PLAYER_X_WINS = intPreferencesKey("player_x_wins")
        val DRAWS = intPreferencesKey("draws")
        val TARGET_WINS = intPreferencesKey("target_wins")
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[PreferenceKeys.SOUND_ENABLED] ?: true
    }

    val musicEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[PreferenceKeys.MUSIC_ENABLED] ?: true
    }

    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[PreferenceKeys.HAPTICS_ENABLED] ?: true
    }

    val gridSizeFlow: Flow<Int> = context.dataStore.data.map {
        it[PreferenceKeys.GRID_SIZE] ?: 3
    }

    val difficultyFlow: Flow<AiDifficulty> = context.dataStore.data.map {
        val name = it[PreferenceKeys.AI_DIFFICULTY] ?: AiDifficulty.HARD.name
        try { AiDifficulty.valueOf(name) } catch (e: Exception) { AiDifficulty.HARD }
    }

    val gameModeFlow: Flow<GameMode> = context.dataStore.data.map {
        val name = it[PreferenceKeys.GAME_MODE] ?: GameMode.VS_AI.name
        try { GameMode.valueOf(name) } catch (e: Exception) { GameMode.VS_AI }
    }

    val playerOWinsFlow: Flow<Int> = context.dataStore.data.map {
        it[PreferenceKeys.PLAYER_O_WINS] ?: 0
    }

    val playerXWinsFlow: Flow<Int> = context.dataStore.data.map {
        it[PreferenceKeys.PLAYER_X_WINS] ?: 0
    }

    val drawsFlow: Flow<Int> = context.dataStore.data.map {
        it[PreferenceKeys.DRAWS] ?: 0
    }

    val targetWinsFlow: Flow<Int> = context.dataStore.data.map {
        it[PreferenceKeys.TARGET_WINS] ?: 3
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setGridSize(size: Int) {
        context.dataStore.edit { it[PreferenceKeys.GRID_SIZE] = size }
    }

    suspend fun setDifficulty(difficulty: AiDifficulty) {
        context.dataStore.edit { it[PreferenceKeys.AI_DIFFICULTY] = difficulty.name }
    }

    suspend fun setGameMode(mode: GameMode) {
        context.dataStore.edit { it[PreferenceKeys.GAME_MODE] = mode.name }
    }

    suspend fun incrementPlayerOWins() {
        context.dataStore.edit {
            val current = it[PreferenceKeys.PLAYER_O_WINS] ?: 0
            it[PreferenceKeys.PLAYER_O_WINS] = current + 1
        }
    }

    suspend fun incrementPlayerXWins() {
        context.dataStore.edit {
            val current = it[PreferenceKeys.PLAYER_X_WINS] ?: 0
            it[PreferenceKeys.PLAYER_X_WINS] = current + 1
        }
    }

    suspend fun incrementDraws() {
        context.dataStore.edit {
            val current = it[PreferenceKeys.DRAWS] ?: 0
            it[PreferenceKeys.DRAWS] = current + 1
        }
    }

    suspend fun resetScores() {
        context.dataStore.edit {
            it[PreferenceKeys.PLAYER_O_WINS] = 0
            it[PreferenceKeys.PLAYER_X_WINS] = 0
            it[PreferenceKeys.DRAWS] = 0
        }
    }
}
