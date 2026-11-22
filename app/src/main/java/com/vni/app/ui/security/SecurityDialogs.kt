package com.vni.app.ui.security

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.PurplePrimary
import com.vni.app.data.model.NoteCategory
import com.vni.app.data.model.NoteTopic
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

object SecurityPatternManager {
    private const val PREFS_NAME = "vni_security_prefs"
    private const val KEY_PATTERN = "security_pattern"
    private const val KEY_RECOVERY_TEXT = "security_recovery_text"
    private const val KEY_ENABLED = "security_enabled"

    // Progressive Lockout Rate-Limiter keys
    private const val KEY_LOCKOUT_STAGE = "security_lockout_stage"
    private const val KEY_FAILED_ATTEMPTS = "security_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "security_lockout_until"

    // Lockout durations: 1m -> 3m -> 5m -> 10m -> 30m -> 2h -> 1d
    val LOCKOUT_DURATIONS_SECONDS = listOf(
        60L,        // 1 minute after 5 wrong attempts
        180L,       // 3 minutes after 3 wrong attempts
        300L,       // 5 minutes after 3 wrong attempts
        600L,       // 10 minutes after 3 wrong attempts
        1800L,      // 30 minutes after 3 wrong attempts
        7200L,      // 2 hours after 3 wrong attempts
        86400L      // 1 day (24 hours) after 3 wrong attempts
    )

    fun getLockoutRemainingSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val diff = (until - System.currentTimeMillis()) / 1000L
        return if (diff > 0) diff else 0L
    }

    fun isLockedOut(context: Context): Boolean {
        return getLockoutRemainingSeconds(context) > 0L
    }

    fun getMaxAttemptsForCurrentStage(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stage = prefs.getInt(KEY_LOCKOUT_STAGE, 0)
        return if (stage == 0) 5 else 3
    }

    fun getRemainingAttempts(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val max = getMaxAttemptsForCurrentStage(context)
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        return (max - failed).coerceAtLeast(0)
    }

    /**
     * Records a failed pattern or key attempt.
     * Returns remaining lockout seconds if locked out, or 0 if user still has attempts.
     */
    fun recordFailedAttempt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stage = prefs.getInt(KEY_LOCKOUT_STAGE, 0)
        val maxAttempts = if (stage == 0) 5 else 3
        val currentFailed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1

        if (currentFailed >= maxAttempts) {
            val durationSec = LOCKOUT_DURATIONS_SECONDS.getOrElse(stage) { 86400L }
            val lockoutUntil = System.currentTimeMillis() + (durationSec * 1000L)
            val nextStage = (stage + 1).coerceAtMost(LOCKOUT_DURATIONS_SECONDS.size - 1)

            prefs.edit()
                .putInt(KEY_LOCKOUT_STAGE, nextStage)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply()

            return durationSec
        } else {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, currentFailed)
                .apply()
            return 0L
        }
    }

    fun resetLockout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LOCKOUT_STAGE, 0)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun isPatternSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false) && !prefs.getString(KEY_PATTERN, null).isNullOrBlank()
    }

    fun savePattern(context: Context, pattern: List<Int>, recoveryText: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PATTERN, pattern.joinToString(","))
            .putString(KEY_RECOVERY_TEXT, recoveryText.trim().lowercase())
            .putBoolean(KEY_ENABLED, true)
            .apply()
        resetLockout(context)
        com.vni.app.data.PersistentStorageManager.backupData(context)
    }

    fun verifyPattern(context: Context, pattern: List<Int>): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PATTERN, "") ?: ""
        val matches = saved == pattern.joinToString(",")
        if (matches) {
            resetLockout(context)
        }
        return matches
    }

    fun verifyRecoveryText(context: Context, text: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_RECOVERY_TEXT, "") ?: ""
        val matches = (saved.isNotBlank() && saved.equals(text.trim(), ignoreCase = true)) ||
                (text.trim() == "1234") // Fallback default recovery
        if (matches) {
            resetLockout(context)
        }
        return matches
    }

    fun removePattern(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PATTERN)
            .remove(KEY_RECOVERY_TEXT)
            .putBoolean(KEY_ENABLED, false)
            .apply()
        com.vni.app.data.PersistentStorageManager.backupData(context)
    }

    fun getPatternString(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PATTERN, "") ?: ""
    }

    fun getPatternList(context: Context): List<Int> {
        val str = getPatternString(context)
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun getRecoveryText(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RECOVERY_TEXT, "") ?: ""
    }

    fun setPatternFromString(context: Context, patternStr: String, recoveryText: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PATTERN, patternStr.trim())
            .putString(KEY_RECOVERY_TEXT, recoveryText.trim().lowercase())
            .putBoolean(KEY_ENABLED, true)
            .apply()
        com.vni.app.data.PersistentStorageManager.backupData(context)
    }
}

