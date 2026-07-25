package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.*
import kotlin.math.pow

@Composable
fun SynthwaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid_anim")
    val gridOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070414),
                        Color(0xFF13092A),
                        Color(0xFF1E0A3C),
                        Color(0xFF09041A)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridOffset = gridOffsetState.value
            val width = size.width
            val height = size.height
            val horizonY = height * 0.65f

            // 1. Draw Horizon Sun Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xBBFF0080),
                        Color(0x777A00FF),
                        Color.Transparent
                    ),
                    center = Offset(width / 2f, horizonY),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width / 2f, horizonY)
            )

            // 2. Draw Distant Mountain Silhouettes
            val mountainPath = Path().apply {
                moveTo(0f, horizonY)
                lineTo(width * 0.12f, horizonY - 35f)
                lineTo(width * 0.22f, horizonY - 15f)
                lineTo(width * 0.35f, horizonY - 50f)
                lineTo(width * 0.48f, horizonY - 20f)
                lineTo(width * 0.62f, horizonY - 55f)
                lineTo(width * 0.78f, horizonY - 25f)
                lineTo(width * 0.90f, horizonY - 40f)
                lineTo(width, horizonY - 10f)
                lineTo(width, horizonY)
                close()
            }
            drawPath(
                path = mountainPath,
                color = Color(0xFF12082C)
            )
            drawPath(
                path = mountainPath,
                color = Color(0x66FF00A0),
                style = Stroke(width = 2f)
            )

            // 3. Draw Perspective Synthwave Grid Floor
            val gridColor = Color(0x55A822FF)
            val vanishingX = width / 2f

            // Vertical perspective lines
            val lineCount = 18
            for (i in -lineCount / 2..lineCount / 2) {
                val startX = vanishingX + (i * 12f)
                val endX = vanishingX + (i * (width / 5f))
                drawLine(
                    color = gridColor,
                    start = Offset(startX, horizonY),
                    end = Offset(endX, height),
                    strokeWidth = 1.5f
                )
            }

            // Horizontal grid lines with perspective spacing + subtle animation
            val horizontalLineCount = 14
            for (i in 0 until horizontalLineCount) {
                val progress = ((i + gridOffset) / horizontalLineCount).coerceIn(0f, 1f)
                val lineY = horizonY + (height - horizonY) * progress.pow(2.2f)
                val alpha = (progress * 0.7f).coerceIn(0f, 0.7f)

                drawLine(
                    color = Color(0xFFBD22FF).copy(alpha = alpha),
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = (1f + progress * 2.5f)
                )
            }

            // Top starfield particles
            val stars = listOf(
                Offset(width * 0.15f, height * 0.10f),
                Offset(width * 0.85f, height * 0.18f),
                Offset(width * 0.30f, height * 0.25f),
                Offset(width * 0.70f, height * 0.08f),
                Offset(width * 0.50f, height * 0.15f),
                Offset(width * 0.20f, height * 0.40f),
                Offset(width * 0.80f, height * 0.35f)
            )
            for (star in stars) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 2f,
                    center = star
                )
            }
        }

        content()
    }
}
