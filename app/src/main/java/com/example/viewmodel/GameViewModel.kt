package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.NeonSoundManager
import com.example.data.GameSettingsRepository
import com.example.logic.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameSettingsRepository(application)
    val soundManager = NeonSoundManager(application)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            repository.soundEnabledFlow.collect { enabled ->
                soundManager.isSoundEnabled = enabled
            }
        }
        viewModelScope.launch {
            repository.musicEnabledFlow.collect { enabled ->
                soundManager.isMusicEnabled = enabled
            }
        }
        viewModelScope.launch {
            repository.hapticsEnabledFlow.collect { enabled ->
                soundManager.isHapticsEnabled = enabled
            }
        }
        viewModelScope.launch {
            repository.gridSizeFlow.collect { size ->
                if (_gameState.value.gridSize != size) {
                    resetBoardForGridSize(size)
                }
            }
        }
        viewModelScope.launch {
            repository.difficultyFlow.collect { diff ->
                _gameState.value = _gameState.value.copy(aiDifficulty = diff)
            }
        }
        viewModelScope.launch {
            repository.gameModeFlow.collect { mode ->
                _gameState.value = _gameState.value.copy(gameMode = mode)
            }
        }
        viewModelScope.launch {
            repository.playerOWinsFlow.collect { wins ->
                _gameState.value = _gameState.value.copy(playerOScore = wins)
            }
        }
        viewModelScope.launch {
            repository.playerXWinsFlow.collect { wins ->
                _gameState.value = _gameState.value.copy(playerXScore = wins)
            }
        }
    }

    private fun cancelAiJob() {
        aiJob?.cancel()
        aiJob = null
    }

    private fun resetBoardForGridSize(size: Int) {
        cancelAiJob()
        val totalCells = size * size
        val targetStreak = GameEngine.defaultStreakTarget(size)
        _gameState.value = _gameState.value.copy(
            gridSize = size,
            winningStreakTarget = targetStreak,
            board = List(totalCells) { null },
            activePlayer = Symbol.O,
            winningLine = null,
            isGameOver = false,
            winner = null,
            isDraw = false,
            hintCellIndex = null,
            moveHistory = emptyList(),
            isAiThinking = false
        )
    }

    fun onUserCellClick(index: Int) {
        val current = _gameState.value
        if (current.isGameOver || current.isAiThinking) return
        if (current.gameMode == GameMode.VS_AI && current.activePlayer != Symbol.O) return
        if (index < 0 || index >= current.board.size) return
        if (current.board[index] != null) return

        makeMove(index)
    }

    fun makeMove(index: Int) {
        val current = _gameState.value
        if (current.isGameOver) return
        if (index < 0 || index >= current.board.size) return
        if (current.board[index] != null) return

        soundManager.playTap()
        soundManager.triggerHapticClick()

        val newBoard = current.board.toMutableList()
        val currentPlayer = current.activePlayer
        newBoard[index] = currentPlayer

        val newHistory = current.moveHistory + Pair(index, currentPlayer)
        val winningLine = GameEngine.checkWin(newBoard, current.gridSize, current.winningStreakTarget)
        val isDraw = GameEngine.checkDraw(newBoard, winningLine)
        val isGameOver = winningLine != null || isDraw

        var newOScore = current.playerOScore
        var newXScore = current.playerXScore

        if (winningLine != null) {
            soundManager.playWin()
            soundManager.triggerHapticWin()
            if (winningLine.winner == Symbol.O) {
                newOScore++
                viewModelScope.launch { repository.incrementPlayerOWins() }
            } else {
                newXScore++
                viewModelScope.launch { repository.incrementPlayerXWins() }
            }
        } else if (isDraw) {
            soundManager.playDraw()
            viewModelScope.launch { repository.incrementDraws() }
        }

        val nextPlayer = if (isGameOver) currentPlayer else currentPlayer.other()

        _gameState.value = current.copy(
            board = newBoard,
            activePlayer = nextPlayer,
            winningLine = winningLine,
            isGameOver = isGameOver,
            winner = winningLine?.winner,
            isDraw = isDraw,
            playerOScore = newOScore,
            playerXScore = newXScore,
            hintCellIndex = null,
            moveHistory = newHistory
        )

        if (!isGameOver && current.gameMode == GameMode.VS_AI && nextPlayer == Symbol.X) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        cancelAiJob()
        _gameState.value = _gameState.value.copy(isAiThinking = true)

        aiJob = viewModelScope.launch {
            try {
                delay(400) // Natural thinking pause for AI
                val state = _gameState.value
                if (state.isGameOver || state.activePlayer != Symbol.X) return@launch

                val aiMove = MinimaxAI.findBestMove(
                    board = state.board,
                    gridSize = state.gridSize,
                    aiSymbol = Symbol.X,
                    difficulty = state.aiDifficulty,
                    streakTarget = state.winningStreakTarget
                )

                if (aiMove != -1 && state.board[aiMove] == null) {
                    makeMove(aiMove)
                }
            } finally {
                _gameState.value = _gameState.value.copy(isAiThinking = false)
            }
        }
    }

    fun undoMove() {
        cancelAiJob()
        val state = _gameState.value
        if (state.moveHistory.isEmpty()) return

        soundManager.playTap()
        val newBoard = state.board.toMutableList()
        val newHistory = state.moveHistory.toMutableList()

        if (state.gameMode == GameMode.VS_AI) {
            // In VS AI mode, undo both AI's last move and Player's last move if needed
            val lastMove = newHistory.removeAt(newHistory.size - 1)
            newBoard[lastMove.first] = null

            if (lastMove.second == Symbol.X && newHistory.isNotEmpty()) {
                val playerMove = newHistory.removeAt(newHistory.size - 1)
                newBoard[playerMove.first] = null
            }
        } else {
            val lastMove = newHistory.removeAt(newHistory.size - 1)
            newBoard[lastMove.first] = null
        }

        val previousPlayer = if (newHistory.isEmpty()) Symbol.O else newHistory.last().second.other()

        _gameState.value = state.copy(
            board = newBoard,
            activePlayer = if (newHistory.isEmpty()) Symbol.O else previousPlayer,
            winningLine = null,
            isGameOver = false,
            winner = null,
            isDraw = false,
            hintCellIndex = null,
            moveHistory = newHistory,
            isAiThinking = false
        )
    }

    fun provideHint() {
        val state = _gameState.value
        if (state.isGameOver || state.isAiThinking) return

        soundManager.playHint()
        val bestMove = MinimaxAI.findBestMove(
            board = state.board,
            gridSize = state.gridSize,
            aiSymbol = state.activePlayer,
            difficulty = AiDifficulty.HARD,
            streakTarget = state.winningStreakTarget
        )

        if (bestMove != -1) {
            _gameState.value = state.copy(hintCellIndex = bestMove)
        }
    }

    fun restartRound() {
        cancelAiJob()
        soundManager.playTap()
        val current = _gameState.value
        val totalCells = current.gridSize * current.gridSize
        _gameState.value = current.copy(
            board = List(totalCells) { null },
            activePlayer = Symbol.O,
            winningLine = null,
            isGameOver = false,
            winner = null,
            isDraw = false,
            hintCellIndex = null,
            moveHistory = emptyList(),
            currentRound = current.currentRound + 1,
            isAiThinking = false
        )
    }

    fun setGameMode(mode: GameMode) {
        viewModelScope.launch {
            repository.setGameMode(mode)
            restartRound()
        }
    }

    fun setGridSize(size: Int) {
        viewModelScope.launch {
            repository.setGridSize(size)
            resetBoardForGridSize(size)
        }
    }

    fun setDifficulty(difficulty: AiDifficulty) {
        viewModelScope.launch {
            repository.setDifficulty(difficulty)
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMusicEnabled(enabled)
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticsEnabled(enabled)
        }
    }

    fun resetScores() {
        viewModelScope.launch {
            repository.resetScores()
            restartRound()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
