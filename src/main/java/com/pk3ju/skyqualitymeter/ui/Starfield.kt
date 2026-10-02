package com.pk3ju.skyqualitymeter.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class StarPoint(val x: Float, val y: Float, val radius: Float, val baseAlpha: Float, val twinkles: Boolean)

@Composable
fun StarfieldBackground(isRedMode: Boolean, isDarkTheme: Boolean) {
    if (!isDarkTheme && !isRedMode) return
    val stars = remember {
        List(180) {
            StarPoint(
                x = Random.nextFloat(), y = Random.nextFloat(),
                radius = Random.nextFloat() * 1.8f + 0.5f,
                baseAlpha = Random.nextFloat() * 0.6f + 0.2f,
                twinkles = Random.nextBoolean()
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition()
    val twinkleAmount by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        stars.forEach { star ->
            val alpha = (star.baseAlpha * if (star.twinkles) twinkleAmount else 1f).coerceIn(0.1f, 1f)
            val color = if (isRedMode) Color(0xFFFF3333) else Color(0xFFE2E8F0)
            drawCircle(color = color.copy(alpha = alpha), radius = star.radius, center = Offset(star.x * w, star.y * h))
        }
    }
}
