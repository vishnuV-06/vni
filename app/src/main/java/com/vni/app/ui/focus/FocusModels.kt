package com.vni.app.ui.focus

import android.graphics.drawable.Drawable

/**
 * Data models for the Focus Mode feature.
 */
data class FocusSession(
    val id: Long = System.currentTimeMillis(),
    val name: String = "STUDY",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationSeconds: Long,
    val mode: String = "TIMER", // "TIMER", "STOPWATCH", "INTERVALS"
    val dateKey: String = ""    // "yyyy-MM-dd"
)

data class BlockedAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false
)

data class FocusDaySummary(
    val dateKey: String,
    val totalSeconds: Long,
    val sessions: List<FocusSession>
)
