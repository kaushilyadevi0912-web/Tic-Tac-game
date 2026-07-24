package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.logic.Symbol
import com.example.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleType {
    CIRCLE, DIAMOND, RING, CROSS, SPARK
}

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Color,
    var alpha: Float,
    var decay: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var type: ParticleType,
    var gravity: Float = 0.15f
)

@Composable
fun ParticleVictoryOverlay(
    isGameOver: Boolean,
    winner: Symbol?,
    isDraw: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isGameOver) return

    val particles = remember { mutableStateListOf<Particle>() }

    val winnerColors = remember(winner, isDraw) {
        when {
            isDraw -> listOf(
                Color(0xFFFFEE00), Color(0xFFFF9900), Color(0xFF00FFCC), Color(0xFFFFFFFF)
            )
            winner == Symbol.O -> listOf(
                NeonPlayerOrange, Color(0xFFFF007F), Color(0xFFFF66B2), Color(0xFFFFCC00)
            )
            else -> listOf(
                NeonPlayerCyan, Color(0xFF0077FF), Color(0xFF8A2BE2), Color(0xFF00FFFF)
            )
        }
    }

    LaunchedEffect(isGameOver, winner, isDraw) {
        particles.clear()

        fun spawnBurst(centerXFraction: Float, centerYFraction: Float, count: Int) {
            val rnd = Random
            for (i in 0 until count) {
                val angle = rnd.nextFloat() * 2f * Math.PI.toFloat()
                val speed = rnd.nextFloat() * 18f + 4f
                val color = winnerColors[rnd.nextInt(winnerColors.size)]
                val type = ParticleType.values()[rnd.nextInt(ParticleType.values().size)]

                particles.add(
                    Particle(
                        x = centerXFraction,
                        y = centerYFraction,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - (rnd.nextFloat() * 6f),
                        radius = rnd.nextFloat() * 10f + 4f,
                        color = color,
                        alpha = 1f,
                        decay = rnd.nextFloat() * 0.012f + 0.008f,
                        rotation = rnd.nextFloat() * 360f,
                        rotationSpeed = (rnd.nextFloat() - 0.5f) * 12f,
                        type = type,
                        gravity = rnd.nextFloat() * 0.2f + 0.1f
                    )
                )
            }
        }

        // Center primary explosion
        spawnBurst(0.5f, 0.45f, 90)
        // Left & right secondary bursts
        spawnBurst(0.25f, 0.35f, 40)
        spawnBurst(0.75f, 0.35f, 40)

        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { currentTime ->
                val dt = ((currentTime - lastTime) / 1_000_000f).coerceIn(1f, 32f) / 16f
                lastTime = currentTime

                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx * dt / 300f // normalized scale movement
                    p.y += p.vy * dt / 500f
                    p.vy += p.gravity * dt
                    p.vx *= 0.98f
                    p.alpha -= p.decay * dt
                    p.rotation += p.rotationSpeed * dt

                    if (p.alpha <= 0f) {
                        iterator.remove()
                    }
                }

                if (particles.size < 35 && Random.nextFloat() < 0.3f) {
                    val rnd = Random
                    val color = winnerColors[rnd.nextInt(winnerColors.size)]
                    particles.add(
                        Particle(
                            x = rnd.nextFloat(),
                            y = -0.05f,
                            vx = (rnd.nextFloat() - 0.5f) * 3f,
                            vy = rnd.nextFloat() * 5f + 3f,
                            radius = rnd.nextFloat() * 8f + 3f,
                            color = color,
                            alpha = rnd.nextFloat() * 0.8f + 0.2f,
                            decay = rnd.nextFloat() * 0.006f + 0.004f,
                            rotation = rnd.nextFloat() * 360f,
                            rotationSpeed = (rnd.nextFloat() - 0.5f) * 8f,
                            type = ParticleType.values()[rnd.nextInt(ParticleType.values().size)],
                            gravity = 0.05f
                        )
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val screenWidth = size.width
        val screenHeight = size.height

        for (p in particles) {
            val px = if (p.x in -0.2f..1.2f) p.x * screenWidth else p.x
            val py = if (p.y in -0.2f..1.2f) p.y * screenHeight else p.y
            val drawColor = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f))

            rotate(p.rotation, pivot = Offset(px, py)) {
                when (p.type) {
                    ParticleType.CIRCLE -> {
                        drawCircle(
                            color = drawColor,
                            radius = p.radius,
                            center = Offset(px, py)
                        )
                    }
                    ParticleType.RING -> {
                        drawCircle(
                            color = drawColor,
                            radius = p.radius,
                            center = Offset(px, py),
                            style = Stroke(width = 3f)
                        )
                    }
                    ParticleType.DIAMOND -> {
                        val path = Path().apply {
                            moveTo(px, py - p.radius * 1.3f)
                            lineTo(px + p.radius, py)
                            lineTo(px, py + p.radius * 1.3f)
                            lineTo(px - p.radius, py)
                            close()
                        }
                        drawPath(path, drawColor)
                    }
                    ParticleType.CROSS -> {
                        val half = p.radius
                        drawLine(
                            color = drawColor,
                            start = Offset(px - half, py - half),
                            end = Offset(px + half, py + half),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = drawColor,
                            start = Offset(px + half, py - half),
                            end = Offset(px - half, py + half),
                            strokeWidth = 3f
                        )
                    }
                    ParticleType.SPARK -> {
                        drawCircle(
                            color = drawColor.copy(alpha = (p.alpha * 0.35f).coerceIn(0f, 1f)),
                            radius = p.radius * 2.2f,
                            center = Offset(px, py)
                        )
                        drawLine(
                            color = drawColor,
                            start = Offset(px - p.radius * 1.5f, py),
                            end = Offset(px + p.radius * 1.5f, py),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = drawColor,
                            start = Offset(px, py - p.radius * 1.5f),
                            end = Offset(px, py + p.radius * 1.5f),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }
        }
    }
}
