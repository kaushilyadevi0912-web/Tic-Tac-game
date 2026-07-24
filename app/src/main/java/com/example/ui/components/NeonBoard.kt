package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.logic.GameMode
import com.example.logic.Symbol
import com.example.logic.WinningLine
import com.example.ui.theme.*

@Composable
fun NeonBoard(
    gridSize: Int,
    board: List<Symbol?>,
    winningLine: WinningLine?,
    hintCellIndex: Int?,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isAiThinking: Boolean = false,
    gameMode: GameMode = GameMode.VS_AI,
    activePlayer: Symbol = Symbol.O,
    isGameOver: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), spotColor = NeonBoardBorder)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x88120A2C))
            .border(3.dp, NeonBoardBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        val boardWidth = constraints.maxWidth.toFloat()
        val cellSize = boardWidth / gridSize

        // 1. Grid Canvas (Grid lines + Winning Line)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Grid Lines
            val gridColor = Color(0x44A200FF)
            val strokeW = 1.5f

            for (i in 1 until gridSize) {
                // Vertical lines
                drawLine(
                    color = gridColor,
                    start = Offset(i * cellSize, 0f),
                    end = Offset(i * cellSize, boardWidth),
                    strokeWidth = strokeW
                )
                // Horizontal lines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, i * cellSize),
                    end = Offset(boardWidth, i * cellSize),
                    strokeWidth = strokeW
                )
            }
        }

        // 2. Cells Grid
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until gridSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (c in 0 until gridSize) {
                        val index = r * gridSize + c
                        val symbol = board.getOrNull(index)
                        val isHint = hintCellIndex == index
                        val isCellClickable = !isAiThinking &&
                                (gameMode != GameMode.VS_AI || activePlayer == Symbol.O) &&
                                symbol == null &&
                                !isGameOver &&
                                winningLine == null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("cell_$index")
                                .clickable(
                                    enabled = isCellClickable,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onCellClick(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isHint) {
                                HintCellRing()
                            }
                            if (symbol != null) {
                                AnimatedSymbolCell(symbol = symbol)
                            }
                        }
                    }
                }
            }
        }

        // 3. Winning Line Overlay (Animated beam with circular endpoints)
        if (winningLine != null) {
            WinningLineCanvas(
                winningLine = winningLine,
                gridSize = gridSize,
                cellSize = cellSize
            )
        }
    }
}

@Composable
fun AnimatedSymbolCell(
    symbol: Symbol,
    modifier: Modifier = Modifier
) {
    val animScale = remember { Animatable(0f) }

    LaunchedEffect(symbol) {
        animScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize(0.72f)
    ) {
        val width = size.width * animScale.value
        val height = size.height * animScale.value
        val center = Offset(size.width / 2f, size.height / 2f)

        if (symbol == Symbol.O) {
            val radius = width * 0.40f
            // Glow layer
            drawCircle(
                color = NeonPlayerOrange.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 10f)
            )
            // Outer colored stroke
            drawCircle(
                color = NeonPlayerOrange,
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )
            // Inner white core
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 3f)
            )
        } else {
            val padding = width * 0.12f
            val startX = center.x - (width / 2f - padding)
            val endX = center.x + (width / 2f - padding)
            val startY = center.y - (height / 2f - padding)
            val endY = center.y + (height / 2f - padding)

            // Outer glow cross
            drawLine(
                color = NeonPlayerCyan.copy(alpha = 0.5f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 10f
            )
            drawLine(
                color = NeonPlayerCyan.copy(alpha = 0.5f),
                start = Offset(endX, startY),
                end = Offset(startX, endY),
                strokeWidth = 10f
            )

            // Outer colored cross
            drawLine(
                color = NeonPlayerCyan,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 6f
            )
            drawLine(
                color = NeonPlayerCyan,
                start = Offset(endX, startY),
                end = Offset(startX, endY),
                strokeWidth = 6f
            )

            // Inner white core
            drawLine(
                color = Color.White,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.White,
                start = Offset(endX, startY),
                end = Offset(startX, endY),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun HintCellRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "hint_ring")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width * 0.42f
        drawCircle(
            color = NeonYellowHint.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun WinningLineCanvas(
    winningLine: WinningLine,
    gridSize: Int,
    cellSize: Float,
    modifier: Modifier = Modifier
) {
    val lineProgress = remember { Animatable(0f) }

    LaunchedEffect(winningLine) {
        lineProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    val startRow = winningLine.startIndex / gridSize
    val startCol = winningLine.startIndex % gridSize
    val endRow = winningLine.endIndex / gridSize
    val endCol = winningLine.endIndex % gridSize

    val startX = startCol * cellSize + cellSize / 2f
    val startY = startRow * cellSize + cellSize / 2f
    val targetEndX = endCol * cellSize + cellSize / 2f
    val targetEndY = endRow * cellSize + cellSize / 2f

    val currentEndX = startX + (targetEndX - startX) * lineProgress.value
    val currentEndY = startY + (targetEndY - startY) * lineProgress.value

    val lineColor = if (winningLine.winner == Symbol.O) NeonPlayerOrange else NeonPlayerCyan

    Canvas(modifier = modifier.fillMaxSize()) {
        val startPt = Offset(startX, startY)
        val currentPt = Offset(currentEndX, currentEndY)

        // 1. Outer Neon Glow Line
        drawLine(
            color = lineColor.copy(alpha = 0.6f),
            start = startPt,
            end = currentPt,
            strokeWidth = 14f
        )

        // 2. Main Color Line
        drawLine(
            color = lineColor,
            start = startPt,
            end = currentPt,
            strokeWidth = 8f
        )

        // 3. Bright White Center Line
        drawLine(
            color = Color.White,
            start = startPt,
            end = currentPt,
            strokeWidth = 4f
        )

        // 4. Circular Endpoint Nodes (matching Screenshots 2, 4, 5!)
        // Start node
        drawCircle(
            color = lineColor,
            radius = 12f,
            center = startPt
        )
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = startPt
        )

        // End node
        if (lineProgress.value > 0.05f) {
            drawCircle(
                color = lineColor,
                radius = 12f,
                center = currentPt
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = currentPt
            )
        }
    }
}
