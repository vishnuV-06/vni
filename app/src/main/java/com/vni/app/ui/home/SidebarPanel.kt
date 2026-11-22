package com.vni.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.PurplePrimary

@Composable
fun SidebarPanel(
    isOpen: Boolean,
    onOpen: () -> Unit = {},
    onDismiss: () -> Unit,
    onEditClick: () -> Unit = {},
    onPetClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    isTaskSection: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
    ) {
        // Only block touches when open
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

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = 20.dp)
                .pointerInput(isOpen) {
                    detectHorizontalDragGestures(
                        onDragEnd = { }
                    ) { change, dragAmount ->
                        change.consume()
                        if (isOpen && dragAmount > 10f) {
                            onDismiss()
                        } else if (!isOpen && dragAmount < -10f) {
                            onOpen()
                        }
                    }
                }
        ) {
            if (isOpen) {
                // Opened state: Edit/Settings & Pet Assistant options
                Row(
                    modifier = Modifier
                        .shadow(
                            elevation = 24.dp, 
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                            spotColor = Color(0xFFAA4BFA), 
                            ambientColor = Color(0xFFAA4BFA)
                        )
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF351280), Color(0xFF130B30))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                        )
                        .border(1.dp, Color(0xFFAA4BFA).copy(0.5f), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTaskSection) {
                        // Edit Tasks Button (only in Task section)
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFAA4BFA).copy(0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Outlined.Edit, "Edit", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        // Settings Button (in all other sections)
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF221555), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFAA4BFA).copy(0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    // Pet Assistant Symbol Button
                    IconButton(
                        onClick = onPetClick,
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF3A1F75), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFFB703).copy(0.7f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pets,
                            contentDescription = "Pet Assistant",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Closed state: The right corner handle bar
                // Clicking this navigates back in the whole app
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(88.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color(0xFF8A2BE2), Color(0xFFAA4BFA))
                            )
                        )
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
