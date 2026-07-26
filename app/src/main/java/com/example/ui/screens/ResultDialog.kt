package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.logic.GameMode
import com.example.logic.Symbol
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun ResultDialog(
    winner: Symbol?,
    isDraw: Boolean,
    gameMode: GameMode,
    playerOScore: Int,
    playerXScore: Int,
    playerOName: String = "Player 1",
    playerXName: String = "Player 2",
    myOnlineSymbol: Symbol = Symbol.O,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animVal by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_anim"
    )

    val isIWinner = gameMode == GameMode.ONLINE_MULTIPLAYER && winner == myOnlineSymbol
    val isILoser = gameMode == GameMode.ONLINE_MULTIPLAYER && winner != null && winner != myOnlineSymbol

    val winnerTitle = when {
        isDraw -> "IT'S A DRAW!"
        gameMode == GameMode.ONLINE_MULTIPLAYER -> {
            if (isIWinner) "YOU WIN!" else "YOU LOSE"
        }
        gameMode == GameMode.VS_AI -> {
            if (winner == Symbol.O) "YOU WIN!" else "AI WINS!"
        }
        else -> {
            if (winner == Symbol.O) "${playerOName.uppercase()} WINS!" else "${playerXName.uppercase()} WINS!"
        }
    }

    val subtitle = when {
        isDraw -> "A close match! Nobody won this round."
        gameMode == GameMode.ONLINE_MULTIPLAYER -> {
            val opponentName = if (myOnlineSymbol == Symbol.O) playerXName else playerOName
            if (isIWinner) "Victory! You defeated $opponentName!" else "$opponentName won this game!"
        }
        gameMode == GameMode.VS_AI -> {
            if (winner == Symbol.O) "Great job! You beat the AI!" else "The AI won this round."
        }
        else -> {
            if (winner == Symbol.O) "$playerOName takes the point!" else "$playerXName takes the point!"
        }
    }

    val bannerColor = when {
        isDraw -> NeonYellowHint
        gameMode == GameMode.ONLINE_MULTIPLAYER -> if (isIWinner) NeonPlayerCyan else NeonPlayerRed
        winner == Symbol.O -> NeonPlayerOrange
        else -> NeonPlayerCyan
    }

    Dialog(onDismissRequest = { /* Modal, must tap button */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(28.dp, RoundedCornerShape(26.dp), spotColor = bannerColor)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF140D2E))
                .border(3.dp, bannerColor, RoundedCornerShape(26.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Neon Sparkle Particles Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val rnd = Random(42)
                for (i in 0..18) {
                    val px = rnd.nextFloat() * size.width
                    val py = (rnd.nextFloat() * size.height + animVal * 120f) % size.height
                    drawCircle(
                        color = bannerColor.copy(alpha = 0.5f),
                        radius = rnd.nextFloat() * 4f + 2f,
                        center = Offset(px, py)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = winnerTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = bannerColor
                )

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Current Score Summary
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$playerOName: $playerOScore", color = NeonPlayerOrange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("VS", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text(
                        if (gameMode == GameMode.VS_AI) "AI: $playerXScore" else "$playerXName: $playerXScore",
                        color = NeonPlayerCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Play Again Button
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bannerColor)
                ) {
                    Text("PLAY AGAIN", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Main Menu Button
                Button(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33A200FF))
                ) {
                    Text("MAIN MENU", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
