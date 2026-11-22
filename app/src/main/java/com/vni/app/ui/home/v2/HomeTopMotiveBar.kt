package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.MotiveHeading
import kotlinx.coroutines.delay

/**
 * Section A:
 * - App logo at top corner: 3 nested chevrons.
 *   - Normal state: Top chevron is white/lavender, bottom two are purple. Sentences show randomly from all blocks.
 *   - Toggled state (clicked): All three glow purple with neon bloom. Sentences are filtered to the current block (e.g. "Motive").
 * - Bell icon (Notification toggle with glowing dot): Clicking it toggles notifications on/off.
 * - Settings icon: Opens settings (Password & Cloud Backup / Firebase upload button).
 * - Motivational sentence in center: Mountain silhouette with purple sun/moon crescent glow.
 * - Bottom right pill: Shows current sentence category: `◀ Motive ▶` (with arrows to switch).
 */
@Composable
fun HomeTopMotiveBar(
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect all categories and sentences
    val defaultCategories = remember {
        listOf(
            "Motive" to listOf(
                "Nobody comes" to "You either change or being struck",
                "Discipline is destiny" to "Win the battle with yourself every day",
                "Push your limits" to "Growth happens outside your comfort zone"
            ),
            "Good Words" to listOf(
                "Consistency transforms average" to "Small daily steps lead to massive results",
                "Energy flows where attention goes" to "Stay dialed in and focused",
                "Do something today" to "Your future self will thank you for it"
            ),
            "Mindset" to listOf(
                "The secret of getting ahead" to "Is simply getting started",
                "Don't stop when tired" to "Stop when you are completely done"
            )
        )
    }

    // Merge with user-defined motiveHeadings if available
    val categoriesMap = remember(motiveHeadings) {
        val map = mutableMapOf<String, List<Pair<String, String>>>()
        defaultCategories.forEach { (cat, list) ->
            map[cat] = list
        }
        motiveHeadings.forEach { mh ->
            if (mh.sentences.isNotEmpty()) {
                val parsed = mh.sentences.map { s ->
                    val parts = when {
                        s.contains(":") -> s.split(":", limit = 2).map { it.trim() }
                        s.contains("\n") -> s.split("\n", limit = 2).map { it.trim() }
                        s.contains("-") -> s.split("-", limit = 2).map { it.trim() }
                        else -> listOf(s.trim(), "Stay consistent and keep pushing forward")
                    }
                    val title = parts.getOrNull(0)?.ifBlank { "Stay Strong" } ?: "Stay Strong"
                    val subtitle = parts.getOrNull(1)?.ifBlank { "Keep going" } ?: "Keep going"
                    title to subtitle
                }
                map[mh.heading.trim()] = parsed
            }
        }
        map
    }

    val categoryNames = remember(categoriesMap) { categoriesMap.keys.toList() }
    var currentCatIndex by remember { mutableIntStateOf(0) }
    val activeCategory = categoryNames.getOrElse(currentCatIndex) { "Motive" }

    // Logo filter toggle:
    // false = normal state (sentences from all categories randomly)
    // true = locked to present block (all 3 chevrons glow purple!)
    var isLogoCategoryLocked by remember { mutableStateOf(false) }

    // Sentences pool based on mode
    val currentSentencePool = remember(isLogoCategoryLocked, activeCategory, categoriesMap) {
        if (isLogoCategoryLocked) {
            categoriesMap[activeCategory] ?: emptyList()
        } else {
            categoriesMap.values.flatten()
        }
    }

    var sentenceIndex by remember { mutableIntStateOf(0) }

    // Auto-rotate sentences every 7 seconds; reset 7s timer whenever user taps
    LaunchedEffect(sentenceIndex, currentSentencePool) {
        if (currentSentencePool.isNotEmpty()) {
            delay(7000L)
            sentenceIndex = (sentenceIndex + 1) % currentSentencePool.size
        }
    }
    val activeSentence = remember(currentSentencePool, sentenceIndex) {
        if (currentSentencePool.isNotEmpty()) {
            currentSentencePool[sentenceIndex % currentSentencePool.size]
        } else {
            "Nobody comes" to "You either change or being struck"
        }
    }

    // Find category for the active sentence
    val activeSentenceCategory = remember(activeSentence, categoriesMap) {
        categoriesMap.entries.firstOrNull { it.value.contains(activeSentence) }?.key ?: activeCategory
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // ── Top Row: App Logo (3 chevrons) on Left | Bell & Settings on Right ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo: 3 nested chevrons
            AppLogoChevrons(
                isAllPurpleGlow = isLogoCategoryLocked,
                onClick = {
                    isLogoCategoryLocked = !isLogoCategoryLocked
                }
            )

            // Right Action Buttons: Bell & Settings
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bell (Notifications) Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF130A2A))
                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                        .clickable { onToggleNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                        contentDescription = "Notifications",
                        tint = if (notificationsEnabled) Color.White else Color(0xFF71717A),
                        modifier = Modifier.size(20.dp)
                    )

                    // Glowing dot badge when notifications are ON
                    if (notificationsEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC084FC))
                                .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFFC084FC))
                        )
                    }
                }

                // Settings Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF130A2A))
                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Center: Motive Sentences with Mountain & Purple Eclipse Crest Glow ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Tap on sentence cycles to the next sentence
                    sentenceIndex = (sentenceIndex + 1) % currentSentencePool.size.coerceAtLeast(1)
                },
            contentAlignment = Alignment.Center
        ) {
            // Background Purple Glow Crest & Mountain Horizon Art
            MotiveAtmosphereBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            // Sentence Text with Smooth Transition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Split title to give that dynamic two-tone purple flair from image ("Nobody" white, "comes" purple)
                val words = activeSentence.first.split(" ")
                val firstWord = words.firstOrNull() ?: ""
                val restWords = words.drop(1).joinToString(" ")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$firstWord ",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    if (restWords.isNotEmpty()) {
                        Text(
                            text = restWords,
                            color = Color(0xFFA855F7), // Glowing purple accent
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = activeSentence.second,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Bottom Right: Category Tag `◀ Motive ▶` ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF130928))
                    .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left Arrow (cycles to previous category or sentence)
                    Text(
                        text = "◀",
                        color = Color(0xFFC084FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                currentCatIndex = if (currentCatIndex > 0) currentCatIndex - 1 else categoryNames.size - 1
                                sentenceIndex = 0
                            }
                            .padding(horizontal = 2.dp)
                    )

                    Text(
                        text = activeSentenceCategory,
                        color = Color(0xFFF1F5F9),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Right Arrow (cycles to next category or sentence)
                    Text(
                        text = "▶",
                        color = Color(0xFFC084FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                currentCatIndex = (currentCatIndex + 1) % categoryNames.size
                                sentenceIndex = 0
                            }
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-chevron App Logo:
 * - When [isAllPurpleGlow] is false (normal state):
 *   Top chevron: White/lavender
 *   Middle & bottom chevrons: Purple
 * - When [isAllPurpleGlow] is true (toggled state):
 *   All 3 chevrons glow electric purple with outer blur glow!
 */
@Composable
fun AppLogoChevrons(
    isAllPurpleGlow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColor by animateColorAsState(
        targetValue = if (isAllPurpleGlow) Color(0xFFD946EF) else Color.Transparent,
        animationSpec = tween(400),
        label = "LogoGlow"
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isAllPurpleGlow) Color(0xFF2A0D52) else Color.Transparent)
            .border(
                width = if (isAllPurpleGlow) 1.5.dp else 0.dp,
                color = if (isAllPurpleGlow) Color(0xFFA855F7) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 3 Chevrons stacked vertically pointing upwards:
            // Chevron 1 (Top)
            // Chevron 2 (Middle)
            // Chevron 3 (Bottom)
            val strokeWidth = 3.6.dp.toPx()

            fun drawChevron(yCenter: Float, color: Color) {
                val path = Path().apply {
                    moveTo(w * 0.18f, yCenter + h * 0.12f)
                    lineTo(w * 0.50f, yCenter - h * 0.12f)
                    lineTo(w * 0.82f, yCenter + h * 0.12f)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }

            if (isAllPurpleGlow) {
                // All three glow vibrant purple!
                drawChevron(h * 0.30f, Color(0xFFE879F9))
                drawChevron(h * 0.52f, Color(0xFFA855F7))
                drawChevron(h * 0.74f, Color(0xFF7C3AED))
            } else {
                // Normal state matching Image:
                // Top chevron: White / light lavender
                // Middle chevron: Vibrant purple
                // Bottom chevron: Deep purple
                drawChevron(h * 0.30f, Color(0xFFFFFFFF))
                drawChevron(h * 0.52f, Color(0xFFA855F7))
                drawChevron(h * 0.74f, Color(0xFF6B21A8))
            }
        }
    }
}

/**
 * Atmospheric background canvas drawing:
 * Silhouette mountain range with glowing purple horizon / crescent moon eclipse crest.
 */
@Composable
private fun MotiveAtmosphereBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Radiant purple aura behind the mountain crest
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA855F7).copy(alpha = 0.55f),
                    Color(0xFF7C3AED).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.82f),
                radius = w * 0.40f
            ),
            radius = w * 0.40f,
            center = Offset(w * 0.5f, h * 0.82f)
        )

        // Luminous glowing purple crest (like the solar eclipse rim in image)
        val crestPath = Path().apply {
            moveTo(w * 0.32f, h * 0.88f)
            quadraticTo(w * 0.50f, h * 0.52f, w * 0.68f, h * 0.88f)
        }
        drawPath(
            path = crestPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE879F9),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dark Mountain Silhouette line
        val mountainPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.94f)
            lineTo(w * 0.22f, h * 0.85f)
            lineTo(w * 0.38f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.80f)
            lineTo(w * 0.62f, h * 0.88f)
            lineTo(w * 0.78f, h * 0.84f)
            lineTo(w, h * 0.95f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF130924).copy(alpha = 0.90f),
                    Color(0xFF080415)
                )
            )
        )
    }
}