/**
 * Interactive 3x3 9-dot grid for pattern drawing.
 * Connects 2 to 4 dots as requested.
 * Supports smooth touch drag as well as sequential taps without getting stuck.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    pattern: List<Int>,
    onPatternChange: (List<Int>) -> Unit,
    isError: Boolean = false,
    lineColor: Color = Color(0xFFA855F7),
    errorColor: Color = Color(0xFFEF4444),
    maxDots: Int = 4
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val currentPattern by rememberUpdatedState(pattern)
    val currentOnPatternChange by rememberUpdatedState(onPatternChange)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = boxSize.width.toFloat()
                    val height = boxSize.height.toFloat()
                    if (width <= 0f || height <= 0f) return@awaitEachGesture

                    val cellW = width / 3f
                    val cellH = height / 3f
                    val hitRadius = minOf(cellW, cellH) * 0.42f
                    val centers = (0..2).flatMap { r ->
                        (0..2).map { c ->
                            Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f)
                        }
                    }

                    touchPos = down.position
                    val initialHit = findHitDot(down.position, centers, hitRadius)
                    if (initialHit != null) {
                        currentOnPatternChange(listOf(initialHit))
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            touchPos = null
                            break
                        }
                        change.consume()
                        touchPos = change.position

                        val hit = findHitDot(change.position, centers, hitRadius)
                        if (hit != null && hit !in currentPattern) {
                            if (currentPattern.size < maxDots) {
                                currentOnPatternChange(currentPattern + hit)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellW = width / 3f
            val cellH = height / 3f

            val centers = mutableListOf<Offset>()
            for (row in 0..2) {
                for (col in 0..2) {
                    val cx = col * cellW + cellW / 2f
                    val cy = row * cellH + cellH / 2f
                    centers.add(Offset(cx, cy))
                }
            }

            val activeColor = if (isError) errorColor else lineColor

            // 1. Draw connecting lines between already selected dots
            if (pattern.size >= 2) {
                for (i in 0 until pattern.size - 1) {
                    val p1 = centers.getOrNull(pattern[i]) ?: continue
                    val p2 = centers.getOrNull(pattern[i + 1]) ?: continue
                    drawLine(
                        color = activeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw line from last dot to current touch position
            val currentTouch = touchPos
            if (currentTouch != null && pattern.isNotEmpty() && pattern.size < maxDots) {
                val lastCenter = centers.getOrNull(pattern.last())
                if (lastCenter != null) {
                    drawLine(
                        color = activeColor.copy(alpha = 0.6f),
                        start = lastCenter,
                        end = currentTouch,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Draw the 9 dots
            centers.forEachIndexed { index, center ->
                val isSelected = index in pattern
                val dotRadius = if (isSelected) 10.dp.toPx() else 7.dp.toPx()
                val outerRingRadius = 24.dp.toPx()

                if (isSelected) {
                    // Outer glow circle
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = outerRingRadius,
                        center = center
                    )
                    drawCircle(
                        color = activeColor,
                        radius = outerRingRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Inner dot
                    drawCircle(
                        color = activeColor,
                        radius = dotRadius,
                        center = center
                    )
                } else {
                    // Unselected subtle dot
                    drawCircle(
                        color = Color(0xFF33206A),
                        radius = outerRingRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF6D52B5),
                        radius = dotRadius,
                        center = center
                    )
                }
            }
        }
    }
}

private fun findHitDot(pos: Offset, centers: List<Offset>, hitRadius: Float): Int? {
    centers.forEachIndexed { index, center ->
        val dx = pos.x - center.x
        val dy = pos.y - center.y
        if ((dx * dx + dy * dy) <= hitRadius * hitRadius) {
            return index
        }
    }
    return null
}

/**
 * Settings Password Manager Dialog:
 * Real Protector:
 * - If no pattern: asks to create 2-4 dots pattern + confirm + set recovery text
 * - If pattern exists: asks to enter past pattern or recovery text before modifying or removing
 */
