package com.vni.app.ui.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Iron Man Arc Reactor Component:
 * - Iconic inverted cyan glowing triangle core
 * - Segmented radial power coils and metallic rim
 * - Continuous smooth clockwise rotation when active
 * - Progress ring with glowing violet fill
 * - Lower opacity when paused (standby / low-power state)
 * - Single-click pause/resume, long-press to stop
 */
@Composable
fun ArcReactorView(
    modifier: Modifier = Modifier,
    reactorSize: Dp = 220.dp,
    isRunning: Boolean = true,
    isPaused: Boolean = false,
    progress: Float = 0f, // 0f to 1f (violet fill)
    showProgressRing: Boolean = true,
    showPowerBeam: Boolean = true,
    onSingleTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    // Rotation animation: rotates smoothly clockwise while active
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for core glow
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Beam pulse animation
    val beamPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_pulse"
    )

    val currentRotation = if (isRunning && !isPaused) rotationAngle else 0f
    val displayAlpha = if (isPaused) 0.38f else 1.0f

    Box(
        modifier = modifier
            .size(reactorSize)
            .alpha(displayAlpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (minOf(size.width, size.height) / 2f) * 0.84f

            // 1. Surrounding Progress Ring (White track + Violet filling)
            if (showProgressRing) {
                val ringStroke = baseRadius * 0.08f
                val ringRadius = baseRadius + ringStroke * 1.4f
                val ringRect = Size(ringRadius * 2f, ringRadius * 2f)
                val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)

                // Background track (subtle translucent white ring)
                drawArc(
                    color = Color.White.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringRect,
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                )

                // Foreground active violet progress arc
                val sweep = (progress.coerceIn(0f, 1f) * 360f)
                if (sweep > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFF8B5CF6))
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = ringTopLeft,
                        size = ringRect,
                        style = Stroke(width = ringStroke * 1.15f, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Power Laser Beams extending downward to digital timer
            if (showPowerBeam && isRunning && !isPaused) {
                val beamColor = Color(0xFFA855F7).copy(alpha = beamPulse)
                val beamWidth = 2.5f
                drawLine(
                    color = beamColor,
                    start = Offset(center.x - 12f, center.y + baseRadius),
                    end = Offset(center.x - 12f, center.y + baseRadius + 48f),
                    strokeWidth = beamWidth
                )
                drawLine(
                    color = Color(0xFFE879F9).copy(alpha = beamPulse * 1.1f),
                    start = Offset(center.x, center.y + baseRadius),
                    end = Offset(center.x, center.y + baseRadius + 58f),
                    strokeWidth = beamWidth * 1.2f
                )
                drawLine(
                    color = beamColor,
                    start = Offset(center.x + 12f, center.y + baseRadius),
                    end = Offset(center.x + 12f, center.y + baseRadius + 48f),
                    strokeWidth = beamWidth
                )
            }

            // 3. Rotating Arc Reactor Core (Rotates clockwise)
            rotate(currentRotation, pivot = center) {
                // Outer Metallic Rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF070B14)),
                        center = center,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )

                // Outer Glowing Blue Ring
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = baseRadius * 0.05f)
                )

                // 10 Segmented Peripheral Coils
                val numCoils = 10
                for (i in 0 until numCoils) {
                    val angleDeg = (i * 360f / numCoils)
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val coilDist = baseRadius * 0.78f
                    val coilCenter = Offset(
                        center.x + (coilDist * cos(rad)).toFloat(),
                        center.y + (coilDist * sin(rad)).toFloat()
                    )

                    // Coil block
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF0284C7), Color(0xFF082F49)),
                            center = coilCenter,
                            radius = baseRadius * 0.14f
                        ),
                        radius = baseRadius * 0.12f,
                        center = coilCenter
                    )
                }

                // Inner Metallic Ring
                val innerRimRadius = baseRadius * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.45f), Color(0xFF0B192C)),
                        center = center,
                        radius = innerRimRadius
                    ),
                    radius = innerRimRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = innerRimRadius,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // Inverted Triangle Core (Iconic Mark VI Arc Reactor)
                val triRadius = innerRimRadius * 0.88f * corePulse
                val triPath = Path().apply {
                    // Inverted triangle vertices (Pointing downwards)
                    // Vertex 1: Top-Left
                    val a1 = Math.toRadians(-150.0)
                    // Vertex 2: Top-Right
                    val a2 = Math.toRadians(-30.0)
                    // Vertex 3: Bottom
                    val a3 = Math.toRadians(90.0)

                    moveTo(center.x + (triRadius * cos(a1)).toFloat(), center.y + (triRadius * sin(a1)).toFloat())
                    lineTo(center.x + (triRadius * cos(a2)).toFloat(), center.y + (triRadius * sin(a2)).toFloat())
                    lineTo(center.x + (triRadius * cos(a3)).toFloat(), center.y + (triRadius * sin(a3)).toFloat())
                    close()
                }

                // Triangle glow outline
                drawPath(
                    path = triPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF0284C7))
                    ),
                    style = Stroke(width = baseRadius * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Triangle filled cyan core with bright white center
                drawPath(
                    path = triPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFF80DEEA), Color(0xFF00E5FF), Color(0xFF0284C7).copy(alpha = 0.85f)),
                        center = center,
                        radius = triRadius * 0.75f
                    )
                )

                // Inner nested inverted triangle
                val innerTriRadius = triRadius * 0.45f
                val innerTriPath = Path().apply {
                    val a1 = Math.toRadians(-150.0)
                    val a2 = Math.toRadians(-30.0)
                    val a3 = Math.toRadians(90.0)
                    moveTo(center.x + (innerTriRadius * cos(a1)).toFloat(), center.y + (innerTriRadius * sin(a1)).toFloat())
                    lineTo(center.x + (innerTriRadius * cos(a2)).toFloat(), center.y + (innerTriRadius * sin(a2)).toFloat())
                    lineTo(center.x + (innerTriRadius * cos(a3)).toFloat(), center.y + (innerTriRadius * sin(a3)).toFloat())
                    close()
                }

                drawPath(
                    path = innerTriPath,
                    color = Color.White,
                    style = Stroke(width = 3.5f)
                )
            }
        }
    }
}
