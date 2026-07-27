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
    private var turnTimerJob: Job? = null

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
                // Do not override ONLINE_MULTIPLAYER if an online session is active
                val current = _gameState.value
                if (current.gameMode == GameMode.ONLINE_MULTIPLAYER && current.onlineRoomCode != null && mode != GameMode.ONLINE_MULTIPLAYER) {
                    return@collect
                }
                _gameState.value = current.copy(gameMode = mode)
            }
        }
        viewModelScope.launch {
            repository.playerOWinsFlow.collect { wins ->
                if (_gameState.value.gameMode != GameMode.ONLINE_MULTIPLAYER) {
                    _gameState.value = _gameState.value.copy(playerOScore = wins)
                }
            }
        }
        viewModelScope.launch {
            repository.playerXWinsFlow.collect { wins ->
                if (_gameState.value.gameMode != GameMode.ONLINE_MULTIPLAYER) {
                    _gameState.value = _gameState.value.copy(playerXScore = wins)
                }
            }
        }
    }

    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        val current = _gameState.value
        if (current.isGameOver) return

        _gameState.value = current.copy(turnTimeRemaining = 30)

        turnTimerJob = viewModelScope.launch {
            var time = 30
            while (time > 0) {
                delay(1000)
                val state = _gameState.value
                if (state.isGameOver) break
                time -= 1
                _gameState.value = _gameState.value.copy(turnTimeRemaining = time)
            }
            val finalState = _gameState.value
            if (!finalState.isGameOver && time <= 0) {
                handleTurnTimeout()
            }
        }
    }

    private fun startTurnTimerForOnline(initialSeconds: Int) {
        turnTimerJob?.cancel()
        val current = _gameState.value
        if (current.isGameOver) return

        val startSecs = initialSeconds.coerceIn(0, 30)
        _gameState.value = current.copy(turnTimeRemaining = startSecs)

        turnTimerJob = viewModelScope.launch {
            var time = startSecs
            while (time > 0) {
                delay(1000)
                val state = _gameState.value
                if (state.isGameOver) break
                time -= 1
                _gameState.value = _gameState.value.copy(turnTimeRemaining = time)
            }
            val finalState = _gameState.value
            if (!finalState.isGameOver && time <= 0) {
                handleTurnTimeout()
            }
        }
    }

    private fun handleTurnTimeout() {
        turnTimerJob?.cancel()
        val current = _gameState.value
        if (current.isGameOver) return

        val timedOutPlayer = current.activePlayer
        val winnerSymbol = timedOutPlayer.other()

        var newOScore = current.playerOScore
        var newXScore = current.playerXScore

        if (winnerSymbol == Symbol.O) {
            newOScore++
            viewModelScope.launch { repository.incrementPlayerOWins() }
        } else {
            newXScore++
            viewModelScope.launch { repository.incrementPlayerXWins() }
        }

        if (current.gameMode == GameMode.VS_AI) {
            if (winnerSymbol == Symbol.O) soundManager.playWin() else soundManager.playLose()
        } else {
            soundManager.playWin()
        }
        soundManager.triggerHapticWin()

        if (current.gameMode == GameMode.ONLINE_MULTIPLAYER && current.onlineRoomCode != null) {
            val strBoard = current.board.map { it?.name ?: "" }
            firebaseManager.makeMove(
                roomCode = current.onlineRoomCode,
                board = strBoard,
                nextPlayer = timedOutPlayer.name,
                winner = winnerSymbol.name,
                isDraw = false,
                scoreO = newOScore,
                scoreX = newXScore
            )
        }

        _gameState.value = current.copy(
            isGameOver = true,
            winner = winnerSymbol,
            isDraw = false,
            playerOScore = newOScore,
            playerXScore = newXScore,
            turnTimeRemaining = 0
        )
    }

    private fun cancelAiJob() {
        aiJob?.cancel()
        aiJob = null
    }

    private fun resetBoardForGridSize(size: Int) {
        cancelAiJob()
        turnTimerJob?.cancel()
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
            isAiThinking = false,
            turnTimeRemaining = 30
        )
        startTurnTimer()
    }

    // --- ONLINE MULTIPLAYER ACTIONS ---

    fun createOnlineRoom(gridSize: Int = 3, hostName: String = "Player 1", onCodeGenerated: (String) -> Unit) {
        val roomCode = firebaseManager.generate3DigitCode()
        val validSize = if (gridSize in 3..7) gridSize else 3
        val totalCells = validSize * validSize
        val targetStreak = GameEngine.defaultStreakTarget(validSize)
        val cleanHostName = hostName.trim().ifBlank { "Player 1" }
        viewModelScope.launch { repository.setGameMode(GameMode.ONLINE_MULTIPLAYER) }
        firebaseManager.createRoom(
            roomCode = roomCode,
            gridSize = validSize,
            hostName = cleanHostName,
            onSuccess = {
                _gameState.value = _gameState.value.copy(
                    gridSize = validSize,
                    winningStreakTarget = targetStreak,
                    gameMode = GameMode.ONLINE_MULTIPLAYER,
                    onlineRoomCode = roomCode,
                    isOnlineHost = true,
                    myOnlineSymbol = Symbol.O,
                    playerOName = cleanHostName,
                    playerXName = "Player 2",
                    playerOScore = 0,
                    playerXScore = 0,
                    onlineStatus = "Waiting for Player B (Code: $roomCode)...",
                    board = List(totalCells) { null },
                    activePlayer = Symbol.O,
                    isGameOver = false,
                    winner = null,
                    isDraw = false,
                    chatMessages = emptyList(),
                    isChatOpen = false,
                    unreadChatCount = 0,
                    latestChatToast = null
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

    fun joinOnlineRoom(roomCode: String, guestName: String = "Player 2", onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch { repository.setGameMode(GameMode.ONLINE_MULTIPLAYER) }
        val cleanGuestName = guestName.trim().ifBlank { "Player 2" }
        firebaseManager.joinRoom(
            roomCode = roomCode,
            guestName = cleanGuestName,
            onSuccess = { roomData ->
                val roomGridSize = if (roomData.gridSize in 3..7) roomData.gridSize else 3
                val totalCells = roomGridSize * roomGridSize
                val targetStreak = GameEngine.defaultStreakTarget(roomGridSize)

                _gameState.value = _gameState.value.copy(
                    gridSize = roomGridSize,
                    winningStreakTarget = targetStreak,
                    gameMode = GameMode.ONLINE_MULTIPLAYER,
                    onlineRoomCode = roomCode,
                    isOnlineHost = false,
                    myOnlineSymbol = Symbol.X,
                    playerOName = roomData.hostName.ifBlank { "Player 1" },
                    playerXName = cleanGuestName,
                    playerOScore = roomData.scoreO,
                    playerXScore = roomData.scoreX,
                    onlineStatus = "Connected to Room $roomCode!",
                    board = List(totalCells) { null },
                    activePlayer = Symbol.O,
                    isGameOver = false,
                    winner = null,
                    isDraw = false,
                    chatMessages = emptyList(),
                    isChatOpen = false,
                    unreadChatCount = 0,
                    latestChatToast = null
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

    fun leaveOnlineRoom() {
        val current = _gameState.value
        val roomCode = current.onlineRoomCode
        if (roomCode != null) {
            firebaseManager.leaveRoom(roomCode, isHost = current.isOnlineHost)
            webRtcCallManager.stopCall()
        }
        onlineObserverJob?.cancel()
        onlineObserverJob = null

        _gameState.value = current.copy(
            onlineRoomCode = null,
            onlineStatus = "",
            gameMode = GameMode.VS_PLAYER
        )
    }

    fun exitGameToMenu() {
        turnTimerJob?.cancel()
        cancelAiJob()
        if (_gameState.value.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            leaveOnlineRoom()
        }
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
            isAiThinking = false,
            turnTimeRemaining = 30
        )
    }

    private fun startObservingOnlineRoom(roomCode: String) {
        onlineObserverJob?.cancel()
        onlineObserverJob = viewModelScope.launch {
            firebaseManager.observeRoom(roomCode).collect { roomData ->
                val current = _gameState.value
                if (roomData == null) {
                    if (current.gameMode == GameMode.ONLINE_MULTIPLAYER && current.onlineRoomCode == roomCode) {
                        webRtcCallManager.stopCall()
                        _gameState.value = current.copy(
                            onlineRoomCode = null,
                            onlineStatus = if (current.isOnlineHost) "" else "Host closed the room.",
                            gameMode = GameMode.VS_PLAYER
                        )
                    }
                    return@collect
                }
                updateFromOnlineRoomData(roomData)
            }
        }
    }

    private fun updateFromOnlineRoomData(roomData: OnlineRoomData) {
        val current = _gameState.value
        val roomGridSize = if (roomData.gridSize in 3..7) roomData.gridSize else 3
        val totalCells = roomGridSize * roomGridSize
        val targetStreak = GameEngine.defaultStreakTarget(roomGridSize)

        val newBoard = if (roomData.board.size == totalCells) {
            roomData.board.map {
                when (it) {
                    "O" -> Symbol.O
                    "X" -> Symbol.X
                    else -> null
                }
            }
        } else {
            List(totalCells) { null }
        }

        val activeSymbol = if (roomData.activePlayer == "X") Symbol.X else Symbol.O
        val winningLine = GameEngine.checkWin(newBoard, roomGridSize, targetStreak)
        val isDraw = roomData.isDraw || GameEngine.checkDraw(newBoard, winningLine)
        val winnerSymbol = when (roomData.winner) {
            "O" -> Symbol.O
            "X" -> Symbol.X
            else -> winningLine?.winner
        }
        val isGameOver = winnerSymbol != null || isDraw

        val opponentMuted = if (current.isOnlineHost) roomData.guestMutedMic else roomData.hostMutedMic

        val hostName = roomData.hostName.ifBlank { "Player 1" }
        val guestName = roomData.guestName.ifBlank { "Player 2" }

        val statusText = when {
            roomData.status == "WAITING" -> "Waiting for Player B (Code: ${roomData.roomCode})..."
            isGameOver -> {
                if (winnerSymbol != null) {
                    if (winnerSymbol == current.myOnlineSymbol) "YOU WIN!" else "YOU LOSE"
                } else "DRAW GAME!"
            }
            activeSymbol == current.myOnlineSymbol -> "YOUR TURN (${current.myOnlineSymbol.name})"
            else -> "OPPONENT'S TURN (${if (current.myOnlineSymbol == Symbol.O) guestName else hostName})"
        }

        // Parse Chat Messages
        val parsedMessages = roomData.chatMessages.values.mapNotNull { msgMap ->
            val id = msgMap["id"] ?: return@mapNotNull null
            val senderStr = msgMap["sender"] ?: "O"
            val text = msgMap["text"] ?: return@mapNotNull null
            val ts = msgMap["timestamp"]?.toLongOrNull() ?: 0L
            val symbol = if (senderStr == "X") Symbol.X else Symbol.O
            ChatMessage(
                id = id,
                senderName = if (symbol == Symbol.O) hostName else guestName,
                senderSymbol = symbol,
                text = text,
                timestamp = ts
            )
        }.sortedBy { it.timestamp }

        val previousCount = current.chatMessages.size
        val newCount = parsedMessages.size
        val hasNewMessages = newCount > previousCount

        val unread = if (hasNewMessages && !current.isChatOpen) {
            current.unreadChatCount + (newCount - previousCount)
        } else if (current.isChatOpen) {
            0
        } else {
            current.unreadChatCount
        }

        val latestToast = if (hasNewMessages && parsedMessages.isNotEmpty()) {
            val lastMsg = parsedMessages.last()
            if (lastMsg.senderSymbol != current.myOnlineSymbol) lastMsg else current.latestChatToast
        } else {
            current.latestChatToast
        }

        val elapsedSecs = ((System.currentTimeMillis() - roomData.turnStartTime) / 1000).toInt()
        val remainingSecs = (30 - elapsedSecs).coerceAtLeast(0)

        val previousBoard = current.board
        val previousActivePlayer = current.activePlayer

        _gameState.value = current.copy(
            gridSize = roomGridSize,
            winningStreakTarget = targetStreak,
            playerOName = hostName,
            playerXName = guestName,
            playerOScore = roomData.scoreO,
            playerXScore = roomData.scoreX,
            board = newBoard,
            activePlayer = activeSymbol,
            winningLine = winningLine,
            isGameOver = isGameOver,
            winner = winnerSymbol,
            isDraw = isDraw,
            onlineStatus = statusText,
            opponentMutedMic = opponentMuted,
            chatMessages = parsedMessages,
            unreadChatCount = unread,
            latestChatToast = latestToast,
            turnTimeRemaining = remainingSecs
        )

        if (isGameOver) {
            turnTimerJob?.cancel()
        } else if (roomData.status == "PLAYING") {
            if (remainingSecs <= 0) {
                if (activeSymbol == current.myOnlineSymbol || current.isOnlineHost) {
                    handleTurnTimeout()
                }
            } else if (previousBoard != newBoard || previousActivePlayer != activeSymbol || turnTimerJob?.isActive != true) {
                startTurnTimerForOnline(remainingSecs)
            }
        }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _gameState.value
        val roomCode = current.onlineRoomCode ?: return
        val mySymbolStr = current.myOnlineSymbol.name
        firebaseManager.sendChatMessage(roomCode, mySymbolStr, trimmed)
    }

    fun toggleChatWindow() {
        val current = _gameState.value
        val open = !current.isChatOpen
        _gameState.value = current.copy(
            isChatOpen = open,
            unreadChatCount = if (open) 0 else current.unreadChatCount
        )
    }

    fun clearChatToast() {
        _gameState.value = _gameState.value.copy(latestChatToast = null)
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

            val winningLine = GameEngine.checkWin(newBoardList, current.gridSize, current.winningStreakTarget)
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

        if (isGameOver) {
            turnTimerJob?.cancel()
        } else {
            startTurnTimer()
        }

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
            isAiThinking = false,
            turnTimeRemaining = 30
        )
        startTurnTimer()
    }

    fun provideHint() {
        val state = _gameState.value
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER || state.isGameOver || state.isAiThinking) return

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
        turnTimerJob?.cancel()
        soundManager.playTap()
        val current = _gameState.value
        val totalCells = current.gridSize * current.gridSize
        if (current.gameMode == GameMode.ONLINE_MULTIPLAYER && current.onlineRoomCode != null) {
            val emptyBoard = List(totalCells) { "" }
            firebaseManager.makeMove(
                roomCode = current.onlineRoomCode,
                board = emptyBoard,
                nextPlayer = "O",
                winner = null,
                isDraw = false,
                scoreO = current.playerOScore,
                scoreX = current.playerXScore
            )
            return
        }

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
            isAiThinking = false,
            turnTimeRemaining = 30
        )
        startTurnTimer()
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
