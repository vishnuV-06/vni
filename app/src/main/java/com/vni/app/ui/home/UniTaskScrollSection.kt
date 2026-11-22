package com.vni.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.UniTask
import com.vni.app.ui.theme.PurpleLight
import com.vni.app.ui.theme.PurplePrimary

/**
 * Uni-Task List Section matching Image 1:
 * - Rounded dark purple cards with centered bold text:
 *   "Eat 2 banana", "play piano", "learn english"
 * - Double-click toggles completed status.
 * - Shows completion indicator and counts.
 */
@Composable
fun UniTaskScrollSection(
    uniTasks: List<UniTask>,
    onToggleUniTask: (Long) -> Unit,
    onSidebarOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (uniTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Uni-Tasks\nDouble click the top bar or use Edit to add",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(uniTasks, key = { it.id }) { uniTask ->
                    UniTaskCard(
                        uniTask = uniTask,
                        onDoubleTap = { onToggleUniTask(uniTask.id) }
                    )
                }
            }
        }

        // Left chevron arrow (decorative hint to open sidebar)
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = "Sidebar hint",
            tint = PurpleLight.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun UniTaskCard(
    uniTask: UniTask,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (uniTask.completedToday) PurplePrimary else Color(0xFF2E1C6A),
        animationSpec = spring(),
        label = "BorderColor"
    )

    val backgroundBrush = if (uniTask.completedToday) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1E103E),
                Color(0xFF140B30),
                Color(0xFF1E103E)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF130B30),
                Color(0xFF130B30)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundBrush)
            .border(
                width = if (uniTask.completedToday) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Centered Bold Task Name matching Image 1
        Text(
            text = uniTask.name,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        // Subtle Completion Check Badge on the right
        if (uniTask.completedToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
