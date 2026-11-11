package com.smart.expense.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smart.expense.ui.theme.EmeraldAccent
import com.smart.expense.ui.theme.ForestGreenPrimary

@Composable
fun VoiceWaveform(
    isListening: Boolean,
    rmsdB: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / barCount) * 0.45f
        val gap = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val normalizedDb = if (isListening) {
            ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1f)
        } else {
            0.12f
        }

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount.toFloat()
            val sineWave = Math.sin((progress * 4.0 * Math.PI + phase)).toFloat()
            val dynamicHeightMultiplier = if (isListening) {
                0.25f + 0.75f * Math.abs(sineWave) * normalizedDb
            } else {
                0.15f
            }

            val barHeight = (height * dynamicHeightMultiplier).coerceIn(6f, height)
            val x = i * (barWidth + gap)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ForestGreenPrimary,
                        EmeraldAccent
                    )
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

