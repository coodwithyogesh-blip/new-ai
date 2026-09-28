package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.TaskEmotion

@Composable
fun VoiceVisualizerBar(
    isSpeaking: Boolean,
    isListening: Boolean,
    emotion: TaskEmotion,
    modifier: Modifier = Modifier
) {
    if (!isSpeaking && !isListening) return

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )
    val wave4 by infiniteTransition.animateFloat(
        initialValue = 24f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w4"
    )

    val labelText = if (isListening) "Listening..." else "Arushi Speaking (${emotion.label})"
    val waveColors = if (isListening) {
        listOf(Color(0xFFEF4444), Color(0xFFF97316))
    } else {
        listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(wave1, wave2, wave3, wave4, wave2).forEach { height ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(waveColors))
                )
            }
        }
    }
}
