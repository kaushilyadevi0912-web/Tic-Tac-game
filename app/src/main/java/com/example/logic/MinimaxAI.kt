package com.example.logic

import kotlin.random.Random

object MinimaxAI {

    fun findBestMove(
        board: List<Symbol?>,
        gridSize: Int,
        aiSymbol: Symbol,
        difficulty: AiDifficulty,
        streakTarget: Int = GameEngine.defaultStreakTarget(gridSize)
    ): Int {
        val emptyIndices = GameEngine.getEmptyIndices(board)
        if (emptyIndices.isEmpty()) return -1

        // EASY difficulty: Random move
        if (difficulty == AiDifficulty.EASY) {
            return emptyIndices.random()
        }

        // MEDIUM difficulty: 50% chance random, 50% best move
        if (difficulty == AiDifficulty.MEDIUM && Random.nextFloat() < 0.45f) {
            return emptyIndices.random()
        }

        // HARD or MEDIUM (when non-random): Best Move Calculation
        if (gridSize == 3) {
            return minimax3x3(board, aiSymbol)
        } else {
            return heuristicSearch(board, gridSize, aiSymbol, streakTarget)
        }
    }

    private fun minimax3x3(board: List<Symbol?>, aiSymbol: Symbol): Int {
        var bestScore = Int.MIN_VALUE
        var bestMove = -1
        val emptyIndices = GameEngine.getEmptyIndices(board)
        val humanSymbol = aiSymbol.other()

        for (idx in emptyIndices) {
            val mutableBoard = board.toMutableList()
            mutableBoard[idx] = aiSymbol

            val score = minimax(
                board = mutableBoard,
                depth = 0,
                isMaximizing = false,
                aiSymbol = aiSymbol,
                humanSymbol = humanSymbol,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = idx
            }
        }

        return if (bestMove != -1) bestMove else emptyIndices.random()
    }

    private fun minimax(
        board: MutableList<Symbol?>,
        depth: Int,
        isMaximizing: Boolean,
        aiSymbol: Symbol,
        humanSymbol: Symbol,
        alpha: Int,
        beta: Int
    ): Int {
        val winLine = GameEngine.checkWin(board, 3, 3)
        if (winLine != null) {
            return if (winLine.winner == aiSymbol) 10 - depth else depth - 10
        }
        if (board.none { it == null }) return 0

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (i in board.indices) {
                if (board[i] == null) {
                    board[i] = aiSymbol
                    val eval = minimax(board, depth + 1, false, aiSymbol, humanSymbol, currentAlpha, currentBeta)
                    board[i] = null
                    maxEval = maxOf(maxEval, eval)
                    currentAlpha = maxOf(currentAlpha, eval)
                    if (currentBeta <= currentAlpha) break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (i in board.indices) {
                if (board[i] == null) {
                    board[i] = humanSymbol
                    val eval = minimax(board, depth + 1, true, aiSymbol, humanSymbol, currentAlpha, currentBeta)
                    board[i] = null
                    minEval = minOf(minEval, eval)
                    currentBeta = minOf(currentBeta, eval)
                    if (currentBeta <= currentAlpha) break
                }
            }
            return minEval
        }
    }

    private fun heuristicSearch(
        board: List<Symbol?>,
        gridSize: Int,
        aiSymbol: Symbol,
        streakTarget: Int
    ): Int {
        val emptyIndices = GameEngine.getEmptyIndices(board)
        val humanSymbol = aiSymbol.other()

        // 1. Can AI win immediately?
        for (idx in emptyIndices) {
            val testBoard = board.toMutableList()
            testBoard[idx] = aiSymbol
            if (GameEngine.checkWin(testBoard, gridSize, streakTarget) != null) {
                return idx
            }
        }

        // 2. Can Human win immediately? Block it!
        for (idx in emptyIndices) {
            val testBoard = board.toMutableList()
            testBoard[idx] = humanSymbol
            if (GameEngine.checkWin(testBoard, gridSize, streakTarget) != null) {
                return idx
            }
        }

        // 3. Can AI create a 1-move setup (streakTarget - 1)?
        for (idx in emptyIndices) {
            val testBoard = board.toMutableList()
            testBoard[idx] = aiSymbol
            if (GameEngine.checkWin(testBoard, gridSize, streakTarget - 1) != null) {
                return idx
            }
        }

        // 4. Prefer center & neighboring cells
        val center = gridSize / 2
        val centerIdx = center * gridSize + center
        if (board[centerIdx] == null) return centerIdx

        // Sort empty cells by proximity to existing moves
        val scoredMoves = emptyIndices.map { idx ->
            val row = idx / gridSize
            val col = idx % gridSize
            var score = 0

            // Distance to center bonus
            val distToCenter = Math.abs(row - center) + Math.abs(col - center)
            score -= distToCenter * 2

            // Proximity to neighboring pieces
            for (r in maxOf(0, row - 1)..minOf(gridSize - 1, row + 1)) {
                for (c in maxOf(0, col - 1)..minOf(gridSize - 1, col + 1)) {
                    val neighborIdx = r * gridSize + c
                    if (board[neighborIdx] == aiSymbol) score += 5
                    if (board[neighborIdx] == humanSymbol) score += 3
                }
            }
            Pair(idx, score)
        }

        return scoredMoves.maxByOrNull { it.second }?.first ?: emptyIndices.random()
    }
}