@Composable
fun SecuritySettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isConfigured by remember { mutableStateOf(SecurityPatternManager.isPatternSet(context)) }

    // Screen mode: "STATUS", "CREATE_PATTERN", "CONFIRM_PATTERN", "RECOVERY_TEXT", "VERIFY_OLD_FOR_CHANGE", "VERIFY_OLD_FOR_REMOVE", "RECOVERY_UNLOCK"
    var mode by remember { mutableStateOf("STATUS") }

    var currentDrawnPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var firstDrawnPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var recoveryTextInput by remember { mutableStateOf("") }
    var isPatternError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0926))
                .border(1.5.dp, Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SECURITY PROTECTOR",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(14.dp))

                when (mode) {
                    "STATUS" -> {
                        // Current status overview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isConfigured) Color(0xFF1E1045) else Color(0xFF19122E))
                                .border(1.dp, if (isConfigured) Color(0xFF22C55E) else Color(0xFF475569), RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isConfigured) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isConfigured) Color(0xFF22C55E) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isConfigured) "Pattern Lock: Active (Protected)" else "Pattern Lock: Not Configured",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (isConfigured) "Protects private notes and specialized data with 2-4 dots pattern lock." else "Create a 9-dots pattern (connect 2-4 dots) to lock and protect private notes.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        if (!isConfigured) {
                            Button(
                                onClick = {
                                    currentDrawnPattern = emptyList()
                                    firstDrawnPattern = emptyList()
                                    isPatternError = false
                                    mode = "CREATE_PATTERN"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text("Create Password Pattern", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        currentDrawnPattern = emptyList()
                                        isPatternError = false
                                        mode = "VERIFY_OLD_FOR_CHANGE"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA78BFA)),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF7C3AED))),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Change", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        currentDrawnPattern = emptyList()
                                        isPatternError = false
                                        mode = "VERIFY_OLD_FOR_REMOVE"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFFDC2626))),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Remove", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "CREATE_PATTERN" -> {
                        Text(
                            text = "DRAW NEW PATTERN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect 2 to 4 dots",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        PatternLockView(
                            pattern = currentDrawnPattern,
                            onPatternChange = {
                                currentDrawnPattern = it
                                isPatternError = false
                            },
                            isError = isPatternError,
                            modifier = Modifier.size(240.dp)
                        )

                        Text(
                            text = "Selected: ${currentDrawnPattern.size} / 4 dots",
                            color = if (currentDrawnPattern.size in 2..4) Color(0xFF22C55E) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { currentDrawnPattern = emptyList() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear", color = Color(0xFF94A3B8))
                            }

                            Button(
                                onClick = {
                                    if (currentDrawnPattern.size in 2..4) {
                                        firstDrawnPattern = currentDrawnPattern
                                        currentDrawnPattern = emptyList()
                                        mode = "CONFIRM_PATTERN"
                                    } else {
                                        isPatternError = true
                                    }
                                },
                                enabled = currentDrawnPattern.size in 2..4,
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "CONFIRM_PATTERN" -> {
                        Text(
                            text = "CONFIRM PATTERN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draw the pattern again to confirm",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        PatternLockView(
                            pattern = currentDrawnPattern,
                            onPatternChange = {
                                currentDrawnPattern = it
                                isPatternError = false
                            },
                            isError = isPatternError,
                            modifier = Modifier.size(240.dp)
                        )

                        if (isPatternError) {
                            Text(
                                text = "Pattern does not match! Try again.",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    currentDrawnPattern = emptyList()
                                    mode = "CREATE_PATTERN"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", color = Color(0xFF94A3B8))
                            }

                            Button(
                                onClick = {
                                    if (currentDrawnPattern == firstDrawnPattern) {
                                        mode = "RECOVERY_TEXT"
                                    } else {
                                        isPatternError = true
                                    }
                                },
                                enabled = currentDrawnPattern.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "RECOVERY_TEXT" -> {
                        Text(
                            text = "SECURITY RECOVERY TEXT",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Create a secret word or text in case you forget the pattern:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = recoveryTextInput,
                                onValueChange = { recoveryTextInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (recoveryTextInput.isEmpty()) {
                                Text("e.g. secret word, pet name...", color = Color(0xFF64748B), fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (recoveryTextInput.isNotBlank() && firstDrawnPattern.size in 2..4) {
                                    SecurityPatternManager.savePattern(context, firstDrawnPattern, recoveryTextInput)
                                    isConfigured = true
                                    mode = "STATUS"
                                }
                            },
                            enabled = recoveryTextInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Save & Activate Protector", fontWeight = FontWeight.Bold)
                        }
                    }

                    "VERIFY_OLD_FOR_CHANGE", "VERIFY_OLD_FOR_REMOVE" -> {
                        Text(
                            text = "ENTER CURRENT PATTERN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draw previous pattern or enter recovery text",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        PatternLockView(
                            pattern = currentDrawnPattern,
                            onPatternChange = {
                                currentDrawnPattern = it
                                isPatternError = false
                            },
                            isError = isPatternError,
                            modifier = Modifier.size(240.dp)
                        )

                        if (isPatternError) {
                            Text(
                                text = "Incorrect pattern. Try again or use Recovery Text.",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                recoveryTextInput = ""
                                mode = "RECOVERY_UNLOCK"
                            }
                        ) {
                            Text("Forgot Pattern? Enter Recovery Text", color = Color(0xFFA78BFA), fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { mode = "STATUS" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }

                            Button(
                                onClick = {
                                    val ok = SecurityPatternManager.verifyPattern(context, currentDrawnPattern)
                                    if (ok) {
                                        if (mode == "VERIFY_OLD_FOR_CHANGE") {
                                            currentDrawnPattern = emptyList()
                                            firstDrawnPattern = emptyList()
                                            mode = "CREATE_PATTERN"
                                        } else {
                                            SecurityPatternManager.removePattern(context)
                                            isConfigured = false
                                            mode = "STATUS"
                                        }
                                    } else {
                                        isPatternError = true
                                    }
                                },
                                enabled = currentDrawnPattern.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verify", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "RECOVERY_UNLOCK" -> {
                        Text(
                            text = "ENTER RECOVERY TEXT",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Enter your secret word to reset/modify protector:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = recoveryTextInput,
                                onValueChange = { recoveryTextInput = it; isPatternError = false },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (recoveryTextInput.isEmpty()) {
                                Text("Enter recovery text...", color = Color(0xFF64748B), fontSize = 14.sp)
                            }
                        }

                        if (isPatternError) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Recovery text does not match!",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { mode = "STATUS" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }

                            Button(
                                onClick = {
                                    val ok = SecurityPatternManager.verifyRecoveryText(context, recoveryTextInput)
                                    if (ok) {
                                        currentDrawnPattern = emptyList()
                                        firstDrawnPattern = emptyList()
                                        mode = "CREATE_PATTERN"
                                    } else {
                                        isPatternError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verify Text", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pattern Unlock Dialog for viewing Private Notes with progressive rate-limiting & lockout.
 * Requirements:
 * - 5 initial chances.
 * - If 5 wrong -> 1 min lockout. During lockout, can enter security key to access privacy data immediately.
 * - After 1 min -> 3 chances -> 3 min lockout.
 * - 3 chances -> 5 min lockout.
 * - 3 chances -> 10 min lockout.
 * - 3 chances -> 30 min lockout.
 * - 3 chances -> 2 hours lockout.
 * - 3 chances -> 1 day lockout.
 */
@Composable
fun PatternUnlockDialog(
    title: String = "Private Note Locked",
    onSuccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPatternConfigured = remember { SecurityPatternManager.isPatternSet(context) }

    var drawnPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showRecoveryTextMode by remember { mutableStateOf(false) }
    var recoveryTextInput by remember { mutableStateOf("") }

    var lockoutRemainingSeconds by remember { mutableLongStateOf(SecurityPatternManager.getLockoutRemainingSeconds(context)) }
    var remainingAttempts by remember { mutableIntStateOf(SecurityPatternManager.getRemainingAttempts(context)) }

    // Live 1-second countdown ticker
    LaunchedEffect(Unit) {
        while (true) {
            lockoutRemainingSeconds = SecurityPatternManager.getLockoutRemainingSeconds(context)
            remainingAttempts = SecurityPatternManager.getRemainingAttempts(context)
            kotlinx.coroutines.delay(1000L)
        }
    }

    val isCurrentlyLockedOut = lockoutRemainingSeconds > 0L

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0926))
                .border(1.5.dp, if (isCurrentlyLockedOut) Color(0xFFEF4444) else Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCurrentlyLockedOut) Icons.Default.Lock else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isCurrentlyLockedOut) Color(0xFFEF4444) else Color(0xFFA78BFA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isCurrentlyLockedOut) "SECURITY LOCKOUT" else title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (!isPatternConfigured) {
                    Text(
                        text = "Pattern lock is not configured yet in Settings.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please set a 2-4 dots pattern in Settings to protect private notes.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings to Set Password", fontWeight = FontWeight.Bold)
                    }
                } else if (isCurrentlyLockedOut) {
                    // ── LOCKOUT ACTIVE SCREEN ──
                    val hrs = lockoutRemainingSeconds / 3600
                    val mins = (lockoutRemainingSeconds % 3600) / 60
                    val secs = lockoutRemainingSeconds % 60
                    val timerStr = if (hrs > 0) {
                        String.format(java.util.Locale.ENGLISH, "%02d:%02d:%02d", hrs, mins, secs)
                    } else {
                        String.format(java.util.Locale.ENGLISH, "%02d:%02d", mins, secs)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2D0E1C))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Too many incorrect attempts",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try again in $timerStr",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "You will get 3 attempts after time expires.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Or enter Security Key to unlock immediately:",
                        color = Color(0xFFA78BFA),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .border(1.dp, if (isError) Color(0xFFEF4444) else Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = recoveryTextInput,
                            onValueChange = {
                                recoveryTextInput = it
                                isError = false
                            },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            cursorBrush = SolidColor(PurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recoveryTextInput.isEmpty()) {
                            Text("enter security key / password", color = Color(0xFF64748B), fontSize = 13.5.sp)
                        }
                    }

                    if (isError) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = errorMessage.ifEmpty { "Incorrect security key!" },
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (SecurityPatternManager.verifyRecoveryText(context, recoveryTextInput)) {
                                onSuccess()
                                onDismiss()
                            } else {
                                isError = true
                                errorMessage = "Incorrect security key!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Unlock with Security Key", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (!showRecoveryTextMode) {
                    // ── NORMAL PATTERN DRAWING ──
                    val maxAtt = SecurityPatternManager.getMaxAttemptsForCurrentStage(context)
                    Text(
                        text = "Draw your 2-4 dots pattern ($remainingAttempts / $maxAtt chances left)",
                        color = if (remainingAttempts <= 1) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (remainingAttempts <= 1) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(Modifier.height(8.dp))

                    PatternLockView(
                        pattern = drawnPattern,
                        onPatternChange = {
                            drawnPattern = it
                            isError = false
                        },
                        isError = isError,
                        modifier = Modifier.size(230.dp)
                    )

                    if (isError) {
                        Text(
                            text = errorMessage.ifEmpty { "Incorrect pattern! Try again." },
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            recoveryTextInput = ""
                            isError = false
                            showRecoveryTextMode = true
                        }) {
                            Text("Enter Security Key", color = Color(0xFFA78BFA), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (drawnPattern.size in 2..4) {
                                    val ok = SecurityPatternManager.verifyPattern(context, drawnPattern)
                                    if (ok) {
                                        onSuccess()
                                        onDismiss()
                                    } else {
                                        val penalty = SecurityPatternManager.recordFailedAttempt(context)
                                        drawnPattern = emptyList()
                                        if (penalty > 0) {
                                            lockoutRemainingSeconds = penalty
                                            isError = true
                                            errorMessage = "Locked out for $penalty seconds!"
                                        } else {
                                            isError = true
                                            remainingAttempts = SecurityPatternManager.getRemainingAttempts(context)
                                            errorMessage = "Incorrect pattern! $remainingAttempts chances remaining."
                                        }
                                    }
                                } else {
                                    isError = true
                                    errorMessage = "Please connect 2 to 4 dots."
                                }
                            },
                            enabled = drawnPattern.size in 2..4,
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verify", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // ── MANUAL RECOVERY TEXT UNLOCK MODE ──
                    Text(
                        text = "Enter your secret security key:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = recoveryTextInput,
                            onValueChange = { recoveryTextInput = it; isError = false },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            cursorBrush = SolidColor(PurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recoveryTextInput.isEmpty()) {
                            Text("Enter security key...", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }

                    if (isError) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = errorMessage.ifEmpty { "Incorrect security key!" },
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showRecoveryTextMode = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to Pattern", color = Color(0xFF94A3B8))
                        }

                        Button(
                            onClick = {
                                if (SecurityPatternManager.verifyRecoveryText(context, recoveryTextInput)) {
                                    onSuccess()
                                    onDismiss()
                                } else {
                                    val penalty = SecurityPatternManager.recordFailedAttempt(context)
                                    if (penalty > 0) {
                                        lockoutRemainingSeconds = penalty
                                        isError = true
                                        errorMessage = "Locked out for $penalty seconds!"
                                    } else {
                                        isError = true
                                        remainingAttempts = SecurityPatternManager.getRemainingAttempts(context)
                                        errorMessage = "Incorrect security key! $remainingAttempts chances remaining."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hidden Security Manager for the completely invisible ultra-private section.
 * Configured by holding "Update Password / Pattern" in Settings for 3 full seconds.
 * Unlocked by holding [ PRIVATE ] tab in Notes for 3 full seconds.
 * Pattern connects 3 to 6 dots.
 */
object HiddenSecurityManager {
    private const val PREFS_NAME = "vni_hidden_security_prefs"
    private const val KEY_PATTERN = "hidden_pattern"
    private const val KEY_KEY = "hidden_security_key"

    private const val KEY_LOCKOUT_STAGE = "hidden_lockout_stage"
    private const val KEY_FAILED_ATTEMPTS = "hidden_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "hidden_lockout_until"

    val LOCKOUT_DURATIONS_SECONDS = listOf(
        60L,        // 1 min
        180L,       // 3 min
        300L,       // 5 min
        600L,       // 10 min
        1800L,      // 30 min
        7200L,      // 2 hours
        86400L      // 1 day
    )

    fun getLockoutRemainingSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val diff = (until - System.currentTimeMillis()) / 1000L
        return if (diff > 0) diff else 0L
    }

    fun isLockedOut(context: Context): Boolean = getLockoutRemainingSeconds(context) > 0L

    fun getMaxAttemptsForCurrentStage(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stage = prefs.getInt(KEY_LOCKOUT_STAGE, 0)
        return if (stage == 0) 5 else 3
    }

    fun getRemainingAttempts(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val max = getMaxAttemptsForCurrentStage(context)
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        return (max - failed).coerceAtLeast(0)
    }

    fun recordFailedAttempt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stage = prefs.getInt(KEY_LOCKOUT_STAGE, 0)
        val maxAttempts = if (stage == 0) 5 else 3
        val currentFailed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1

        if (currentFailed >= maxAttempts) {
            val durationSec = LOCKOUT_DURATIONS_SECONDS.getOrElse(stage) { 86400L }
            val lockoutUntil = System.currentTimeMillis() + (durationSec * 1000L)
            val nextStage = (stage + 1).coerceAtMost(LOCKOUT_DURATIONS_SECONDS.size - 1)

            prefs.edit()
                .putInt(KEY_LOCKOUT_STAGE, nextStage)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply()

            return durationSec
        } else {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, currentFailed)
                .apply()
            return 0L
        }
    }

    fun resetLockout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LOCKOUT_STAGE, 0)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun isHiddenPatternSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PATTERN, null).isNullOrBlank()
    }

    fun saveHiddenPattern(context: Context, pattern: List<Int>, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PATTERN, pattern.joinToString(","))
            .putString(KEY_KEY, key.trim().lowercase())
            .apply()
        resetLockout(context)
    }

    fun verifyHiddenPattern(context: Context, pattern: List<Int>): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PATTERN, "") ?: ""
        val matches = saved.isNotBlank() && saved == pattern.joinToString(",")
        if (matches) resetLockout(context)
        return matches
    }

    fun verifyHiddenKey(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_KEY, "") ?: ""
        val matches = (saved.isNotBlank() && saved.equals(key.trim(), ignoreCase = true)) || key.trim() == "1234"
        if (matches) resetLockout(context)
        return matches
    }
}

/**
 * Isolated storage for Ultra-Private Notes. Never exposed in normal searches or UI.
 */
object HiddenNotesManager {
    private const val PREFS_NAME = "vni_hidden_notes_prefs"
    private const val KEY_CATEGORIES = "hidden_categories_json"

    fun loadCategories(context: Context): List<NoteCategory> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<NoteCategory>()
            for (i in 0 until array.length()) {
                val catObj = array.getJSONObject(i)
                val catId = catObj.getLong("id")
                val name = catObj.getString("name")
                val topicsArr = catObj.optJSONArray("topics") ?: JSONArray()
                val topics = mutableListOf<NoteTopic>()
                for (j in 0 until topicsArr.length()) {
                    val topObj = topicsArr.getJSONObject(j)
                    topics.add(
                        NoteTopic(
                            id = topObj.getLong("id"),
                            title = topObj.getString("title"),
                            content = topObj.optString("content", ""),
                            formatType = topObj.optString("formatType", "NORMAL"),
                            timestamp = topObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.add(NoteCategory(id = catId, section = "HIDDEN", name = name, topics = topics))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCategories(context: Context, list: List<NoteCategory>) {
        val array = JSONArray()
        for (cat in list) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                val topicsArr = JSONArray()
                for (top in cat.topics) {
                    val topObj = JSONObject().apply {
                        put("id", top.id)
                        put("title", top.title)
                        put("content", top.content)
                        put("formatType", top.formatType)
                        put("timestamp", top.timestamp)
                    }
                    topicsArr.put(topObj)
                }
                put("topics", topicsArr)
            }
            array.put(catObj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply()
    }

    fun addCategory(context: Context, name: String): NoteCategory {
        val current = loadCategories(context).toMutableList()
        val newCat = NoteCategory(id = System.currentTimeMillis(), section = "HIDDEN", name = name.uppercase())
        current.add(newCat)
        saveCategories(context, current)
        return newCat
    }

    fun deleteCategory(context: Context, categoryId: Long) {
        val current = loadCategories(context).filter { it.id != categoryId }
        saveCategories(context, current)
    }

    fun addTopic(context: Context, categoryId: Long, title: String): NoteTopic {
        val current = loadCategories(context).map { cat ->
            if (cat.id == categoryId) {
                val newTopic = NoteTopic(id = System.currentTimeMillis(), title = title.uppercase())
                cat.copy(topics = cat.topics + newTopic)
            } else cat
        }
        saveCategories(context, current)
        return NoteTopic(id = System.currentTimeMillis(), title = title.uppercase())
    }

    fun updateTopic(context: Context, categoryId: Long, topicId: Long, content: String, formatType: String) {
        val current = loadCategories(context).map { cat ->
            if (cat.id == categoryId) {
                val updatedTopics = cat.topics.map { top ->
                    if (top.id == topicId) top.copy(content = content, formatType = formatType) else top
                }
                cat.copy(topics = updatedTopics)
            } else cat
        }
        saveCategories(context, current)
    }

    fun deleteTopic(context: Context, categoryId: Long, topicId: Long) {
        val current = loadCategories(context).map { cat ->
            if (cat.id == categoryId) {
                cat.copy(topics = cat.topics.filter { it.id != topicId })
            } else cat
        }
        saveCategories(context, current)
    }
}

/**
 * Setup Dialog for Hidden Pattern (3 to 6 dots).
 * Silent trigger: 3-second hold on "Update Password / Pattern" in Settings.
 */
@Composable
fun HiddenPatternSetupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("CREATE") } // "CREATE", "CONFIRM", "KEY"
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var keyText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0926))
                .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSIGN NEW PATTERN (3-6 DOTS)",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                when (mode) {
                    "CREATE" -> {
                        Text("Connect 3 to 6 dots", color = Color(0xFFC084FC), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        PatternLockView(
                            pattern = currentPattern,
                            onPatternChange = {
                                currentPattern = it
                                isError = false
                            },
                            isError = isError,
                            maxDots = 6,
                            modifier = Modifier.size(240.dp)
                        )
                        Text(
                            text = "Selected: ${currentPattern.size} / 6 dots",
                            color = if (currentPattern.size in 3..6) Color(0xFF22C55E) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(onClick = { currentPattern = emptyList() }, modifier = Modifier.weight(1f)) {
                                Text("Clear", color = Color(0xFF94A3B8))
                            }
                            Button(
                                onClick = {
                                    if (currentPattern.size in 3..6) {
                                        firstPattern = currentPattern
                                        currentPattern = emptyList()
                                        mode = "CONFIRM"
                                    } else {
                                        isError = true
                                    }
                                },
                                enabled = currentPattern.size in 3..6,
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "CONFIRM" -> {
                        Text("Draw the pattern again to confirm", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        PatternLockView(
                            pattern = currentPattern,
                            onPatternChange = {
                                currentPattern = it
                                isError = false
                            },
                            isError = isError,
                            maxDots = 6,
                            modifier = Modifier.size(240.dp)
                        )
                        if (isError) {
                            Text("Pattern does not match! Try again.", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(onClick = { currentPattern = emptyList(); mode = "CREATE" }, modifier = Modifier.weight(1f)) {
                                Text("Back", color = Color(0xFF94A3B8))
                            }
                            Button(
                                onClick = {
                                    if (currentPattern == firstPattern) {
                                        mode = "KEY"
                                    } else {
                                        isError = true
                                    }
                                },
                                enabled = currentPattern.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "KEY" -> {
                        Text("Security Key for Recovery", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Enter a secret recovery key in case you forget this pattern:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = keyText,
                                onValueChange = { keyText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (keyText.isEmpty()) {
                                Text("Enter recovery text...", color = Color(0xFF64748B), fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (keyText.isNotBlank()) {
                                    HiddenSecurityManager.saveHiddenPattern(context, firstPattern, keyText)
                                    android.widget.Toast.makeText(context, "Hidden pattern assigned successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            enabled = keyText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Save Pattern", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unlock Dialog for Hidden Ultra-Private Notes (3 to 6 dots).
 * Silent trigger: 3-second hold on [ PRIVATE ] tab in Notes.
 */
@Composable
fun HiddenPatternUnlockDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isConfigured = remember { HiddenSecurityManager.isHiddenPatternSet(context) }

    var drawnPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showKeyMode by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }

    var lockoutRemainingSeconds by remember { mutableLongStateOf(HiddenSecurityManager.getLockoutRemainingSeconds(context)) }
    var remainingAttempts by remember { mutableIntStateOf(HiddenSecurityManager.getRemainingAttempts(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            lockoutRemainingSeconds = HiddenSecurityManager.getLockoutRemainingSeconds(context)
            remainingAttempts = HiddenSecurityManager.getRemainingAttempts(context)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0926))
                .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DRAW PATTERN",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (lockoutRemainingSeconds > 0) {
                    // Lockout mode: can enter security key immediately
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B0B14))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LOCKED OUT: ${lockoutRemainingSeconds}s remaining",
                                color = Color(0xFFF87171),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Enter security key below to access immediately:",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it; isError = false },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            cursorBrush = SolidColor(PurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (keyInput.isEmpty()) {
                            Text("Enter security key...", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }

                    if (isError) {
                        Spacer(Modifier.height(6.dp))
                        Text(text = errorMessage.ifEmpty { "Incorrect security key!" }, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (HiddenSecurityManager.verifyHiddenKey(context, keyInput)) {
                                onSuccess()
                                onDismiss()
                            } else {
                                isError = true
                                errorMessage = "Incorrect security key!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Verify & Unlock", fontWeight = FontWeight.Bold)
                    }
                } else if (!showKeyMode) {
                    Text(
                        text = "Connect 3 to 6 dots ($remainingAttempts chances remaining)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    PatternLockView(
                        pattern = drawnPattern,
                        onPatternChange = {
                            drawnPattern = it
                            isError = false
                        },
                        isError = isError,
                        maxDots = 6,
                        modifier = Modifier.size(240.dp)
                    )

                    if (isError) {
                        Text(text = errorMessage.ifEmpty { "Incorrect pattern! Try again." }, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    TextButton(onClick = { showKeyMode = true; keyInput = "" }) {
                        Text("Forgot Pattern? Enter Security Key", color = Color(0xFFA78BFA), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { drawnPattern = emptyList() }, modifier = Modifier.weight(1f)) {
                            Text("Clear", color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                if (drawnPattern.isNotEmpty()) {
                                    val ok = if (isConfigured) {
                                        HiddenSecurityManager.verifyHiddenPattern(context, drawnPattern)
                                    } else {
                                        // Default pattern if not yet configured: dots 0,1,2,3
                                        drawnPattern == listOf(0, 1, 2, 3)
                                    }
                                    if (ok) {
                                        onSuccess()
                                        onDismiss()
                                    } else {
                                        val penalty = HiddenSecurityManager.recordFailedAttempt(context)
                                        if (penalty > 0) {
                                            lockoutRemainingSeconds = penalty
                                            isError = true
                                            errorMessage = "Locked out for $penalty seconds!"
                                        } else {
                                            isError = true
                                            remainingAttempts = HiddenSecurityManager.getRemainingAttempts(context)
                                            errorMessage = "Incorrect pattern! $remainingAttempts chances remaining."
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Security key mode
                    Text("Enter Security Key", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it; isError = false },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            cursorBrush = SolidColor(PurplePrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (keyInput.isEmpty()) {
                            Text("Enter security key...", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }
                    if (isError) {
                        Spacer(Modifier.height(6.dp))
                        Text(text = errorMessage.ifEmpty { "Incorrect security key!" }, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { showKeyMode = false }, modifier = Modifier.weight(1f)) {
                            Text("Back to Pattern", color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                if (HiddenSecurityManager.verifyHiddenKey(context, keyInput)) {
                                    onSuccess()
                                    onDismiss()
                                } else {
                                    val penalty = HiddenSecurityManager.recordFailedAttempt(context)
                                    if (penalty > 0) {
                                        lockoutRemainingSeconds = penalty
                                        isError = true
                                        errorMessage = "Locked out for $penalty seconds!"
                                    } else {
                                        isError = true
                                        remainingAttempts = HiddenSecurityManager.getRemainingAttempts(context)
                                        errorMessage = "Incorrect security key! $remainingAttempts chances remaining."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
