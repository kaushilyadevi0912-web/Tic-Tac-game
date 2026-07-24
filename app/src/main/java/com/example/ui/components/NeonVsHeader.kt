package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.GameMode
import com.example.logic.Symbol
import com.example.ui.theme.*

@Composable
fun NeonVsHeader(
    activePlayer: Symbol,
    gameMode: GameMode,
    playerOScore: Int,
    playerXScore: Int,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "turn_pulse")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player O Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width * 0.38f

                        if (activePlayer == Symbol.O) {
                            drawCircle(
                                color = NeonPlayerRed.copy(alpha = auraAlpha * 0.4f),
                                radius = radius * 1.3f
                            )
                        }

                        // Outer glow
                        drawCircle(
                            color = NeonPlayerOrange.copy(alpha = 0.6f),
                            radius = radius,
                            style = Stroke(width = 9f)
                        )
                        // Inner core
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            style = Stroke(width = 4f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Player",
                    color = NeonPlayerOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // VS Label
            Text(
                text = "vs",
                color = NeonPlayerCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Player X / AI Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val padding = width * 0.22f

                        if (activePlayer == Symbol.X) {
                            drawCircle(
                                color = NeonPlayerCyan.copy(alpha = auraAlpha * 0.4f),
                                radius = width * 0.48f
                            )
                        }

                        // Outer glow cross
                        drawLine(
                            color = NeonPlayerCyan.copy(alpha = 0.6f),
                            start = Offset(padding, padding),
                            end = Offset(width - padding, height - padding),
                            strokeWidth = 9f
                        )
                        drawLine(
                            color = NeonPlayerCyan.copy(alpha = 0.6f),
                            start = Offset(width - padding, padding),
                            end = Offset(padding, height - padding),
                            strokeWidth = 9f
                        )

                        // Inner core cross
                        drawLine(
                            color = Color.White,
                            start = Offset(padding, padding),
                            end = Offset(width - padding, height - padding),
                            strokeWidth = 4f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(width - padding, padding),
                            end = Offset(padding, height - padding),
                            strokeWidth = 4f
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (gameMode == GameMode.VS_AI) "AI" else "Player 2",
                    color = NeonPlayerCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Linked Round Score Circles Bar (as in Screenshots 2, 3, 4, 5)
        LinkedScoreDots(
            dotCount = gridSize.coerceIn(3, 7),
            playerOScore = playerOScore,
            playerXScore = playerXScore
        )
    }
}

@Composable
fun LinkedScoreDots(
    dotCount: Int,
    playerOScore: Int,
    playerXScore: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth(0.65f)
            .height(30.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val step = width / (dotCount - 1)

        // Draw horizontal connecting line through circles
        drawLine(
            color = NeonPlayerOrange,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.5f
        )

        // Draw linked circles
        for (i in 0 until dotCount) {
            val cx = i * step
            val isWonByO = i < playerOScore
            val isWonByX = i >= (dotCount - playerXScore)

            val circleColor = when {
                isWonByO -> NeonPlayerOrange
                isWonByX -> NeonPlayerCyan
                else -> NeonPlayerOrange
            }

            // Glow ring
            drawCircle(
                color = circleColor,
                radius = 11f,
                center = Offset(cx, centerY),
                style = Stroke(width = 3.5f)
            )

            // Inner fill if won
            if (isWonByO || isWonByX) {
                drawCircle(
                    color = circleColor,
                    radius = 6f,
                    center = Offset(cx, centerY)
                )
            } else {
                drawCircle(
                    color = NeonBackgroundDark,
                    radius = 8f,
                    center = Offset(cx, centerY)
                )
            }
        }
    }
}
