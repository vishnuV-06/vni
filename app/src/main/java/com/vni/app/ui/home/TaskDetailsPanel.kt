package com.vni.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Task
import com.vni.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PanelMode { MAIN, TIME, DAYS }

@Composable
fun TaskDetailsPanel(
    task: Task?, 
    currentLogo: ImageVector,
    onIconClick: () -> Unit,
    onSave: (String, Int, Int, String) -> Unit
) {
    var mode by remember { mutableStateOf(PanelMode.MAIN) }
    
    var editingName by remember(task) { mutableStateOf(task?.name ?: "") }
    var editingDays by remember(task) { mutableStateOf(task?.daysLeft ?: 15) }
    var editingMins by remember(task) { mutableStateOf(task?.targetMinutes ?: 30) }
    var editingRepeat by remember(task) { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // M-F

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF0F0924))
            .border(1.dp, PurpleLight.copy(0.4f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
            .animateContentSize()
    ) {
        // Top Handle
        Box(
            modifier = Modifier
                .width(40.dp).height(4.dp)
                .background(PurpleLight.copy(0.5f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(32.dp))

        when (mode) {
            PanelMode.MAIN -> {
                // Row 1: Days, Repeat, OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Days Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF0C071E), RoundedCornerShape(16.dp))
                            .border(1.dp, PurpleLight.copy(0.3f), RoundedCornerShape(16.dp))
                            .clickable { mode = PanelMode.DAYS },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(editingDays.toString(), color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // Repeat Tokens
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        val days = listOf("S", "M", "T", "W", "T", "F", "S")
                        days.forEachIndexed { i, day ->
                            val isSel = editingRepeat.contains(i)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (isSel) PurplePrimary else Color(0xFF1C1040), RoundedCornerShape(6.dp))
                                    .clickable { 
                                        editingRepeat = if (isSel) editingRepeat - i else editingRepeat + i 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // OK Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, Color(0xFF5588FF), CircleShape)
                            .clickable {
                                onSave(editingName, editingMins, editingDays, currentLogo.name)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color(0xFF5588FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Row 2: Task Name and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Task Name Field
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0xFF130B30), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                            cursorBrush = SolidColor(PurpleLight),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (editingName.isEmpty()) Text("Task Name", color = TextMuted, fontSize = 16.sp)
                                innerTextField()
                            }
                        )
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    // Time Button
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .width(100.dp)
                            .background(Color(0xFF0C071E), RoundedCornerShape(12.dp))
                            .border(1.dp, PurpleLight.copy(0.3f), RoundedCornerShape(12.dp))
                            .clickable { mode = PanelMode.TIME },
                        contentAlignment = Alignment.Center
                    ) {
                        val hrs = editingMins / 60
                        val mins = editingMins % 60
                        Text(String.format("%d:%02d", hrs, mins), color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Row 3: ADD NEW button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                        .clickable {
                            onSave(editingName, editingMins, editingDays, currentLogo.name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (task == null) "ADD NEW" else "UPDATE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            
            PanelMode.DAYS -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF0C071E), RoundedCornerShape(20.dp))
                            .border(1.dp, PurpleLight.copy(0.3f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(editingDays.toString(), color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                
                var joystickOffset by remember { mutableFloatStateOf(0f) }
                val maxOffset = with(LocalDensity.current) { 120.dp.toPx() }
                
                // Ticker for continuous increment/decrement
                LaunchedEffect(joystickOffset) {
                    while (joystickOffset != 0f) {
                        val amt = abs(joystickOffset)
                        val sign = if (joystickOffset > 0) 1 else -1
                        val tickVal = when {
                            amt > maxOffset * 0.8f -> 5
                            amt > maxOffset * 0.4f -> 2
                            else -> 1
                        }
                        editingDays = (editingDays + sign * tickVal).coerceAtLeast(0)
                        delay(200) // tick speed
                    }
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Track graphic
                    Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                        val cy = size.height / 2
                        val cx = size.width / 2
                        val len = maxOffset
                        // Center line
                        drawLine(Color.White.copy(alpha=0.5f), Offset(cx - len, cy), Offset(cx + len, cy), strokeWidth = 2.dp.toPx())
                        // Arrows (Decorative)
                        val arrowY1 = cy - 4.dp.toPx()
                        val arrowY2 = cy + 4.dp.toPx()
                        // Right arrows
                        drawLine(Color.White, Offset(cx + len*0.3f, cy), Offset(cx + len*0.3f - 4.dp.toPx(), arrowY1), strokeWidth=2.dp.toPx())
                        drawLine(Color.White, Offset(cx + len*0.3f, cy), Offset(cx + len*0.3f - 4.dp.toPx(), arrowY2), strokeWidth=2.dp.toPx())
                        
                        drawLine(Color.White, Offset(cx + len*0.6f, cy), Offset(cx + len*0.6f - 4.dp.toPx(), arrowY1), strokeWidth=2.dp.toPx())
                        drawLine(Color.White, Offset(cx + len*0.6f + 4.dp.toPx(), cy), Offset(cx + len*0.6f, arrowY1), strokeWidth=2.dp.toPx())
                        
                        // Left arrows
                        drawLine(Color.White, Offset(cx - len*0.3f, cy), Offset(cx - len*0.3f + 4.dp.toPx(), arrowY1), strokeWidth=2.dp.toPx())
                        drawLine(Color.White, Offset(cx - len*0.3f, cy), Offset(cx - len*0.3f + 4.dp.toPx(), arrowY2), strokeWidth=2.dp.toPx())
                    }
                    
                    // Thumb
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(joystickOffset.roundToInt(), 0) }
                            .size(36.dp)
                            .background(PurplePrimary, CircleShape)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = { joystickOffset = 0f },
                                    onDragCancel = { joystickOffset = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        joystickOffset = (joystickOffset + dragAmount).coerceIn(-maxOffset, maxOffset)
                                    }
                                )
                            }
                    )
                }

                Spacer(Modifier.height(32.dp))
                // Done button for DAYS
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, Color(0xFF5588FF), CircleShape)
                            .clickable { mode = PanelMode.MAIN },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color(0xFF5588FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            PanelMode.TIME -> {
                // Time Picker Wheel (Hours and Mins)
                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var hrDrag by remember { mutableFloatStateOf(0f) }
                    var minDrag by remember { mutableFloatStateOf(0f) }

                    // We will just do a simple vertical drag gesture on two boxes
                    // And fake the arc using Canvas
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        // Background arcs
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawArc(
                                color = Color.White.copy(alpha=0.2f),
                                startAngle = -90f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - 100.dp.toPx(), cy - 100.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(200.dp.toPx(), 200.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Hours Scroll
                        val hrs = editingMins / 60
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { hrDrag = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            hrDrag += dragAmount
                                            if (hrDrag > 40f) {
                                                editingMins = maxOf(0, editingMins - 60)
                                                hrDrag = 0f
                                            } else if (hrDrag < -40f) {
                                                editingMins = minOf(23 * 60 + (editingMins % 60), editingMins + 60)
                                                hrDrag = 0f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(hrs.toString(), color = PurplePrimary, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Text(":", color = TextWhite, fontSize = 48.sp, fontWeight = FontWeight.Bold)

                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        // Background arcs
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawArc(
                                color = Color.White.copy(alpha=0.2f),
                                startAngle = -90f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - 50.dp.toPx(), cy - 100.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(200.dp.toPx(), 200.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Mins Scroll
                        val mins = editingMins % 60
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { minDrag = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            minDrag += dragAmount
                                            if (minDrag > 40f) {
                                                if (mins > 0) editingMins -= 1
                                                minDrag = 0f
                                            } else if (minDrag < -40f) {
                                                if (mins < 59) editingMins += 1
                                                minDrag = 0f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(((mins - 1 + 60) % 60).toString(), color = TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%02d", mins), color = TextWhite, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                                Text(((mins + 1) % 60).toString(), color = TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // OK Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, Color(0xFF5588FF), CircleShape)
                            .clickable { mode = PanelMode.MAIN },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color(0xFF5588FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
