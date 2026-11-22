package com.vni.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.Task
import com.vni.app.ui.theme.*

/**
 * Middle section: single non-infinite scrollable task list.
 * Each task occupies only ONCE (one task one time only, no looping).
 */
@Composable
fun TaskScrollSection(
    tasks: List<Task>,
    activeIndex: Int,
    onActiveChange: (Int) -> Unit,
    onUpdateTodayMinutes: (taskId: Long, newMins: Int) -> Unit,
    onSidebarOpen: () -> Unit,
    showGlobalTaskProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex.coerceIn(0, tasks.size - 1))
    var currentActiveIndex by remember(tasks.size) { mutableIntStateOf(activeIndex.coerceIn(0, tasks.size - 1)) }

    // Active task whose time is currently being edited with the bottom slider
    var activeSliderTask by remember { mutableStateOf<Task?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            userScrollEnabled = true,
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ONLY ONCE: One task one time only, no infinite multiplication!
            itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                if (showGlobalTaskProgress) {
                    // Exact Image 1 matching layout for all tasks!
                    ProgressTaskCard(
                        task = task,
                        onClick = {
                            activeSliderTask = task
                        }
                    )
                } else {
                    val isActive = index == currentActiveIndex
                    TaskListItem(
                        task = task,
                        isActive = isActive,
                        showGlobalTaskProgress = false,
                        onOpenTimeSlider = {
                            activeSliderTask = task
                        },
                        modifier = Modifier,
                        onClick = {
                            currentActiveIndex = index
                            onActiveChange(index)
                        }
                    )
                }
            }
        }

        // Left chevron arrow (decorative hint)
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = "Scroll hint",
            tint = PurpleLight.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .size(24.dp)
        )

        // Time Slider Bottom Card matching Image 1: appears at the bottom in the exact same position!
        AnimatedVisibility(
            visible = activeSliderTask != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                .zIndex(30f)
        ) {
            if (activeSliderTask != null) {
                TimeSliderBottomCard(
                    task = activeSliderTask!!,
                    onConfirm = { newMins ->
                        onUpdateTodayMinutes(activeSliderTask!!.id, newMins)
                        activeSliderTask = null
                    },
                    onDismiss = {
                        activeSliderTask = null
                    }
                )
            }
        }
    }
}

/**
 * A single task row: collapsed or active.
 */
@Composable
fun TaskListItem(
    task: Task,
    isActive: Boolean,
    showGlobalTaskProgress: Boolean = false,
    onOpenTimeSlider: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isActive) {
        ActiveTaskCard(
            task = task,
            onOpenTimeSlider = onOpenTimeSlider,
            showGlobalTaskProgress = showGlobalTaskProgress,
            modifier = modifier,
            onClick = onClick
        )
    } else {
        val progress = (task.todayMinutes.toFloat() / task.targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
        val progressColor = getTaskProgressColor(progress)

        val isGeneral = !task.isPrimary
        val cardBg = when {
            showGlobalTaskProgress -> progressColor.copy(alpha = 0.22f)
            isGeneral -> Color(0xFF0C1322)
            else -> CardNavy
        }

        val cardBorder = when {
            showGlobalTaskProgress -> progressColor
            isGeneral -> Color(0xFF2E384D)
            else -> BorderNavy
        }

        // Collapsed card
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .border(
                    width = if (showGlobalTaskProgress) 1.5.dp else 1.dp,
                    color = cardBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
        ) {
            if (showGlobalTaskProgress && progress > 0f) {
                // Colored progress bar fill inside collapsed card
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(progressColor.copy(alpha = 0.35f))
                        .align(Alignment.CenterStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGeneral) TextWhite.copy(alpha = 0.85f) else TextWhite,
                        textAlign = TextAlign.Start
                    )
                    if (isGeneral) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(0.8.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GENERAL",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                if (showGlobalTaskProgress) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }
        }
    }
}

/**
 * Task card matching Image 1:
 * - Status-colored border (Green for 100%, Yellow for 75%, Green for 87%, Red for 0%)
 * - Left: Task name (white, bold 17.sp) + clock icon and "$todayMinutes min / $targetMinutes min"
 * - Right: Status-colored percent + 4 segmented progress bars + Chevron Right
 */
@Composable
fun ProgressTaskCard(
    task: Task,
    onClick: () -> Unit = {}
) {
    val progress = (task.todayMinutes.toFloat() / task.targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    val statusColor = when {
        percent >= 80 -> Color(0xFF22C55E) // Green (100% or 87%)
        percent >= 50 -> Color(0xFFEAB308) // Yellow (75%)
        percent > 0   -> Color(0xFFF97316) // Orange
        else          -> Color(0xFFEF4444) // Red (0%)
    }

    val filledCount = when {
        percent >= 95 -> 4
        percent >= 70 -> 3
        percent >= 45 -> 2
        percent >= 20 -> 1
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1424))
            .border(1.5.dp, statusColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Task title + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${task.todayMinutes} min / ${task.targetMinutes} min",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Percent + 4 mini progress segments + Chevron Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$percent%",
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..4) {
                            val isFilled = i <= filledCount
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isFilled) statusColor else statusColor.copy(alpha = 0.14f)
                                    )
                                    .then(
                                        if (!isFilled && percent == 0) {
                                            Modifier.border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

