package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Sidebar Dock (Requirement 1):
 * - Clicking the handle acts as (come back from page / go back).
 * - Dragging the handle opens the sidebar dock.
 * - Dock contains category logos:
 *   1. Plan (🎯 TrackChanges)
 *   2. Home (🏠 Home)
 *   3. Daily (⏰ Schedule)
 *   4. Pet (🐾 Pets)
 */
@Composable
fun HomeSidebarDock(
    isOpen: Boolean,
    activeSection: String, // "PLAN", "HOME", "DAILY", "PET"
    isBackAvailable: Boolean = false,
    onBack: () -> Unit = {},
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSelectSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(95f)
    ) {
        // Scrim / Outside dismiss when open
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        // Dock & Handle on the right side
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-20).dp)
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                // Expanded Dock (Plan, Home, Daily, Pet)
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF0F0724).copy(alpha = 0.94f))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 15f) {
                                    onDismiss()
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Plan (🎯)
                        SidebarIconButton(
                            icon = Icons.Outlined.TrackChanges,
                            contentDescription = "Plan",
                            isActive = (activeSection == "PLAN"),
                            onClick = { onSelectSection("PLAN") }
                        )

                        // 2. Home (🏠)
                        SidebarIconButton(
                            icon = Icons.Outlined.Home,
                            contentDescription = "Home",
                            isActive = (activeSection == "HOME"),
                            onClick = { onSelectSection("HOME") }
                        )

                        // 3. Focus (⏱️)
                        SidebarIconButton(
                            icon = Icons.Outlined.Timer,
                            contentDescription = "Focus",
                            isActive = (activeSection == "FOCUS"),
                            onClick = { onSelectSection("FOCUS") }
                        )
                    }
                }
            }

            // Closed tab handle anchored on the right edge:
            // - Clicking it acts as BACK ("come back from page")
            // - Dragging left opens the dock
            if (!isOpen) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 58.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
                            )
                        )
                        .border(1.2.dp, Color(0xFFE879F9).copy(alpha = 0.85f), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .shadow(elevation = 10.dp, spotColor = Color(0xFFA855F7))
                        .clickable {
                            // Clicking acts as back / come back from page
                            onBack()
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -10f) {
                                    onOpen()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBackAvailable) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Sidebar / Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1B0E3C), Color(0xFF14082E))
        )
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Color(0xFFE879F9) else Color(0xFF4C1D95).copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (isActive) {
                    Modifier.shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else Color(0xFFC084FC),
            modifier = Modifier.size(24.dp)
        )
    }
}
