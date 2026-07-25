package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.NeonSoundManager
import com.example.audio.WebRtcAudioCallManager
import com.example.data.FirebaseRealtimeManager
import com.example.data.GameSettingsRepository
import com.example.data.OnlineRoomData
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
    val firebaseManager = FirebaseRealtimeManager(application)
    val webRtcCallManager = WebRtcAudioCallManager(application, firebaseManager)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var aiJob: Job? = null
    private var onlineObserverJob: Job? = null

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
                if (_gameState.value.gridSize != size && _gameState.value.gameMode != GameMode.ONLINE_MULTIPLAYER) {
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

    // --- ONLINE MULTIPLAYER ACTIONS ---

    fun createOnlineRoom(onCodeGenerated: (String) -> Unit) {
        val roomCode = firebaseManager.generate3DigitCode()
        firebaseManager.createRoom(
            roomCode = roomCode,
            onSuccess = {
                _gameState.value = _gameState.value.copy(
                    gridSize = 3,
                    winningStreakTarget = 3,
                    gameMode = GameMode.ONLINE_MULTIPLAYER,
                    onlineRoomCode = roomCode,
                    isOnlineHost = true,
                    myOnlineSymbol = Symbol.O,
                    onlineStatus = "Waiting for Player B to join (Code: $roomCode)...",
                    board = List(9) { null },
                    activePlayer = Symbol.O,
                    isGameOver = false,
                    winner = null,
                    isDraw = false
                )
                startObservingOnlineRoom(roomCode)
                webRtcCallManager.startCall(roomCode, isHost = true)
                onCodeGenerated(roomCode)
            },
            onError = { err ->
                _gameState.value = _gameState.value.copy(onlineStatus = "Error: $err")
            }
        )
    }

    fun joinOnlineRoom(roomCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        firebaseManager.joinRoom(
            roomCode = roomCode,
            onSuccess = {
                _gameState.value = _gameState.value.copy(
                    gridSize = 3,
                    winningStreakTarget = 3,
                    gameMode = GameMode.ONLINE_MULTIPLAYER,
                    onlineRoomCode = roomCode,
                    isOnlineHost = false,
                    myOnlineSymbol = Symbol.X,
                    onlineStatus = "Connected to Room $roomCode!",
                    board = List(9) { null },
                    activePlayer = Symbol.O,
                    isGameOver = false,
                    winner = null,
                    isDraw = false
                )
                startObservingOnlineRoom(roomCode)
                webRtcCallManager.startCall(roomCode, isHost = false)
                onSuccess()
            },
            onError = { err ->
                onError(err)
            }
        )
    }

    private fun startObservingOnlineRoom(roomCode: String) {
        onlineObserverJob?.cancel()
        onlineObserverJob = viewModelScope.launch {
            firebaseManager.observeRoom(roomCode).collect { roomData ->
                if (roomData == null) return@collect
                updateFromOnlineRoomData(roomData)
            }
        }
    }

    private fun updateFromOnlineRoomData(roomData: OnlineRoomData) {
        val current = _gameState.value
        val newBoard = roomData.board.map {
            when (it) {
                "O" -> Symbol.O
                "X" -> Symbol.X
                else -> null
            }
        }

        val activeSymbol = if (roomData.activePlayer == "X") Symbol.X else Symbol.O
        val winningLine = GameEngine.checkWin(newBoard, 3, 3)
        val isDraw = roomData.isDraw || GameEngine.checkDraw(newBoard, winningLine)
        val winnerSymbol = when (roomData.winner) {
            "O" -> Symbol.O
            "X" -> Symbol.X
            else -> winningLine?.winner
        }
        val isGameOver = winnerSymbol != null || isDraw

        val opponentMuted = if (current.isOnlineHost) roomData.guestMutedMic else roomData.hostMutedMic

        val statusText = when {
            roomData.status == "WAITING" -> "Waiting for Player B (Code: ${roomData.roomCode})..."
            isGameOver -> if (winnerSymbol != null) "Winner: Player ${winnerSymbol.name}!" else "Draw Game!"
            activeSymbol == current.myOnlineSymbol -> "YOUR TURN (${current.myOnlineSymbol.name})"
            else -> "OPPONENT'S TURN (${activeSymbol.name})"
        }

        _gameState.value = current.copy(
            board = newBoard,
            activePlayer = activeSymbol,
            winningLine = winningLine,
            isGameOver = isGameOver,
            winner = winnerSymbol,
            isDraw = isDraw,
            onlineStatus = statusText,
            opponentMutedMic = opponentMuted
        )
    }

    fun toggleMicrophone() {
        webRtcCallManager.toggleMicrophone { muted ->
            _gameState.value = _gameState.value.copy(isMicMuted = muted)
        }
    }

    fun onUserCellClick(index: Int) {
        val current = _gameState.value
        if (current.isGameOver || current.isAiThinking) return
        if (index < 0 || index >= current.board.size) return
        if (current.board[index] != null) return

        if (current.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            if (current.onlineRoomCode == null) return
            if (current.activePlayer != current.myOnlineSymbol) return // Not my turn

            val newBoardList = current.board.toMutableList()
            newBoardList[index] = current.myOnlineSymbol
            val strBoard = newBoardList.map { it?.name ?: "" }

            val winningLine = GameEngine.checkWin(newBoardList, 3, 3)
            val isDraw = GameEngine.checkDraw(newBoardList, winningLine)
            val winnerStr = winningLine?.winner?.name
            val nextPlayerStr = current.myOnlineSymbol.other().name

            soundManager.playTap()
            soundManager.triggerHapticClick()

            firebaseManager.makeMove(
                roomCode = current.onlineRoomCode,
                board = strBoard,
                nextPlayer = nextPlayerStr,
                winner = winnerStr,
                isDraw = isDraw
            )
            return
        }

        if (current.gameMode == GameMode.VS_AI && current.activePlayer != Symbol.O) return
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
            if (current.gameMode == GameMode.VS_AI) {
                if (winningLine.winner == Symbol.O) {
                    soundManager.playWin()
                } else {
                    soundManager.playLose()
                }
            } else {
                soundManager.playWin()
            }
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
                delay(400)
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
        if (_gameState.value.gameMode == GameMode.ONLINE_MULTIPLAYER) return
        cancelAiJob()
        val state = _gameState.value
        if (state.moveHistory.isEmpty()) return

        soundManager.playTap()
        val newBoard = state.board.toMutableList()
        val newHistory = state.moveHistory.toMutableList()

        if (state.gameMode == GameMode.VS_AI) {
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
        if (current.gameMode == GameMode.ONLINE_MULTIPLAYER && current.onlineRoomCode != null) {
            val emptyBoard = List(9) { "" }
            firebaseManager.makeMove(current.onlineRoomCode, emptyBoard, "O", null, false)
            return
        }

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
            soundManager.isSoundEnabled = enabled
        }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMusicEnabled(enabled)
            soundManager.isMusicEnabled = enabled
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticsEnabled(enabled)
            soundManager.isHapticsEnabled = enabled
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
        onlineObserverJob?.cancel()
        webRtcCallManager.stopCall()
        soundManager.release()
    }
}
