package com.example.logic

object GameEngine {

    fun defaultStreakTarget(gridSize: Int): Int = when (gridSize) {
        3 -> 3
        4 -> 4
        5 -> 4
        6 -> 5
        7 -> 5
        else -> 3
    }

    fun checkWin(board: List<Symbol?>, gridSize: Int, streakTarget: Int = defaultStreakTarget(gridSize)): WinningLine? {
        val totalCells = gridSize * gridSize
        if (board.size != totalCells) return null

        // Directions: (deltaRow, deltaCol)
        val directions = listOf(
            Pair(0, 1),   // Horizontal ->
            Pair(1, 0),   // Vertical v
            Pair(1, 1),   // Diagonal Down-Right \
            Pair(-1, 1)   // Diagonal Up-Right /
        )

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val symbol = board[row * gridSize + col] ?: continue

                for ((dRow, dCol) in directions) {
                    val endRow = row + (streakTarget - 1) * dRow
                    val endCol = col + (streakTarget - 1) * dCol

                    // Check bounds for the full streak
                    if (endRow in 0 until gridSize && endCol in 0 until gridSize) {
                        var isStreak = true
                        val indices = ArrayList<Int>(streakTarget)

                        for (k in 0 until streakTarget) {
                            val r = row + k * dRow
                            val c = col + k * dCol
                            val idx = r * gridSize + c
                            indices.add(idx)
                            if (board[idx] != symbol) {
                                isStreak = false
                                break
                            }
                        }

                        if (isStreak) {
                            val startIdx = row * gridSize + col
                            val endIdx = (row + (streakTarget - 1) * dRow) * gridSize + (col + (streakTarget - 1) * dCol)
                            return WinningLine(
                                startIndex = startIdx,
                                endIndex = endIdx,
                                winner = symbol,
                                winningIndices = indices
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    fun checkDraw(board: List<Symbol?>, winningLine: WinningLine?): Boolean {
        return winningLine == null && board.none { it == null }
    }

    fun getEmptyIndices(board: List<Symbol?>): List<Int> {
        val empty = mutableListOf<Int>()
        for (i in board.indices) {
            if (board[i] == null) empty.add(i)
        }
        return empty
    }
}
