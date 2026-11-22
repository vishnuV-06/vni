package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.PlanContainer
import com.vni.app.ui.theme.PurplePrimary

/**
 * Full-screen Plan Page:
 * - Top Row: [+ ADD] button on left, [✕ Close] button on right.
 * - Headings (containers) have an "✕" button to delete them.
 * - Sub topics (elements) have an "✕" button to delete them.
 * - Double-clicking an element marks it disabled (grey color) and moves it to last position.
 * - Compact design with reduced padding and slim heights matching Notes/Do/Motive.
 */
@Composable
fun PlanScreen(
    planContainers: List<PlanContainer>,
    onToggleExpand: (Long) -> Unit,
    onAddContainer: (String) -> Unit,
    onAddElement: (containerId: Long, title: String, subtitle: String) -> Unit,
    onToggleElementDisabled: (containerId: Long, elementId: Long) -> Unit,
    onDeleteContainer: (Long) -> Unit = {},
    onDeleteElement: (containerId: Long, elementId: Long) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddContainerDialog by remember { mutableStateOf(false) }
    var newContainerTitle by remember { mutableStateOf("") }

    var showAddElementDialogForContainerId by remember { mutableStateOf<Long?>(null) }
    var newElementTitle by remember { mutableStateOf("") }
    var newElementSubtitle by remember { mutableStateOf("") }

    val indicatorColors = listOf(
        Color(0xFF8B5CF6), // Purple
        Color(0xFF3B82F6), // Blue
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Green
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4)  // Cyan
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B061A))
            .zIndex(60f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── Top Middle: "Plan" Section Name & Below it: Corner Buttons ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Plan",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [+ ADD] button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0F3C))
                            .border(1.2.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable {
                                newContainerTitle = ""
                                showAddContainerDialog = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "ADD",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // [✕ Close] button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0F3C))
                            .border(1.2.dp, PurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── List of Containers (collapsed buttons or expanded cards) ──
            if (planContainers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No plan categories yet.\nTap [+ ADD] to create a new subject plan!",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(planContainers, key = { it.id }) { container ->
                        if (!container.isExpanded) {
                            // Collapsed Pill Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF6D28D9), Color(0xFF7C3AED), Color(0xFF6D28D9))
                                        )
                                    )
                                    .clickable { onToggleExpand(container.id) }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = container.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // "✕" button to delete collapsed heading
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.3f))
                                                .clickable { onDeleteContainer(container.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Plan Heading",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Expand",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Expanded Container Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF100926))
                                    .border(1.2.dp, Color(0xFF4C1D95), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Container Header: Title + Subjects badge + "✕" button + Collapse Arrow
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = container.title,
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onToggleExpand(container.id) }
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val totalCount = container.elements.size
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF1C123E))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "$totalCount Subjects",
                                                    color = Color(0xFFC084FC),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            // "✕" button to delete expanded heading
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF25164B))
                                                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f), CircleShape)
                                                .clickable { onDeleteContainer(container.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Plan Heading",
                                                    tint = Color(0xFFF43F5E),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Collapse",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable { onToggleExpand(container.id) }
                                            )
                                        }
                                    }

                                    // Elements inside container (compact slim rows)
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        container.elements.forEach { element ->
                                            val indicatorColor = if (element.isDisabled) {
                                                Color(0xFF475569) // Greyed out when disabled
                                            } else {
                                                indicatorColors[element.colorIndex % indicatorColors.size]
                                            }

                                            val titleColor = if (element.isDisabled) Color(0xFF64748B) else Color.White
                                            val subtitleColor = if (element.isDisabled) Color(0xFF475569) else Color(0xFF94A3B8)
                                            val cardBg = if (element.isDisabled) Color(0xFF0F0A20).copy(alpha = 0.7f) else Color(0xFF150D33)
                                            val borderColor = if (element.isDisabled) Color(0xFF1E1738) else Color(0xFF26194E)

                                            val hasSubtitle = element.subtitle.isNotBlank()

                                            // Double-click toggles disabled state and moves to last position
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(cardBg)
                                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                    .pointerInput(container.id, element.id) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                onToggleElementDisabled(container.id, element.id)
                                                            }
                                                        )
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = if (hasSubtitle) 8.dp else 5.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Left indicator pill
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.5.dp)
                                                            .height(if (hasSubtitle) 22.dp else 16.dp)
                                                            .clip(CircleShape)
                                                            .background(indicatorColor)
                                                    )
                                                    Spacer(Modifier.width(10.dp))

                                                    // Title and Subtitle
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = element.title,
                                                            color = titleColor,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (hasSubtitle) {
                                                            Text(
                                                                text = element.subtitle,
                                                                color = subtitleColor,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }

                                                    if (element.isDisabled) {
                                                        Text(
                                                            text = "Done",
                                                            color = Color(0xFF64748B),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(end = 6.dp)
                                                        )
                                                    }

                                                    // "✕" button for sub topics / elements to delete them
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF25164B))
                                                            .border(1.dp, Color(0xFF4C1D95).copy(alpha = 0.5f), CircleShape)
                                                            .clickable {
                                                                onDeleteElement(container.id, element.id)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete Subject",
                                                            tint = Color(0xFFF43F5E),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // "+ Add New Subject" button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF130B2A))
                                            .border(
                                                width = 1.dp,
                                                color = PurplePrimary.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                newElementTitle = ""
                                                newElementSubtitle = ""
                                                showAddElementDialogForContainerId = container.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Add New Subject",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog: Add Plan Container / Heading ──
        if (showAddContainerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showAddContainerDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160D35))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "New Plan Box",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = newContainerTitle,
                                onValueChange = { newContainerTitle = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (newContainerTitle.isEmpty()) {
                                        Text(text = "e.g., Computer Networks, Syllabus...", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddContainerDialog = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        if (newContainerTitle.isNotBlank()) {
                                            onAddContainer(newContainerTitle)
                                            showAddContainerDialog = false
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Create",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog: Add Element / Subtopic inside Container ──
        val targetContainerId = showAddElementDialogForContainerId
        if (targetContainerId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showAddElementDialogForContainerId = null },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160D35))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add New Subject / Topic",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

                        // Title Field
                        Text(text = "SUBJECT TITLE", color = Color(0xFFC084FC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = newElementTitle,
                                onValueChange = { newElementTitle = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (newElementTitle.isEmpty()) {
                                        Text(text = "e.g., Mathematics, Operating Systems...", color = Color.Gray, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Subtitle / Sub-topics Field
                        Text(text = "SUB-TOPICS / DETAILS", color = Color(0xFFC084FC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = newElementSubtitle,
                                onValueChange = { newElementSubtitle = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (newElementSubtitle.isEmpty()) {
                                        Text(text = "Optional: Sub-topics, notes, chapters...", color = Color.Gray, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddElementDialogForContainerId = null }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        if (newElementTitle.isNotBlank()) {
                                            onAddElement(targetContainerId, newElementTitle, newElementSubtitle)
                                            showAddElementDialogForContainerId = null
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
