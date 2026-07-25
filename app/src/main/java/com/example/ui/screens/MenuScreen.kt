package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.GameMode
import com.example.ui.components.NeonTopBar
import com.example.ui.theme.*

import androidx.compose.material.icons.rounded.Wifi

@Composable
fun MenuScreen(
    currentGridSize: Int,
    onSelectMode: (GameMode) -> Unit,
    onSelectGridSize: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnlineRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Bar
        NeonTopBar(
            onBackClick = null,
            onSettingsClick = onOpenSettings
        )

        // Title Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "TIC TAC TOE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NeonPlayerOrange
            )
        }

        // Central Mini Board Graphic Preview (Matching Screenshot 1)
        Box(
            modifier = Modifier
                .size(190.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = NeonBoardBorder)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x88120A2C))
                .border(3.dp, NeonBoardBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val cW = width / 3f
                val cH = height / 3f

                // Grid lines
                val gridColor = Color(0x44A200FF)
                drawLine(color = gridColor, start = Offset(cW, 0f), end = Offset(cW, height), strokeWidth = 2f)
                drawLine(color = gridColor, start = Offset(cW * 2, 0f), end = Offset(cW * 2, height), strokeWidth = 2f)
                drawLine(color = gridColor, start = Offset(0f, cH), end = Offset(width, cH), strokeWidth = 2f)
                drawLine(color = gridColor, start = Offset(0f, cH * 2), end = Offset(width, cH * 2), strokeWidth = 2f)

                // Symbols (matching Screenshot 1 preview)
                // O at (0,0), O at (1,1), O at (2,0)
                fun drawO(r: Int, c: Int) {
                    val center = Offset(c * cW + cW / 2f, r * cH + cH / 2f)
                    val rad = cW * 0.32f
                    drawCircle(color = NeonPlayerOrange.copy(alpha = 0.5f), radius = rad, center = center, style = Stroke(6f))
                    drawCircle(color = NeonPlayerOrange, radius = rad, center = center, style = Stroke(4f))
                    drawCircle(color = Color.White, radius = rad, center = center, style = Stroke(2f))
                }

                fun drawX(r: Int, c: Int) {
                    val cx = c * cW + cW / 2f
                    val cy = r * cH + cH / 2f
                    val pad = cW * 0.22f
                    val p1 = Offset(cx - pad, cy - pad)
                    val p2 = Offset(cx + pad, cy + pad)
                    val p3 = Offset(cx + pad, cy - pad)
                    val p4 = Offset(cx - pad, cy + pad)

                    drawLine(color = NeonPlayerCyan.copy(alpha = 0.5f), start = p1, end = p2, strokeWidth = 6f)
                    drawLine(color = NeonPlayerCyan.copy(alpha = 0.5f), start = p3, end = p4, strokeWidth = 6f)
                    drawLine(color = NeonPlayerCyan, start = p1, end = p2, strokeWidth = 4f)
                    drawLine(color = NeonPlayerCyan, start = p3, end = p4, strokeWidth = 4f)
                    drawLine(color = Color.White, start = p1, end = p2, strokeWidth = 2f)
                    drawLine(color = Color.White, start = p3, end = p4, strokeWidth = 2f)
                }

                drawO(0, 0)
                drawX(0, 1)
                drawO(0, 2)
                drawX(1, 0)
                drawO(1, 1)
                drawX(1, 2)
                drawO(2, 0)
                drawX(2, 1)
                drawX(2, 2)

                // Diagonal winning line through O's from bottom-left to top-right
                val start = Offset(0 * cW + cW / 2f, 2 * cH + cH / 2f)
                val end = Offset(2 * cW + cW / 2f, 0 * cH + cH / 2f)

                drawLine(color = NeonPlayerOrange.copy(alpha = 0.6f), start = start, end = end, strokeWidth = 10f)
                drawLine(color = NeonPlayerOrange, start = start, end = end, strokeWidth = 6f)
                drawLine(color = Color.White, start = start, end = end, strokeWidth = 3f)

                drawCircle(color = NeonPlayerOrange, radius = 8f, center = start)
                drawCircle(color = Color.White, radius = 4f, center = start)
                drawCircle(color = NeonPlayerOrange, radius = 8f, center = end)
                drawCircle(color = Color.White, radius = 4f, center = end)
            }
        }

        // Grid Size Selector Pills
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = "BOARD SIZE",
                color = NeonTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(3, 4, 5, 6, 7).forEach { size ->
                    val isSelected = currentGridSize == size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonPlayerCyan.copy(alpha = 0.25f) else Color(0x33140D2E))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonPlayerCyan else NeonButtonBorder.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectGridSize(size) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${size}x${size}",
                            color = if (isSelected) NeonPlayerCyan else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Main Mode Buttons (Matching Screenshot 1: 👤 VS 💻 and 👤 VS 👤)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player vs AI Button
            NeonModeButton(
                iconLeft = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                vsText = "VS",
                iconRight = { Icon(Icons.Rounded.Computer, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                onClick = { onSelectMode(GameMode.VS_AI) }
            )

            // Player vs Player Local Button
            NeonModeButton(
                iconLeft = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                vsText = "VS",
                iconRight = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                onClick = { onSelectMode(GameMode.VS_PLAYER) }
            )

            // Online Room Button
            NeonModeButton(
                iconLeft = { Icon(Icons.Rounded.Person, contentDescription = null, tint = NeonPlayerCyan, modifier = Modifier.size(28.dp)) },
                vsText = "ONLINE",
                iconRight = { Icon(Icons.Rounded.Wifi, contentDescription = null, tint = NeonPlayerCyan, modifier = Modifier.size(28.dp)) },
                onClick = onOpenOnlineRoom
            )
        }
    }
}

@Composable
fun NeonModeButton(
    iconLeft: @Composable () -> Unit,
    vsText: String,
    iconRight: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp), spotColor = NeonButtonBorder)
            .clip(RoundedCornerShape(22.dp))
            .background(NeonButtonBg)
            .border(2.5.dp, NeonButtonBorder, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconLeft()
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = vsText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(18.dp))
            iconRight()
        }
    }
}
