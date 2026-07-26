package com.buildwclaude.dialer.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.buildwclaude.dialer.core.ui.theme.palette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slow drifting "northern lights" wash used behind the call screens — soft
 * coloured blobs that orbit continuously, in the app's accent family. Pure
 * Compose drawing; no images, no external assets.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intense: Boolean = false,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "aurora")

    // Three independent slow phases so the blobs never repeat in lockstep.
    val p1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "p1",
    )
    val p2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(21000, easing = LinearEasing)),
        label = "p2",
    )
    val p3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "p3",
    )
    // Gentle breathing of the whole wash.
    val breathe by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    val base = palette.Surface
    val alpha = if (intense) 0.42f else 0.30f

    Box(modifier.fillMaxSize().background(base)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            val w = size.width
            val h = size.height
            val r = maxOf(w, h) * 0.72f * breathe

            fun blob(color: Color, cx: Float, cy: Float, scale: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * scale,
                    ),
                    radius = r * scale,
                    center = Offset(cx, cy),
                )
            }

            // Cyan ribbon sweeping the upper half.
            blob(
                Color(0xFF56CCF2),
                cx = w * (0.28f + 0.30f * cos(p1)),
                cy = h * (0.24f + 0.10f * sin(p1 * 1.3f)),
                scale = 0.95f,
            )
            // Deep blue anchor, slow orbit.
            blob(
                Color(0xFF2F80ED),
                cx = w * (0.74f + 0.22f * sin(p2)),
                cy = h * (0.40f + 0.16f * cos(p2 * 0.8f)),
                scale = 1.05f,
            )
            // Teal-green shimmer low on the screen — the "aurora" tint.
            blob(
                Color(0xFF38B2A3),
                cx = w * (0.42f + 0.26f * sin(p3 * 1.7f)),
                cy = h * (0.78f + 0.10f * cos(p3)),
                scale = 0.85f,
            )
            // Violet accent, counter-rotating.
            blob(
                Color(0xFF6A5ACD),
                cx = w * (0.62f - 0.28f * cos(p1 * 0.6f)),
                cy = h * (0.66f - 0.14f * sin(p2 * 1.1f)),
                scale = 0.78f,
            )

            // Vignette so the controls stay legible over the wash.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        base.copy(alpha = 0.55f),
                        Color.Transparent,
                        base.copy(alpha = 0.70f),
                    ),
                ),
                size = Size(w, h),
            )
        }
        content()
    }
}
