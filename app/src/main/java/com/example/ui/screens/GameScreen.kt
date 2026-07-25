package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.GameMode
import com.example.logic.GameState
import com.example.ui.components.NeonBoard
import com.example.ui.components.NeonBottomBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.NeonVsHeader
import com.example.ui.theme.*

@Composable
fun GameScreen(
    gameState: GameState,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleMic: () -> Unit,
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

        // Audio & Voice Chat Controls Bar (Mute/Unmute Game SFX and Microphone)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute Game Sound Effects
            AudioControlButton(
                isMuted = !isSoundEnabled,
                activeIcon = Icons.Rounded.VolumeUp,
                mutedIcon = Icons.Rounded.VolumeOff,
                label = if (isSoundEnabled) "Sound On" else "Sound Off",
                activeColor = NeonPlayerCyan,
                onClick = onToggleSound
            )

            if (gameState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                // Online Room Code & Status Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ROOM: ${gameState.onlineRoomCode ?: "---"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonPlayerOrange
                    )
                    Text(
                        text = gameState.onlineStatus,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Mute / Unmute WebRTC Voice Chat Microphone
                AudioControlButton(
                    isMuted = gameState.isMicMuted,
                    activeIcon = Icons.Rounded.Mic,
                    mutedIcon = Icons.Rounded.MicOff,
                    label = if (!gameState.isMicMuted) "Mic On" else "Mic Muted",
                    activeColor = NeonPlayerOrange,
                    onClick = onToggleMic
                )
            }
        }

        // Player vs AI / Online Header
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

@Composable
fun AudioControlButton(
    isMuted: Boolean,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    mutedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isMuted) Color(0x33FF3B30) else activeColor.copy(alpha = 0.2f),
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isMuted) NeonPlayerRed else activeColor,
        label = "borderColor"
    )

    Row(
        modifier = Modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = borderColor)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isMuted) mutedIcon else activeIcon,
            contentDescription = label,
            tint = if (isMuted) NeonPlayerRed else activeColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
