package com.example.logic

enum class Symbol {
    O, X;

    fun other(): Symbol = if (this == O) X else O
}

enum class GameMode {
    VS_AI, VS_PLAYER, ONLINE_MULTIPLAYER
}

enum class AiDifficulty {
    EASY, MEDIUM, HARD
}

data class WinningLine(
    val startIndex: Int,
    val endIndex: Int,
    val winner: Symbol,
    val winningIndices: List<Int>
)

data class ChatMessage(
    val id: String = "",
    val senderName: String = "",
    val senderSymbol: Symbol = Symbol.O,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val gridSize: Int = 3,
    val winningStreakTarget: Int = 3,
    val board: List<Symbol?> = List(9) { null },
    val activePlayer: Symbol = Symbol.O,
    val winningLine: WinningLine? = null,
    val isGameOver: Boolean = false,
    val winner: Symbol? = null,
    val isDraw: Boolean = false,
    val playerOScore: Int = 0,
    val playerXScore: Int = 0,
    val currentRound: Int = 1,
    val targetWins: Int = 3, // For round dots display
    val hintCellIndex: Int? = null,
    val moveHistory: List<Pair<Int, Symbol>> = emptyList(),
    val gameMode: GameMode = GameMode.VS_AI,
    val aiDifficulty: AiDifficulty = AiDifficulty.HARD,
    val playerOIsHuman: Boolean = true,
    val playerXIsHuman: Boolean = false,
    val playerOName: String = "Player 1",
    val playerXName: String = "Player 2",
    val isAiThinking: Boolean = false,

    // Online Multiplayer & WebRTC Fields
    val onlineRoomCode: String? = null,
    val isOnlineHost: Boolean = true,
    val myOnlineSymbol: Symbol = Symbol.O,
    val onlineStatus: String = "",
    val isMicMuted: Boolean = false,
    val opponentMutedMic: Boolean = false,

    // Chat Fields
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatOpen: Boolean = false,
    val unreadChatCount: Int = 0,
    val latestChatToast: ChatMessage? = null
)
