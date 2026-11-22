package com.vni.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Task
import com.vni.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Bottom card for updating task time, matching Image 1:
 * - 4 tilted purple gradient bars at the top
 * - Smooth purple slider with glowing white/purple circle thumb
 * - Large "27min" on left, white-bordered "OKAY" button on right
 */
@Composable
fun TimeSliderBottomCard(
    task: Task,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialProgress = (task.todayMinutes.toFloat() / task.targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    var progress by remember(task.id) { mutableStateOf(initialProgress) }
    val currentMins by remember {
        derivedStateOf { (progress * task.targetMinutes).toInt().coerceIn(0, task.targetMinutes) }
    }
    var trackWidthPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0C071E))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFC084FC).copy(alpha = 0.8f),
                        Color(0xFFA855F7).copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(enabled = false) {}
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4 Stylized tilted bars matching Image 1
            val activeBarsCount = (progress * 4).toInt().coerceIn(0, 4)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(4) { i ->
                    val isFilled = i < activeBarsCount
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        val slant = 10.dp.toPx()
                        val path = Path().apply {
                            moveTo(slant, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width - slant, size.height)
                            lineTo(0f, size.height)
                            close()
                        }

                        // Fill with gradient
                        drawPath(
                            path = path,
                            brush = if (isFilled) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFC084FC),
                                        Color(0xFFA855F7),
                                        Color(0xFF7E22CE)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1E1738),
                                        Color(0xFF140E29)
                                    )
                                )
                            }
                        )

                        // Outline stroke
                        drawPath(
                            path = path,
                            color = if (isFilled) Color(0xFFD8B4FE).copy(alpha = 0.7f) else Color(0xFF2E2452),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Slider Track with glowing thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(task.id, task.targetMinutes) {
                        detectTapGestures { offset ->
                            progress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(task.id, task.targetMinutes) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            progress = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF26194E))
                )

                // Active Filled Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF8B5CF6),
                                    Color(0xFFA855F7),
                                    Color(0xFFC084FC)
                                )
                            )
                        )
                )

                // Glowing Thumb Circle
                val thumbRadiusPx = with(density) { 12.dp.toPx() }
                val thumbOffsetXPx = (progress * trackWidthPx - thumbRadiusPx).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetXPx.roundToInt(), 0) }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color(0xFFA855F7), CircleShape)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Row: "27min" and "OKAY" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Minutes Text
                Text(
                    text = "${currentMins}min",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                // OKAY Button matching Image 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        .clickable {
                            onConfirm(currentMins)
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OKAY",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
