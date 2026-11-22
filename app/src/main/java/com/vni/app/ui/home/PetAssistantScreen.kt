package com.vni.app.ui.home

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.*
import com.vni.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Data model for Rich Data Progress Insights (Item 7)
 */
data class ProgressInsightItem(
    val category: String,
    val title: String,
    val message: String,
    val iconType: String,
    val color: Color
)

/**
 * Pet Assistant Screen:
 * Implements all 8 non-linear modes:
 * 1. Wake Up Time (saves date to not ask again today until tomorrow)
 * 2. Today's Tasks & Suggestions (redirects on click, quick action buttons)
 * 3. Tomorrow Suggestions (redirects on click)
 * 4. This Week Overview & Milestones
 * 5. Motive / Quotes Reels (vertical swipe changes quote like reels, pet stays still at top!)
 * 6. Forgotten / Inactive Tasks Alert ("at least do now!")
 * 7. Rich Data Progress Insights Reels (progress on day, peak performance, time invested, streak, consistency trend)
 * 8. Good Night Sleep Check-in & Wasted Time Input
 *
 * Non-linear routing:
 * - Starting day: 1 -> 2 (scroll up) -> 3 -> 5 -> end | (scroll left in 2 -> 5 -> end)
 * - Ending day (>= 21:00 or night): 8 -> end
 * - Periodic check-in (after 2-3 hours): 6 -> end, or 7 -> end, or 4 -> end, or 3 -> end
 * - Manual open: channels 2 (Tasks: 2->3->4), 5 (Motive reels infinity), 7 (Data insights reels infinity)
 *   Swiping left (<-) cycles between 2, 5, 7!
 * - Double tap bottom returns to home immediately!
 */
