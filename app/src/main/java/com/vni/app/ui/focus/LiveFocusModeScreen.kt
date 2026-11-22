package com.vni.app.ui.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.SimpleHomeViewModel
import kotlinx.coroutines.delay

/**
 * Live Focus Mode Screen (Image 5):
 * - Top Motive Sentences (changes every 7 seconds)
 * - Session Name in bold violet (e.g. STUDY)
 * - Center rotating Iron Man Arc Reactor with glowing violet progress ring
 * - Single click reactor to Pause / Resume (reduces opacity on pause)
 * - Long-press reactor to Stop session
 * - Displays digital countdown or stopwatch time
 * - [ BREAK ] button at bottom
 * - 3 epic sci-fi sound effects (start, pause/break, stop)
 */
@Composable
fun LiveFocusModeScreen(
    sessionName: String,
    mode: String, // "TIMER", "STOPWATCH", "INTERVALS"
    targetDurationSeconds: Long,
    breakDurationSeconds: Long,
    intervalFocusSeconds: Long,
    intervalBreakSeconds: Long,
    motiveQuotes: List<String>,
    onFinishSession: (elapsedSeconds: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Prevent exiting without confirming stop
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    BackHandler { showStopConfirmDialog = true }

    // Session timer state
    var isRunning by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var isBreakActive by remember { mutableStateOf(false) }
    var breakElapsedSeconds by remember { mutableStateOf(0L) }

    // Motive quotes rotating every 7 seconds
    val defaultQuotes = remember {
        listOf(
            "Never regard Study as a duty , But as the eviable Oppurtunity to learn",
            "Concentrate all your thoughts upon the work at hand. The sun's rays do not burn until brought to a focus.",
            "You either change or being struck. Keep pushing.",
            "Do not wait to strike till the iron is hot; but make it hot by striking.",
            "Discipline is choosing between what you want now and what you want most.",
            "Energy flows where attention goes. Focus on your mastery.",
            "Starve your distractions, feed your focus."
        )
    }
    val allQuotes = remember(motiveQuotes) {
        if (motiveQuotes.isNotEmpty()) motiveQuotes else defaultQuotes
    }
    var currentQuoteIndex by remember { mutableStateOf(0) }

    // Auto rotate quote every 7 seconds
    LaunchedEffect(allQuotes) {
        while (true) {
            delay(7000)
            currentQuoteIndex = (currentQuoteIndex + 1) % allQuotes.size
        }
    }

    // Play Iron Man start sound once on launch
    LaunchedEffect(Unit) {
        FocusSoundPlayer.playStartSound()
    }

    // Main 1-second timer loop
    LaunchedEffect(isRunning, isPaused, isBreakActive) {
        while (isRunning && !isPaused) {
            delay(1000)
            if (!isBreakActive) {
                elapsedSeconds += 1
                // In Timer mode: check if target reached
                if (mode == "TIMER" && targetDurationSeconds > 0 && elapsedSeconds >= targetDurationSeconds) {
                    FocusSoundPlayer.playStopSound()
                    onFinishSession(elapsedSeconds)
                    break
                }
                // In Intervals mode: check if focus interval finished -> trigger break automatically!
                if (mode == "INTERVALS" && intervalFocusSeconds > 0 && (elapsedSeconds % intervalFocusSeconds == 0L)) {
                    isBreakActive = true
                    breakElapsedSeconds = 0L
                    FocusSoundPlayer.playPauseBreakSound()
                }
            } else {
                breakElapsedSeconds += 1
                val activeBreakLimit = if (mode == "INTERVALS") intervalBreakSeconds else breakDurationSeconds
                if (activeBreakLimit > 0 && breakElapsedSeconds >= activeBreakLimit) {
                    // Break finished -> auto resume focus!
                    isBreakActive = false
                    breakElapsedSeconds = 0L
                    FocusSoundPlayer.playStartSound()
                }
            }
        }
    }

    // Progress calculation (0f to 1f) for violet progress arc
    val progress = remember(elapsedSeconds, targetDurationSeconds, mode, isBreakActive, breakElapsedSeconds) {
        when {
            isBreakActive -> {
                val limit = if (mode == "INTERVALS") intervalBreakSeconds else breakDurationSeconds
                if (limit > 0) (breakElapsedSeconds.toFloat() / limit).coerceIn(0f, 1f) else 0f
            }
            mode == "TIMER" -> {
                if (targetDurationSeconds > 0) (elapsedSeconds.toFloat() / targetDurationSeconds).coerceIn(0f, 1f) else 0f
            }
            mode == "INTERVALS" -> {
                if (intervalFocusSeconds > 0) ((elapsedSeconds % intervalFocusSeconds).toFloat() / intervalFocusSeconds).coerceIn(0f, 1f) else 0f
            }
            else -> { // STOPWATCH
                ((elapsedSeconds % 3600).toFloat() / 3600f).coerceIn(0f, 1f)
            }
        }
    }

    // Digital time string calculation
    val displayTimeString = remember(elapsedSeconds, targetDurationSeconds, mode, isBreakActive, breakElapsedSeconds) {
        if (isBreakActive) {
            val limit = if (mode == "INTERVALS") intervalBreakSeconds else breakDurationSeconds
            val remaining = (limit - breakElapsedSeconds).coerceAtLeast(0)
            val m = remaining / 60
            val s = remaining % 60
            String.format("%02d:%02d", m, s)
        } else {
            when (mode) {
                "TIMER" -> {
                    val remaining = (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0)
                    val m = remaining / 60
                    val s = remaining % 60
                    String.format("%02d:%02d", m, s)
                }
                "INTERVALS" -> {
                    val remainingInInterval = (intervalFocusSeconds - (elapsedSeconds % intervalFocusSeconds)).coerceAtLeast(0)
                    val m = remainingInInterval / 60
                    val s = remainingInInterval % 60
                    String.format("%02d:%02d", m, s)
                }
                else -> { // STOPWATCH
                    val h = elapsedSeconds / 3600
                    val m = (elapsedSeconds % 3600) / 60
                    val s = elapsedSeconds % 60
                    if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
                    else String.format("%02d:%02d", m, s)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP: Motive Sentence (Rotates every 7 seconds) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = allQuotes.getOrElse(currentQuoteIndex) { allQuotes.first() },
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "quote_anim"
                ) { quote ->
                    Text(
                        text = quote,
                        color = Color(0xFFE2E8F0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── MIDDLE SECTION: Focus Name, Arc Reactor & Timer ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Big Bold Session Name
                Text(
                    text = if (isBreakActive) "BREAK TIME" else sessionName.uppercase(),
                    color = if (isBreakActive) Color(0xFF38BDF8) else Color(0xFFA855F7),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(10.dp))

                // Iconic Rotating Iron Man Arc Reactor
                ArcReactorView(
                    reactorSize = 230.dp,
                    isRunning = isRunning,
                    isPaused = isPaused,
                    progress = progress,
                    showProgressRing = true,
                    showPowerBeam = !isPaused,
                    onSingleTap = {
                        // Single tap: Pause / Resume
                        isPaused = !isPaused
                        if (isPaused) {
                            FocusSoundPlayer.playPauseBreakSound()
                        } else {
                            FocusSoundPlayer.playStartSound()
                        }
                    },
                    onLongPress = {
                        // Long press (1 second hold): Stop
                        showStopConfirmDialog = true
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Digital Timer Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayTimeString,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (isPaused) "TAP TO RESUME • HOLD TO STOP" else if (isBreakActive) "RESTING • CHARGING" else "TAP REACTOR TO PAUSE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // ── BOTTOM: [ BREAK ] / Resume Button ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isBreakActive) {
                                Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFF4C1D95)))
                            }
                        )
                        .clickable {
                            if (!isBreakActive) {
                                // Start break
                                isBreakActive = true
                                breakElapsedSeconds = 0L
                                FocusSoundPlayer.playPauseBreakSound()
                            } else {
                                // End break early & resume focus
                                isBreakActive = false
                                breakElapsedSeconds = 0L
                                FocusSoundPlayer.playStartSound()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBreakActive) "RESUME FOCUS" else "BREAK",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // ── Stop Confirmation Dialog ──
        if (showStopConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmDialog = false },
                containerColor = Color(0xFF13092C),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCBD5E1),
                title = { Text("Stop Focus Session?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Your completed focused time ($displayTimeString) will be saved to today's summary and weekly stats.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStopConfirmDialog = false
                            isRunning = false
                            FocusSoundPlayer.playStopSound()
                            onFinishSession(elapsedSeconds)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Stop & Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}
