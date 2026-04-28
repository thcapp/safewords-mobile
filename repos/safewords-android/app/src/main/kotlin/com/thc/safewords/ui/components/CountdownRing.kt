package com.thc.safewords.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thc.safewords.ui.theme.Ink
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Dashed-dial countdown ring: 60 ticks (larger at each 5th), an ember
// progress arc, and a knob on the leading edge. Matches the design's
// CountdownRing primitive from the handoff bundle.
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 340.dp,
    content: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = LinearEasing),
        label = "countdown"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val sizePx = this.size.minDimension
            val r = sizePx / 2f - 12.dp.toPx()
            val cx = sizePx / 2f
            val cy = sizePx / 2f

            // 60 ticks
            for (i in 0 until 60) {
                val big = i % 5 == 0
                val a = (i.toDouble() / 60.0) * 2.0 * PI - PI / 2.0
                val r1 = r - if (big) 6.dp.toPx() else 3.dp.toPx()
                val r2 = r + if (big) 2.dp.toPx() else 0f
                val elapsed = i.toFloat() / 60f < animated
                drawLine(
                    color = Ink.fgFaint.copy(alpha = if (elapsed) 0.9f else 0.25f),
                    start = Offset(cx + (cos(a) * r1).toFloat(), cy + (sin(a) * r1).toFloat()),
                    end = Offset(cx + (cos(a) * r2).toFloat(), cy + (sin(a) * r2).toFloat()),
                    strokeWidth = if (big) 1.dp.toPx() else 0.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Progress arc
            val arcSize = Size(r * 2, r * 2)
            val arcTopLeft = Offset(cx - r, cy - r)
            drawArc(
                color = Ink.accent,
                startAngle = -90f,
                sweepAngle = animated * 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Knob
            val angle = animated * 2.0 * PI - PI / 2.0
            val kx = cx + (cos(angle) * r).toFloat()
            val ky = cy + (sin(angle) * r).toFloat()
            drawCircle(
                color = Ink.accent,
                radius = 5.dp.toPx(),
                center = Offset(kx, ky)
            )
            drawCircle(
                color = Ink.accent.copy(alpha = 0.25f),
                radius = 8.dp.toPx(),
                center = Offset(kx, ky),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        content()
    }
}