@Composable
fun PetAssistantScreen(
    tasks: List<Task>,
    uniTasks: List<UniTask>,
    notices: List<Notice>,
    doItems: List<DoItem>,
    plans: List<DaySchedulePlan>,
    selectedPlanName: String,
    motives: List<Motive> = emptyList(),
    motiveHeadings: List<MotiveHeading> = emptyList(),
    todayProgress: Int = 0,
    weekWastedMinutes: List<Int> = emptyList(),
    sessionOrigin: String = "AUTO",
    onSelectPlan: (String) -> Unit = {},
    onLogWakeUpTime: (String) -> Unit = {},
    onLogSleepAndWasted: (sleepTime: String, wastedMinutes: Int) -> Unit = { _, _ -> },
    onCompleteDoItem: (Long) -> Unit = {},
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToDoItem: () -> Unit = {},
    onNavigateToNotice: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vni_pet_assistant", Context.MODE_PRIVATE) }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isWakeUpLoggedToday = remember { prefs.getString("wake_up_logged_date", "") == todayKey }
    val isFirstOpenAskedToday = remember { prefs.getString("first_open_asked_date", "") == todayKey }

    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)

    // Current active day plan
    val activePlan = plans.find { it.name.equals(selectedPlanName, ignoreCase = true) } ?: plans.firstOrNull()
    val activeSlot = remember(activePlan, currentHour, currentMinute) {
        activePlan?.sections?.find { sec ->
            val startTotal = sec.startHour * 60 + sec.startMinute
            val endTotal = sec.endHour * 60 + sec.endMinute
            val nowTotal = currentHour * 60 + currentMinute
            nowTotal in startTotal..endTotal
        }
    }

    // Determine initial card based on sessionOrigin and conditions
    val initialCard = remember {
        when (sessionOrigin) {
            "MANUAL" -> 2 // Opens Tasks (with 2, 5, 7 channel navigation)
            "NIGHT" -> 8 // End of day check-in
            "START_OF_DAY" -> 1 // First open of the day
            "PERIODIC" -> {
                val hasForgotten = tasks.any { !it.completedToday && it.todayMinutes == 0 }
                val candidates = if (hasForgotten) listOf(6, 7, 4, 3) else listOf(7, 4, 3)
                candidates.random()
            }
            else -> {
                if (currentHour >= 21 || currentHour < 4) {
                    8
                } else if (!isFirstOpenAskedToday) {
                    1
                } else {
                    val hasForgotten = tasks.any { !it.completedToday && it.todayMinutes == 0 }
                    if (hasForgotten) listOf(6, 7, 2).random() else listOf(2, 7, 5).random()
                }
            }
        }
    }

    var currentCard by remember { mutableIntStateOf(initialCard) }
    var petJumpTrigger by remember { mutableIntStateOf(0) }

    // Wake-up states
    var wakeHour by remember { mutableIntStateOf(7) }
    var wakeMinute by remember { mutableIntStateOf(30) }
    var showWakeTimeDialog by remember { mutableStateOf(false) }

    // Sleep states
    var sleepHour by remember { mutableIntStateOf(23) }
    var sleepMinute by remember { mutableIntStateOf(0) }
    var showSleepTimeDialog by remember { mutableStateOf(false) }
    var wastedMinutesInput by remember { mutableIntStateOf(45) }

    // Motives & Quotes library
    val allQuotes = remember(motives, motiveHeadings) {
        val list = mutableListOf<String>()
        motives.forEach { if (it.text.isNotBlank()) list.add(it.text) }
        motiveHeadings.flatMap { it.sentences }.forEach { if (it.isNotBlank()) list.add(it) }
        if (list.isEmpty()) {
            list.addAll(
                listOf(
                    "Discipline is choosing between what you want now and what you want most.",
                    "Consistency is what transforms average into excellence.",
                    "Small daily improvements over time lead to stunning results.",
                    "You don't have to be extreme, just consistent, Boss.",
                    "Do something today that your future self will thank you for.",
                    "Focus on the process, and the results will take care of themselves.",
                    "Energy flows where attention goes. Stay dialed in, Boss!",
                    "The secret of getting ahead is getting started.",
                    "Don't stop when you're tired. Stop when you're done, Boss."
                )
            )
        }
        list.distinct()
    }
    var motiveReelIndex by remember { mutableIntStateOf(0) }

    // Data Progress Insights
    val progressInsights = remember(tasks, uniTasks, doItems, todayProgress, weekWastedMinutes) {
        buildProgressInsights(tasks, uniTasks, doItems, todayProgress, weekWastedMinutes)
    }
    var progressInsightIndex by remember { mutableIntStateOf(0) }

    // Gesture Handling
    fun onSwipeUp() {
        petJumpTrigger++
        when (currentCard) {
            1 -> {
                // Save wake-up and advance to 2
                val timeFormatted = String.format("%02d:%02d", wakeHour, wakeMinute)
                prefs.edit()
                    .putString("first_open_asked_date", todayKey)
                    .putString("wake_up_logged_date", todayKey)
                    .putString("wake_up_time", timeFormatted)
                    .apply()
                onLogWakeUpTime(timeFormatted)
                currentCard = 2
            }
            2 -> currentCard = 3 // 2 -> 3
            3 -> currentCard = if (sessionOrigin == "START_OF_DAY") 5 else 4 // 3 -> 4 or 5
            4 -> currentCard = 5 // 4 -> 5
            5 -> {
                // Cycle Motive quotes infinitely!
                motiveReelIndex = (motiveReelIndex + 1) % allQuotes.size
            }
            6 -> currentCard = 7 // 6 -> 7
            7 -> {
                // Cycle Progress insights infinitely!
                progressInsightIndex = (progressInsightIndex + 1) % progressInsights.size
            }
            8 -> onDismiss()
        }
    }

    fun onSwipeDown() {
        petJumpTrigger++
        when (currentCard) {
            3 -> currentCard = 2
            4 -> currentCard = 3
            5 -> {
                if (allQuotes.isNotEmpty()) {
                    motiveReelIndex = if (motiveReelIndex > 0) motiveReelIndex - 1 else allQuotes.size - 1
                }
            }
            7 -> {
                if (progressInsights.isNotEmpty()) {
                    progressInsightIndex = if (progressInsightIndex > 0) progressInsightIndex - 1 else progressInsights.size - 1
                }
            }
            2 -> if (!isWakeUpLoggedToday) currentCard = 1
        }
    }

    fun onSwipeLeft() {
        petJumpTrigger++
        if (sessionOrigin == "MANUAL") {
            // Cycle between 2, 5, 7 as requested!
            currentCard = when (currentCard) {
                2, 3, 4 -> 5
                5 -> 7
                else -> 2
            }
        } else if (sessionOrigin == "START_OF_DAY") {
            // "if we scroll left in 2 -> 5 then end"
            if (currentCard == 2 || currentCard == 3 || currentCard == 4) {
                currentCard = 5
            } else if (currentCard == 5) {
                onDismiss()
            } else {
                onDismiss()
            }
        } else {
            // Periodic / Night check-in: Swipe left finishes
            onDismiss()
        }
    }

    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07051A),
                        Color(0xFF0D082A),
                        Color(0xFF08051E)
                    )
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += dragAmount.x
                        dragAccumulatorY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(dragAccumulatorX) > abs(dragAccumulatorY)) {
                            if (dragAccumulatorX < -40f) {
                                onSwipeLeft()
                            }
                        } else {
                            if (dragAccumulatorY < -40f) {
                                onSwipeUp()
                            } else if (dragAccumulatorY > 40f) {
                                onSwipeDown()
                            }
                        }
                    }
                )
            }
            .zIndex(85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top Navigation Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button or Mode indicator
                if (currentCard in listOf(3, 4)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B143A))
                            .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onSwipeDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }

                // Middle: Channel Tabs (for MANUAL mode) or Title Badge
                if (sessionOrigin == "MANUAL") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF130A2A))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Tasks", listOf(2, 3, 4), "🎯 Tasks"),
                            Triple("Motives", listOf(5), "🔥 Motive"),
                            Triple("Insights", listOf(7), "📊 Insights")
                        ).forEach { (_, cardGroup, label) ->
                            val isSel = currentCard in cardGroup
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) PurplePrimary else Color.Transparent)
                                    .clickable {
                                        petJumpTrigger++
                                        currentCard = cardGroup.first()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Badge indicating mode
                    val modeTitle = when (currentCard) {
                        1 -> "MORNING CHECK-IN"
                        2 -> "TODAY'S MISSION"
                        3 -> "TOMORROW'S RADAR"
                        4 -> "WEEKLY TARGETS"
                        5 -> "MOTIVE REELS"
                        6 -> "ACTION REQUIRED"
                        7 -> "DATA INSIGHTS"
                        8 -> "NIGHT CHECK-IN"
                        else -> "ASSISTANT"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1045))
                            .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = modeTitle,
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Right: Close button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B143A))
                        .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Notification Off Reminder Banner (Item 7)
            val appPrefs = remember(context) { context.getSharedPreferences("vni_app_prefs_v2", Context.MODE_PRIVATE) }
            val notificationsEnabled = remember { appPrefs.getBoolean("notifications_enabled", true) }
            if (!notificationsEnabled) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2C0B18),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Boss, notifications are currently turned off! Please turn on notifications using the bell symbol in Notice section to stay on track.",
                            color = Color(0xFFFFCDD2),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Fixed Cute Pet Header Area (Pet does NOT move when text changes!) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                CutePetCharacter(
                    modifier = Modifier.size(92.dp),
                    jumpTrigger = petJumpTrigger,
                    isSleepy = (currentCard == 8)
                )
            }

            // ── Main Dynamic Card Content Area ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                AnimatedContent(
                    targetState = currentCard,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height / 2 } + fadeOut()
                        )
                    },
                    label = "PetCardContent"
                ) { card ->
                    when (card) {
                        1 -> StepWakeUpTimeCard(
                            currentHour = currentHour,
                            wakeHour = wakeHour,
                            wakeMinute = wakeMinute,
                            onTimeClick = { showWakeTimeDialog = true },
                            onSet = {
                                val timeFormatted = String.format("%02d:%02d", wakeHour, wakeMinute)
                                prefs.edit()
                                    .putString("first_open_asked_date", todayKey)
                                    .putString("wake_up_logged_date", todayKey)
                                    .putString("wake_up_time", timeFormatted)
                                    .apply()
                                onLogWakeUpTime(timeFormatted)
                                petJumpTrigger++
                                currentCard = 2
                            }
                        )

                        2 -> StepTodaySuggestionsCard(
                            tasks = tasks,
                            doItems = doItems,
                            notices = notices,
                            activeSlot = activeSlot,
                            onNavigateToTask = onNavigateToTask,
                            onNavigateToDoItem = onNavigateToDoItem,
                            onNavigateToNotice = onNavigateToNotice,
                            onNavigateToPlan = onNavigateToPlan,
                            onDoneFirstTask = {
                                val firstPending = doItems.firstOrNull { !it.isCompleted }
                                if (firstPending != null) {
                                    onCompleteDoItem(firstPending.id)
                                    petJumpTrigger++
                                }
                            },
                            onNext = { onSwipeUp() }
                        )

                        3 -> StepTomorrowSuggestionsCard(
                            tasks = tasks,
                            doItems = doItems,
                            notices = notices,
                            onNavigateToTask = onNavigateToTask,
                            onNavigateToDoItem = onNavigateToDoItem,
                            onNavigateToNotice = onNavigateToNotice,
                            onNext = { onSwipeUp() }
                        )

                        4 -> StepThisWeekOverviewCard(
                            tasks = tasks,
                            uniTasks = uniTasks,
                            notices = notices,
                            todayProgress = todayProgress,
                            onNext = { onSwipeUp() }
                        )

                        5 -> StepMotiveReelsCard(
                            quotes = allQuotes,
                            currentIndex = motiveReelIndex,
                            onNextQuote = {
                                petJumpTrigger++
                                motiveReelIndex = (motiveReelIndex + 1) % allQuotes.size
                            },
                            onClose = { onDismiss() }
                        )

                        6 -> StepForgottenTasksCard(
                            tasks = tasks,
                            onNavigateToTask = onNavigateToTask,
                            onNext = { onSwipeUp() }
                        )

                        7 -> StepProgressInsightsReelsCard(
                            insights = progressInsights,
                            currentIndex = progressInsightIndex,
                            onNextInsight = {
                                petJumpTrigger++
                                progressInsightIndex = (progressInsightIndex + 1) % progressInsights.size
                            },
                            onClose = { onDismiss() }
                        )

                        8 -> StepGoodNightSleepCard(
                            sleepHour = sleepHour,
                            sleepMinute = sleepMinute,
                            wastedMinutes = wastedMinutesInput,
                            onTimeClick = { showSleepTimeDialog = true },
                            onWastedChange = { wastedMinutesInput = it },
                            onConfirm = {
                                val sleepTime = String.format("%02d:%02d", sleepHour, sleepMinute)
                                prefs.edit().putString("sleep_logged_date", todayKey).putString("sleep_time", sleepTime).apply()
                                onLogSleepAndWasted(sleepTime, wastedMinutesInput)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // ── Bottom Area: Gesture Hints + Double Tap Detector ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onDismiss() }
                        )
                    }
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwipeVertical,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (currentCard in listOf(5, 7)) "swipe up for next • swipe left to switch" else "swipe up for next • swipe left to advance",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "•• double-tap bottom to return home ••",
                        color = PurplePrimary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Time Picker Dialog for Wake Up
        if (showWakeTimeDialog) {
            WakeUpTimePickerDialog(
                title = "Set Wake Up Time",
                initialHour = wakeHour,
                initialMinute = wakeMinute,
                onConfirm = { h, m ->
                    wakeHour = h
                    wakeMinute = m
                    showWakeTimeDialog = false
                },
                onDismiss = { showWakeTimeDialog = false }
            )
        }

        // Time Picker Dialog for Sleep
        if (showSleepTimeDialog) {
            WakeUpTimePickerDialog(
                title = "Set Sleep Time",
                initialHour = sleepHour,
                initialMinute = sleepMinute,
                onConfirm = { h, m ->
                    sleepHour = h
                    sleepMinute = m
                    showSleepTimeDialog = false
                },
                onDismiss = { showSleepTimeDialog = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Wake Up Time Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepWakeUpTimeCard(
    currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    wakeHour: Int,
    wakeMinute: Int,
    onTimeClick: () -> Unit,
    onSet: () -> Unit
) {
    val isLate = wakeHour > 8 || (wakeHour == 8 && wakeMinute >= 30)
    val greetingText = when {
        currentHour < 12 -> "GOOD MORNING !"
        currentHour in 12..16 -> "GOOD AFTERNOON !"
        currentHour in 17..20 -> "GOOD EVENING !"
        else -> "GOOD NIGHT !"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HELLO , BOSS",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = greetingText,
            color = TextWhite.copy(alpha = 0.85f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "What time did you wake up today?",
            color = Color(0xFFE2E8F0),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Time card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF130D32).copy(alpha = 0.85f))
                .border(1.5.dp, PurplePrimary, RoundedCornerShape(16.dp))
                .clickable { onTimeClick() }
                .padding(horizontal = 40.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d", wakeHour, wakeMinute),
                color = TextWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // Late warning
        if (isLate) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF381528))
                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ You're losing consistency, boss! At least sleep early tonight to reset your rhythm!",
                    color = Color(0xFFFDA4AF),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Rectangular [ SET ] Button with curved edges
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF9333EA))
                    )
                )
                .border(1.dp, Color(0xFFA78BFA), RoundedCornerShape(10.dp))
                .clickable { onSet() }
                .padding(horizontal = 48.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SET",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Today's Tasks & Suggestions Card (Redirects on click!)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepTodaySuggestionsCard(
    tasks: List<Task>,
    doItems: List<DoItem>,
    notices: List<Notice>,
    activeSlot: DayTimeSection?,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToDoItem: () -> Unit,
    onNavigateToNotice: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onDoneFirstTask: () -> Unit,
    onNext: () -> Unit
) {
    val uncompletedTasks = tasks.filter { !it.completedToday }
    val todayDoItems = doItems.filter { !it.isCompleted }
    val todayNotices = notices.filter { it.isToday }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        item {
            Column {
                Text(
                    text = "TODAY'S MISSION",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap any item to go straight to work, Boss!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // Active ongoing slot announcement
        if (activeSlot != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1045))
                        .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                        .clickable { onNavigateToPlan() }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⚡ CURRENT SLOT: ${activeSlot.name}",
                                color = Color(0xFFDDD6FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeSlot.startTime} - ${activeSlot.endTime}",
                                color = TextWhite,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Plan",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row: [ Done ] , [ I will do ] , [ After 10 min ]
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF163820))
                        .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp))
                        .clickable { onDoneFirstTask() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", color = Color(0xFF4ADE80), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF251A4D))
                        .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                        .clickable { onNext() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("I will do", color = TextWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181330))
                        .border(1.dp, Color(0xFF3B2870), RoundedCornerShape(8.dp))
                        .clickable { onNext() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("After 10 min", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Suggestions List (Clicking redirects to task/do/notice!)
        items(uncompletedTasks.take(4)) { task ->
            SuggestItemRow(
                badge = "Task",
                badgeColor = Color(0xFFA855F7),
                title = task.name,
                subtitle = "Target: ${task.targetMinutes} mins • Left: ${task.targetMinutes - task.todayMinutes}m",
                onClick = { onNavigateToTask(task.id) }
            )
        }

        items(todayDoItems.take(3)) { item ->
            SuggestItemRow(
                badge = "Do",
                badgeColor = Color(0xFF06B6D4),
                title = item.name,
                subtitle = if (item.isDailyUntilComplete) "Daily until complete" else "Deadline: ${item.deadlineDateText}",
                onClick = { onNavigateToDoItem() }
            )
        }

        items(todayNotices.take(2)) { notice ->
            SuggestItemRow(
                badge = "Notice",
                badgeColor = Color(0xFFF97316),
                title = notice.title,
                subtitle = notice.subtitle,
                onClick = { onNavigateToNotice() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Tomorrow Suggestions Card (Redirects on click!)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepTomorrowSuggestionsCard(
    tasks: List<Task>,
    doItems: List<DoItem>,
    notices: List<Notice>,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToDoItem: () -> Unit,
    onNavigateToNotice: () -> Unit,
    onNext: () -> Unit
) {
    val tomorrowDo = doItems.filter { isTomorrow(it.deadlineDateText) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        item {
            Column {
                Text(
                    text = "TOMORROW'S RADAR",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Get ahead of tomorrow's priorities, Boss!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        if (tomorrowDo.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF130D2E))
                        .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨ Clean radar! No specific deadlines due tomorrow.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(tomorrowDo) { item ->
                SuggestItemRow(
                    badge = "Due Tomorrow",
                    badgeColor = Color(0xFFEC4899),
                    title = item.name,
                    subtitle = "Days req: ${item.daysRequired} • ${item.deadlineDateText}",
                    onClick = { onNavigateToDoItem() }
                )
            }
        }

        item {
            Text(
                text = "Key Daily Tasks Scheduled Tomorrow:",
                color = Color(0xFFA78BFA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(tasks.take(3)) { task ->
            SuggestItemRow(
                badge = "Scheduled",
                badgeColor = Color(0xFF3B82F6),
                title = task.name,
                subtitle = "Target: ${task.targetMinutes} minutes planned",
                onClick = { onNavigateToTask(task.id) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. This Week Overview Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepThisWeekOverviewCard(
    tasks: List<Task>,
    uniTasks: List<UniTask>,
    notices: List<Notice>,
    todayProgress: Int,
    onNext: () -> Unit
) {
    val totalLoggedMins = tasks.sumOf { it.weekLoggedMinutes.sum() + it.todayMinutes }
    val totalTargetMins = tasks.sumOf { it.targetMinutes * 7 }.coerceAtLeast(1)
    val weekPct = ((totalLoggedMins.toFloat() / totalTargetMins) * 100).toInt().coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        item {
            Column {
                Text(
                    text = "THIS WEEK'S TARGETS",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "High-level radar of your weekly performance",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // Summary Metric Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF130A2C))
                    .border(1.dp, Color(0xFF331E70), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalLoggedMins / 60}h ${totalLoggedMins % 60}m", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Logged Time", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFF2E1C6A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$todayProgress%", color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Today's Score", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFF2E1C6A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$weekPct%", color = Color(0xFFA78BFA), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Weekly Target", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }

        // Notices this week
        if (notices.isNotEmpty()) {
            item {
                Text(
                    text = "Notices & Deadlines:",
                    color = Color(0xFFA78BFA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items(notices.take(3)) { notice ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF160E36))
                        .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(notice.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(notice.subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Motive / Quotes Reels Card (Cycles infinitely like reels, pet stays still!)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepMotiveReelsCard(
    quotes: List<String>,
    currentIndex: Int,
    onNextQuote: () -> Unit,
    onClose: () -> Unit
) {
    val quote = quotes.getOrElse(currentIndex) { "Consistency is what transforms average into excellence." }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tag badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF261245))
                .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🔥 MOTIVE REEL • #${currentIndex + 1}",
                color = Color(0xFFD8B4FE),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        // Reel Quote Card with animated content
        AnimatedContent(
            targetState = quote,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                )
            },
            label = "MotiveReelQuote"
        ) { currentQuoteText ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF170E3A), Color(0xFF100829))
                        )
                    )
                    .border(1.5.dp, Color(0xFF6B21A8), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\"$currentQuoteText\"",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221555))
                    .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                    .clickable { onNextQuote() }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Next Quote ↑", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181036))
                .border(1.dp, Color(0xFF33206A), RoundedCornerShape(8.dp))
                .clickable { onClose() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close ←", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Forgotten Tasks Alert Card ("At least do now!")
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepForgottenTasksCard(
    tasks: List<Task>,
    onNavigateToTask: (Long) -> Unit,
    onNext: () -> Unit
) {
    val forgottenTask = tasks.firstOrNull { !it.completedToday && it.todayMinutes == 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF381024))
                .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "⚡ AT LEAST DO NOW !",
                color = Color(0xFFFDA4AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        if (forgottenTask != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF150928))
                    .border(1.5.dp, Color(0xFFE11D48).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = forgottenTask.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Boss, you haven't touched this task today! Don't let your streak slip away. Even 15 minutes right now will keep your momentum alive!",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // [ Do Now > ] button redirects directly to task!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE11D48))
                    .clickable { onNavigateToTask(forgottenTask.id) }
                    .padding(horizontal = 36.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Do Now >",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Text(
                text = "Boss, you have touched all your active tasks today! Exceptional discipline.",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Rich Data Progress Insights Reels Card (Item 7)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepProgressInsightsReelsCard(
    insights: List<ProgressInsightItem>,
    currentIndex: Int,
    onNextInsight: () -> Unit,
    onClose: () -> Unit
) {
    val insight = insights.getOrElse(currentIndex) {
        ProgressInsightItem("CONSISTENCY", "Stay Relentless", "Small daily actions build unstoppable empires, Boss.", "Bolt", Color(0xFFA855F7))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tag badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(insight.color.copy(alpha = 0.2f))
                .border(1.dp, insight.color, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "📊 ${insight.category} • #${currentIndex + 1}",
                color = insight.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        // Animated insight card
        AnimatedContent(
            targetState = insight,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                )
            },
            label = "ProgressInsightContent"
        ) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF120B2F))
                    .border(1.5.dp, item.color.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.message,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221555))
                    .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                    .clickable { onNextInsight() }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Next Insight ↑", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181036))
                    .border(1.dp, Color(0xFF33206A), RoundedCornerShape(8.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close ←", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Good Night & Wasted Time Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StepGoodNightSleepCard(
    sleepHour: Int,
    sleepMinute: Int,
    wastedMinutes: Int,
    onTimeClick: () -> Unit,
    onWastedChange: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GOOD NIGHT , BOSS",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Are you going to sleep now?",
            color = Color(0xFFA78BFA),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(20.dp))

        // Sleep Time card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF130D32).copy(alpha = 0.85f))
                .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(14.dp))
                .clickable { onTimeClick() }
                .padding(horizontal = 36.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d", sleepHour, sleepMinute),
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // Today's Wasted Time input
        Text(
            text = "Today's wasted time (minutes):",
            color = Color(0xFFCBD5E1),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1442))
                    .border(1.dp, Color(0xFF4C2889), RoundedCornerShape(8.dp))
                    .clickable { if (wastedMinutes >= 10) onWastedChange(wastedMinutes - 10) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF140B30))
                    .border(1.dp, Color(0xFF331E70), RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$wastedMinutes min",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1442))
                    .border(1.dp, Color(0xFF4C2889), RoundedCornerShape(8.dp))
                    .clickable { onWastedChange(wastedMinutes + 10) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Rectangular [ OKAY ] Button with curved edges
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF16A34A))
                .border(1.dp, Color(0xFF4ADE80), RoundedCornerShape(10.dp))
                .clickable { onConfirm() }
                .padding(horizontal = 48.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OKAY",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper Composables & Utilities
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SuggestItemRow(
    badge: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF120B2C))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(0.8.dp, badgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(title, color = TextWhite, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Go",
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun isTomorrow(dateText: String): Boolean {
    val clean = dateText.trim().uppercase()
    if (clean == "TOMORROW") return true
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthStr = SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.time).uppercase()
    val str1 = "$day $monthStr"
    val str2 = String.format("%02d %s", day, monthStr)
    return clean == str1 || clean == str2
}

/**
 * Builds data insights calculated directly from user's data (Item 7)
 */
fun buildProgressInsights(
    tasks: List<Task>,
    uniTasks: List<UniTask>,
    doItems: List<DoItem>,
    todayProgress: Int,
    weekWastedMinutes: List<Int>
): List<ProgressInsightItem> {
    val list = mutableListOf<ProgressInsightItem>()
    val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val cal = Calendar.getInstance()
    val todayIdx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)

    // 1. Progress on certain day
    for (i in 0 until todayIdx) {
        val spentOnDay = tasks.sumOf { it.weekLoggedMinutes.getOrElse(i) { 0 } }
        val targetOnDay = tasks.sumOf { it.targetMinutes }.coerceAtLeast(1)
        val dayPct = ((spentOnDay.toFloat() / targetOnDay) * 100).toInt().coerceIn(0, 100)
        if (dayPct > 0) {
            val praise = if (dayPct >= 75) "flawless execution!" else "solid effort and consistency!"
            list.add(
                ProgressInsightItem(
                    category = "DAILY SCORE",
                    title = "Progress on ${dayNames[i]}",
                    message = "On ${dayNames[i]}, you conquered $dayPct% of your planned targets — $praise",
                    iconType = "TrackChanges",
                    color = Color(0xFF10B981)
                )
            )
        }
    }

    // 2. Peak Performance
    var peakTaskName = ""
    var peakMins = 0
    var peakDayName = ""
    tasks.forEach { t ->
        t.weekLoggedMinutes.forEachIndexed { dayIdx, mins ->
            if (mins > peakMins) {
                peakMins = mins
                peakTaskName = t.name
                peakDayName = dayNames.getOrElse(dayIdx) { "this week" }
            }
        }
    }
    if (peakMins > 0) {
        list.add(
            ProgressInsightItem(
                category = "PEAK FOCUS",
                title = "Peak Focus: $peakTaskName",
                message = "Your highest peak was $peakMins minutes of deep work on '$peakTaskName' on $peakDayName! Outstanding discipline, Boss!",
                iconType = "Bolt",
                color = Color(0xFFA855F7)
            )
        )
    }

    // 3. Time spent on tasks till now
    tasks.sortedByDescending { it.weekLoggedMinutes.sum() + it.todayMinutes }.take(3).forEach { t ->
        val totalSpent = t.weekLoggedMinutes.sum() + t.todayMinutes
        if (totalSpent > 0) {
            val hours = totalSpent / 60
            val mins = totalSpent % 60
            val timeText = if (hours > 0) "${hours}h ${mins}m" else "${mins} mins"
            list.add(
                ProgressInsightItem(
                    category = "TIME INVESTED",
                    title = "Dedicated Focus: ${t.name}",
                    message = "You have invested $timeText into '${t.name}' this week so far. That compounding effort is building your mastery!",
                    iconType = "HourglassFull",
                    color = Color(0xFF06B6D4)
                )
            )
        }
    }

    // 4. Inactive tasks / "did this task ___ days ago at least do today"
    tasks.filter { !it.completedToday && it.todayMinutes == 0 }.forEach { t ->
        var daysAgo = 1
        for (i in (todayIdx - 1) downTo 0) {
            if (t.weekLoggedMinutes.getOrElse(i) { 0 } > 0) {
                daysAgo = todayIdx - i
                break
            }
        }
        val agoText = if (daysAgo == 1) "yesterday" else "$daysAgo days ago"
        list.add(
            ProgressInsightItem(
                category = "STREAK ALERT",
                title = "Keep Streak Alive: ${t.name}",
                message = "You worked on '${t.name}' $agoText, but haven't logged time today. At least do 15 minutes right now to protect your momentum!",
                iconType = "Warning",
                color = Color(0xFFF59E0B)
            )
        )
    }

    // 5. Completed on this day
    tasks.filter { it.completedToday }.forEach { t ->
        list.add(
            ProgressInsightItem(
                category = "CONQUERED",
                title = "Target Reached: ${t.name}",
                message = "You completed '${t.name}' today with 100% target! Take pride in this victory, Boss!",
                iconType = "CheckCircle",
                color = Color(0xFF22C55E)
            )
        )
    }
    doItems.filter { it.isCompleted }.take(2).forEach { d ->
        list.add(
            ProgressInsightItem(
                category = "CONQUERED",
                title = "To-Do Finished: ${d.name}",
                message = "You cleared '${d.name}' from your to-do radar today! One step closer to your vision.",
                iconType = "TaskAlt",
                color = Color(0xFF3B82F6)
            )
        )
    }

    // 6. Consistency growing / decreasing nowadays
    val primaryTasks = tasks.filter { it.isPrimary }
    val avgDiff = if (primaryTasks.isNotEmpty()) {
        primaryTasks.map { it.getAvgTimeDiff(todayIdx) }.average().toInt()
    } else 0

    if (avgDiff >= 0) {
        list.add(
            ProgressInsightItem(
                category = "CONSISTENCY TREND",
                title = "Consistency Growing!",
                message = "Boss, your consistency is growing (+${avgDiff}% above baseline this week)! You are outworking your previous limits!",
                iconType = "TrendingUp",
                color = Color(0xFF10B981)
            )
        )
    } else {
        list.add(
            ProgressInsightItem(
                category = "CONSISTENCY TREND",
                title = "Bounce-Back Opportunity",
                message = "Your weekly consistency is dipping slightly (${avgDiff}%). Remember: discipline means pushing when motivation fades. Today is your comeback!",
                iconType = "TrendingDown",
                color = Color(0xFFEF4444)
            )
        )
    }

    // 7. Uni-Task Habits
    val bestHabit = uniTasks.maxByOrNull { it.getWeekCompletedCount() }
    if (bestHabit != null && bestHabit.getWeekCompletedCount() > 0) {
        list.add(
            ProgressInsightItem(
                category = "HABIT STREAK",
                title = "Habit Mastery: ${bestHabit.name}",
                message = "Your daily habit '${bestHabit.name}' stands at ${bestHabit.getWeekCompletedCount()}/7 days completed this week! Unstoppable discipline.",
                iconType = "WorkspacePremium",
                color = Color(0xFFEAB308)
            )
        )
    }

    // 8. Today's overall progress
    list.add(
        ProgressInsightItem(
            category = "TODAY'S SCORE",
            title = "Live Progress: $todayProgress%",
            message = if (todayProgress >= 100) "100% TARGET REACHED! You owned the day, Boss!" else "You are currently at $todayProgress% today. Every minute you put in right now brings you closer to 100%!",
            iconType = "Star",
            color = Color(0xFFA855F7)
        )
    )

    // 9. Wasted time control
    val totalWasted = weekWastedMinutes.take(todayIdx + 1).sum()
    list.add(
        ProgressInsightItem(
            category = "TIME DEFENSE",
            title = "Wasted Time Defense",
            message = "You have logged $totalWasted mins of lost time this week. Guard your attention relentlessly, Boss — it's your rarest currency.",
            iconType = "Security",
            color = Color(0xFF6366F1)
        )
    )

    if (list.size < 3) {
        list.add(
            ProgressInsightItem(
                category = "MINDSET",
                title = "The Power of Consistency",
                message = "Success isn't always about greatness. It's about consistency. Consistent hard work leads to success. Greatness will come.",
                iconType = "Psychology",
                color = Color(0xFF8B5CF6)
            )
        )
    }

    return list
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Canvas Cute Pet Character
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CutePetCharacter(
    modifier: Modifier = Modifier,
    jumpTrigger: Int = 0,
    isSleepy: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PetAnimation")

    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceY"
    )

    val earWaggle by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EarWaggle"
    )

    val eyeScaleY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                1f at 0
                1f at 3000
                0.1f at 3100
                1f at 3200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "EyeBlink"
    )

    val jumpOffsetY = remember { Animatable(0f) }
    LaunchedEffect(jumpTrigger) {
        if (jumpTrigger > 0) {
            jumpOffsetY.animateTo(-16f, tween(150, easing = EaseOutQuad))
            jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val totalOffsetY = bounceY + jumpOffsetY.value

        withTransform({
            translate(top = totalOffsetY)
        }) {
            val centerX = width / 2f
            val centerY = height / 2f

            val faceWidth = width * 0.70f
            val faceHeight = height * 0.52f
            val faceLeft = centerX - faceWidth / 2f
            val faceTop = centerY - faceHeight / 2f + 4f

            // ── Caramel Puppy Ears ──
            val earWidth = faceWidth * 0.36f
            val earHeight = faceHeight * 0.75f
            val earColor = Color(0xFFE58D26) // Warm caramel amber

            // Left Ear
            withTransform({
                rotate(
                    degrees = -30f + earWaggle,
                    pivot = Offset(faceLeft + 8f, faceTop + 14f)
                )
            }) {
                drawRoundRect(
                    color = earColor,
                    topLeft = Offset(faceLeft - 6f, faceTop - 12f),
                    size = Size(earWidth, earHeight),
                    cornerRadius = CornerRadius(earWidth / 2f, earWidth / 2f)
                )
            }

            // Right Ear
            withTransform({
                rotate(
                    degrees = 30f - earWaggle,
                    pivot = Offset(faceLeft + faceWidth - 8f, faceTop + 14f)
                )
            }) {
                drawRoundRect(
                    color = earColor,
                    topLeft = Offset(faceLeft + faceWidth - earWidth + 6f, faceTop - 12f),
                    size = Size(earWidth, earHeight),
                    cornerRadius = CornerRadius(earWidth / 2f, earWidth / 2f)
                )
            }

            // ── Face Squircle ──
            drawRoundRect(
                color = Color(0xFFF9FAFB),
                topLeft = Offset(faceLeft, faceTop),
                size = Size(faceWidth, faceHeight),
                cornerRadius = CornerRadius(faceHeight * 0.45f, faceHeight * 0.45f)
            )

            // ── Blushing Pink Cheeks ──
            val cheekRadius = faceWidth * 0.09f
            val cheekY = centerY + 8f
            drawCircle(
                color = Color(0xFFFF7597).copy(alpha = 0.55f),
                radius = cheekRadius,
                center = Offset(faceLeft + faceWidth * 0.18f, cheekY)
            )
            drawCircle(
                color = Color(0xFFFF7597).copy(alpha = 0.55f),
                radius = cheekRadius,
                center = Offset(faceLeft + faceWidth * 0.82f, cheekY)
            )

            // ── Eyes (Sleepy or Happy Open) ──
            val eyeColor = Color(0xFF1B1B2F)
            val eyeRadiusX = faceWidth * 0.055f
            val eyeRadiusY = if (isSleepy) 1.5f else (faceHeight * 0.075f) * eyeScaleY
            val eyeY = centerY + 1f

            if (isSleepy) {
                // Sleepy curved lines (closed eyes)
                drawArc(
                    color = eyeColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(faceLeft + faceWidth * 0.32f - 7f, eyeY - 4f),
                    size = Size(14f, 8f),
                    style = Stroke(width = 2.5f)
                )
                drawArc(
                    color = eyeColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(faceLeft + faceWidth * 0.68f - 7f, eyeY - 4f),
                    size = Size(14f, 8f),
                    style = Stroke(width = 2.5f)
                )
            } else {
                // Left Eye
                drawRoundRect(
                    color = eyeColor,
                    topLeft = Offset(faceLeft + faceWidth * 0.32f - eyeRadiusX, eyeY - eyeRadiusY),
                    size = Size(eyeRadiusX * 2, eyeRadiusY * 2),
                    cornerRadius = CornerRadius(eyeRadiusX, eyeRadiusX)
                )
                // Left Eye Sparkle
                if (eyeScaleY > 0.4f) {
                    drawCircle(
                        color = Color.White,
                        radius = eyeRadiusX * 0.4f,
                        center = Offset(faceLeft + faceWidth * 0.32f - eyeRadiusX * 0.25f, eyeY - eyeRadiusY * 0.35f)
                    )
                }

                // Right Eye
                drawRoundRect(
                    color = eyeColor,
                    topLeft = Offset(faceLeft + faceWidth * 0.68f - eyeRadiusX, eyeY - eyeRadiusY),
                    size = Size(eyeRadiusX * 2, eyeRadiusY * 2),
                    cornerRadius = CornerRadius(eyeRadiusX, eyeRadiusX)
                )
                // Right Eye Sparkle
                if (eyeScaleY > 0.4f) {
                    drawCircle(
                        color = Color.White,
                        radius = eyeRadiusX * 0.4f,
                        center = Offset(faceLeft + faceWidth * 0.68f - eyeRadiusX * 0.25f, eyeY - eyeRadiusY * 0.35f)
                    )
                }
            }

            // ── Golden Nose ──
            val noseRadius = faceWidth * 0.045f
            drawCircle(
                color = Color(0xFFD97706),
                radius = noseRadius,
                center = Offset(centerX, centerY + 3f)
            )

            // ── Smiling Mouth ──
            val mouthY = centerY + 8f
            drawArc(
                color = eyeColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(centerX - 8f, mouthY - 4f),
                size = Size(16f, 10f),
                style = Stroke(width = 2f)
            )

            // ── Tiny Paws ──
            val pawColor = Color(0xFFFFFFFF)
            val pawY = faceTop + faceHeight - 4f
            drawRoundRect(
                color = pawColor,
                topLeft = Offset(faceLeft + faceWidth * 0.24f, pawY),
                size = Size(faceWidth * 0.20f, faceHeight * 0.18f),
                cornerRadius = CornerRadius(faceWidth * 0.10f, faceWidth * 0.10f)
            )
            drawRoundRect(
                color = pawColor,
                topLeft = Offset(faceLeft + faceWidth * 0.56f, pawY),
                size = Size(faceWidth * 0.20f, faceHeight * 0.18f),
                cornerRadius = CornerRadius(faceWidth * 0.10f, faceWidth * 0.10f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interactive Time Picker Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WakeUpTimePickerDialog(
    title: String = "Set Time",
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF10092B))
                .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▲",
                            color = PurplePrimary,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { selectedHour = (selectedHour + 1) % 24 }
                                .padding(8.dp)
                        )
                        Text(
                            text = String.format("%02d", selectedHour),
                            color = TextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "▼",
                            color = PurplePrimary,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }
                                .padding(8.dp)
                        )
                    }

                    Text(
                        text = " : ",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Minute selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▲",
                            color = PurplePrimary,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { selectedMinute = (selectedMinute + 5) % 60 }
                                .padding(8.dp)
                        )
                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = TextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "▼",
                            color = PurplePrimary,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5 }
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF221555))
                            .border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Cancel", color = TextMuted, fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E1A68))
                            .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable { onConfirm(selectedHour, selectedMinute) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Set", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
