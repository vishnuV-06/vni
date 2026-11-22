package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Notice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Section C: Notice & Reminders Page matching Image 1:
 * - [ Remind | + ] draggable to the right or tap [+] to add reminder.
 * - Reminder cards with live countdown timer pill (e.g. "10 : 00 : 57 | ✓").
 *   Dragging the timer pill to the right completes and dismisses it.
 * - [ Notice | + ] draggable to the right or tap [+] to add notice.
 * - Notice cards arranged in 2-column grid with draggable "Today \n ✕" deadline badge.
 *   Dragging the deadline badge downwards completes and dismisses it.
 * - Right screen edge pull tab to open the Sidebar Dock (Image 4).
 */
@Composable
fun HomeNoticeSection(
    notices: List<Notice>,
    onAddReminder: () -> Unit,
    onAddNotice: () -> Unit,
    onCompleteReminder: (Long) -> Unit,
    onCompleteNotice: (Long) -> Unit,
    onSidebarOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current time ticker for live countdown timers
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Separate Reminders from Notices (No dummy seeds when deleted!)
    val reminders = remember(notices) {
        notices.filter { it.iconName == "School" || it.durationMinutes != null || it.reminderTriggerMillis != null }
    }

    val regularNotices = remember(notices) {
        notices.filter { it.iconName != "School" && it.durationMinutes == null && it.reminderTriggerMillis == null }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Remind Header Button Pill ──
            DraggableAddPill(
                title = "Remind",
                icon = Icons.Outlined.Notifications,
                onTrigger = onAddReminder
            )

            // ── 2. Reminders List ──
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active reminders",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    reminders.forEach { reminder ->
                        key(reminder.id) {
                            ReminderCardItem(
                                reminder = reminder,
                                currentTimeMillis = currentTimeMillis,
                                onComplete = { onCompleteReminder(reminder.id) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── 3. Notice Header Button Pill ──
            DraggableAddPill(
                title = "Notice",
                icon = Icons.Outlined.Layers,
                onTrigger = onAddNotice
            )

            // ── 4. Notices 2-Column Grid ──
            if (regularNotices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active notices",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val chunkedNotices = regularNotices.chunked(2)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunkedNotices.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { notice ->
                                Box(modifier = Modifier.weight(1f)) {
                                    key(notice.id) {
                                        NoticeCardItem(
                                            notice = notice,
                                            onComplete = { onCompleteNotice(notice.id) }
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * Draggable pill button matching Image 1:
 * [ Remind | + ] or [ Notice | + ]
 * Dragging the pill horizontally to the right (> 45dp) or tapping [+] triggers the callback.
 */
@Composable
private fun DraggableAddPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTrigger: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (dragOffsetX.value > 110f) {
                                onTrigger()
                            }
                            dragOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch { dragOffsetX.animateTo(0f) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (dragOffsetX.value + dragAmount).coerceIn(0f, 220f)
                        coroutineScope.launch { dragOffsetX.snapTo(newOffset) }
                    }
                )
            }
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF240E4F), Color(0xFF170836))
                )
            )
            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
            .clickable { onTrigger() }
            .padding(horizontal = 22.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFE879F9),
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color(0xFF6B21A8))
            )

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add $title",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Reminder Card Item matching Image 1:
 * - Title (e.g. "Lap charge")
 * - Subtitle / Category badge (e.g. "compulsary")
 * - Live Countdown Pill on the right ("10 : 00 : 57 | ✓").
 *   Dragging this timer pill to the right or tapping [✓] dismisses and marks it complete.
 */
@Composable
private fun ReminderCardItem(
    reminder: Notice,
    currentTimeMillis: Long,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDismissed by remember { mutableStateOf(false) }
    val cardOffsetX = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }

    val timerDragX = remember { Animatable(0f) }

    // Calculate remaining countdown
    val triggerMillis = reminder.reminderTriggerMillis
        ?: (System.currentTimeMillis() + (reminder.durationMinutes ?: 120) * 60 * 1000L)
    val diffMillis = (triggerMillis - currentTimeMillis).coerceAtLeast(0L)
    val totalSecs = diffMillis / 1000L
    val hours = totalSecs / 3600L
    val mins = (totalSecs % 3600L) / 60L
    val secs = totalSecs % 60L
    val countdownText = String.format(Locale.ENGLISH, "%02d : %02d : %02d", hours, mins, secs)

    val triggerDismissAnimation = {
        coroutineScope.launch {
            isDismissed = true
            launch { cardOffsetX.animateTo(600f, tween(300)) }
            launch { cardAlpha.animateTo(0f, tween(300)) }
            delay(320L)
            onComplete()
        }
    }

    if (!isDismissed || cardAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(cardOffsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF14082D), Color(0xFF0F0624))
                    )
                )
                .border(1.2.dp, Color(0xFF581C87).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Title and Subtitle / Category
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = reminder.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = reminder.subtitle.ifEmpty { "Important" },
                        color = Color(0xFFA78BFA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Right: Countdown Timer Pill with [✓] checkmark
                // Draggable to the right to complete
                Box(
                    modifier = Modifier
                        .offset { IntOffset(timerDragX.value.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (timerDragX.value > 70f) {
                                            triggerDismissAnimation()
                                        } else {
                                            timerDragX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch { timerDragX.animateTo(0f) }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (timerDragX.value + dragAmount).coerceIn(0f, 150f)
                                    coroutineScope.launch { timerDragX.snapTo(newOffset) }
                                }
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF220D45))
                        .border(1.2.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = countdownText,
                            color = Color(0xFFF3E8FF),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color(0xFF7C3AED))
                        )

                        // Checkmark [✓]
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED))
                                .clickable { triggerDismissAnimation() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete Reminder",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Notice Card Item matching Image 1:
 * - Notice title (e.g. "Special class")
 * - Subtitle / details (e.g. "from 12 to 2")
 * - Deadline badge on right/bottom ("Today \n ✕")
 *   Dragging the deadline badge downwards or clicking [✕] completes and dismisses it.
 */
@Composable
private fun NoticeCardItem(
    notice: Notice,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDismissed by remember { mutableStateOf(false) }
    val cardOffsetY = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }

    val badgeDragY = remember { Animatable(0f) }

    val triggerDismissAnimation = {
        coroutineScope.launch {
            isDismissed = true
            launch { cardOffsetY.animateTo(300f, tween(300)) }
            launch { cardAlpha.animateTo(0f, tween(300)) }
            delay(320L)
            onComplete()
        }
    }

    val deadlineBadgeLabel = when {
        notice.deadlineText.contains("Today", ignoreCase = true) -> "Today"
        notice.deadlineText.startsWith("Deadline: ") -> notice.deadlineText.removePrefix("Deadline: ")
        else -> notice.deadlineText.ifEmpty { "Today" }
    }

    if (!isDismissed || cardAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .offset { IntOffset(0, cardOffsetY.value.roundToInt()) }
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF14082D), Color(0xFF0F0624))
                    )
                )
                .border(1.2.dp, Color(0xFF581C87).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Title and Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = notice.title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = notice.subtitle,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Bottom Right: Deadline Badge ("Today \n ✕")
                // Draggable downwards to complete
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .offset { IntOffset(0, badgeDragY.value.roundToInt()) }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (badgeDragY.value > 45f) {
                                            triggerDismissAnimation()
                                        } else {
                                            badgeDragY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch { badgeDragY.animateTo(0f) }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (badgeDragY.value + dragAmount).coerceIn(0f, 100f)
                                    coroutineScope.launch { badgeDragY.snapTo(newOffset) }
                                }
                            )
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF220D45))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = deadlineBadgeLabel,
                            color = Color(0xFFF3E8FF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // ✕ Icon button
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38146F))
                                .clickable { triggerDismissAnimation() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Complete Notice",
                                tint = Color(0xFFE879F9),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
