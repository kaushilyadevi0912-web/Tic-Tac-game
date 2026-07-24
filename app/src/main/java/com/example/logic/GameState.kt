package com.example.logic

enum class Symbol {
    O, X;

    fun other(): Symbol = if (this == O) X else O
}

enum class GameMode {
    VS_AI, VS_PLAYER
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
    val targetWins: Int = 3, // For round dots display like ⊝⊝⊝
    val hintCellIndex: Int? = null,
    val moveHistory: List<Pair<Int, Symbol>> = emptyList(),
    val gameMode: GameMode = GameMode.VS_AI,
    val aiDifficulty: AiDifficulty = AiDifficulty.HARD,
    val playerOIsHuman: Boolean = true,
    val playerXIsHuman: Boolean = false,
    val isAiThinking: Boolean = false
)
