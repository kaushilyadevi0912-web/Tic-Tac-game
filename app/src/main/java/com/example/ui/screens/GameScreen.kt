package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logic.GameState
import com.example.ui.components.NeonBoard
import com.example.ui.components.NeonBottomBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.NeonVsHeader

@Composable
fun GameScreen(
    gameState: GameState,
    onCellClick: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onHintClick: () -> Unit,
    onRestartClick: () -> Unit,
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        NeonTopBar(
            onBackClick = onBackClick,
            onSettingsClick = onOpenSettings
        )

        // Player vs AI Section Header
        NeonVsHeader(
            activePlayer = gameState.activePlayer,
            gameMode = gameState.gameMode,
            playerOScore = gameState.playerOScore,
            playerXScore = gameState.playerXScore,
            gridSize = gameState.gridSize,
            isAiThinking = gameState.isAiThinking,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Central Game Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            NeonBoard(
                gridSize = gameState.gridSize,
                board = gameState.board,
                winningLine = gameState.winningLine,
                hintCellIndex = gameState.hintCellIndex,
                onCellClick = onCellClick,
                isAiThinking = gameState.isAiThinking,
                gameMode = gameState.gameMode,
                activePlayer = gameState.activePlayer,
                isGameOver = gameState.isGameOver
            )
        }

        // Bottom Controls Bar (Undo ↶, Hint 💡, Restart 🌀)
        NeonBottomBar(
            onUndoClick = onUndoClick,
            onHintClick = onHintClick,
            onRestartClick = onRestartClick
        )
    }
}
