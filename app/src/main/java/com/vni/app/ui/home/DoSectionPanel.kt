package com.vni.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.DoItem
import com.vni.app.ui.theme.PurplePrimary
import com.vni.app.ui.theme.PurpleLight
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * "Do" Section Bottom Panel matching Image 2 & Image 3:
 * - Slide-from-bottom panel with glowing purple border.
 * - Header: [+ ADD] pill button on left, [✕ Close] pill button on right.
 * - Item Cards: Left colored thick vertical pill indicator, neon colored border (purple, cyan, orange),
 *   bold title, "Dead line : DD MMM", and [✕] delete button.
 * - Clicking [✕] moves item into History with current date!
 * - [+ ADD] opens dialog matching Image 3 (Name input, Deadline date picker via CalendarPanel, Done button).
 */
@Composable
fun DoSectionPanel(
    doItems: List<DoItem>,
    onAddDoItem: (name: String, deadlineDateText: String, daysRequired: Int, isSpecificDate: Boolean, isDailyUntilComplete: Boolean, hasTimeSchedule: Boolean, specialized: String) -> Unit,
    onRemoveDoItem: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var specializedInput by remember { mutableStateOf("") }
    var hasTimeSchedule by remember { mutableStateOf(false) } // Default OFF as requested
    var deadlineText by remember { mutableStateOf("27 SEP") }
    var doMode by remember { mutableStateOf("Deadline") } // "Deadline", "Specific Date", "Daily"
    var daysRequired by remember { mutableIntStateOf(3) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(doItems) {
        val list = mutableListOf("All")
        doItems.forEach { item ->
            val spec = item.specialized.trim()
            if (spec.isNotEmpty() && !list.any { it.equals(spec, ignoreCase = true) }) {
                list.add(spec)
            }
        }
        list
    }
    val activeCategory = if (categories.any { it.equals(selectedCategory, ignoreCase = true) }) selectedCategory else "All"
    val displayedItems = if (activeCategory == "All") {
        doItems
    } else {
        doItems.filter { it.specialized.trim().equals(activeCategory, ignoreCase = true) }
    }

    val density = LocalDensity.current
    val sheetHeightPx = with(density) { 700.dp.toPx() }
    val offsetY = remember { Animatable(sheetHeightPx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun dismiss() {
        scope.launch {
            offsetY.animateTo(sheetHeightPx, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
    }

    val monthNames = listOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(55f)
    ) {
        // Scrim background (tapping outside does not accidentally dismiss, only clicking Close)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Full Screen Page matching Requirement 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070417))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Purple Top Handle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(54.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )

                // Top Middle: "Do" Section Name
                Text(
                    text = "Do",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(8.dp))

                // Header Row: [+ ADD] on left | [✕ Close] on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + ADD Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.5.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable {
                                taskName = ""
                                specializedInput = ""
                                hasTimeSchedule = false
                                deadlineText = "27 SEP"
                                showAddDialog = true
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ADD",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // ✕ Close Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.5.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable { dismiss() }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Category Headings Filter Bar (matching History all, do, task, note)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category.equals(activeCategory, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePrimary else Color(0xFF140D30))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PurpleLight else Color(0xFF2E1C6A),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // List of Do Items matching Image 2 (Compact Slim Style matching Notes/Plan/Motive)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        val itemColor = when (item.colorIndex % 3) {
                            0 -> Color(0xFFA855F7) // Purple
                            1 -> Color(0xFF06B6D4) // Cyan
                            else -> Color(0xFFF97316) // Orange
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D0824))
                                .border(1.dp, itemColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left colored vertical thick pill indicator
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(itemColor)
                                )

                                Spacer(Modifier.width(10.dp))

                                // Task title, specialized badge, and deadline info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    )
                                    if (item.specialized.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = item.specialized,
                                            color = Color(0xFFA78BFA),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (item.hasTimeSchedule) {
                                        val labelPrefix = when {
                                            item.isDailyUntilComplete -> "Daily : "
                                            item.isSpecificDate -> "Specific Date : "
                                            else -> "Dead line : "
                                        }
                                        val daysSuffix = if (!item.isSpecificDate && !item.isDailyUntilComplete && item.daysRequired > 1) " • ${item.daysRequired} days req" else ""
                                        Text(
                                            text = "$labelPrefix${item.deadlineDateText}$daysSuffix",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

                                // Date badge displayed on the right-side corner for tasks with specific date or deadline
                                if (item.hasTimeSchedule && (item.isSpecificDate || !item.isDailyUntilComplete)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1F1242))
                                            .border(1.dp, itemColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.deadlineDateText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                // Delete ✕ Button (moves to History with date)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF170E33))
                                        .border(1.dp, Color(0xFF33206A), CircleShape)
                                        .clickable { onRemoveDoItem(item.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove to History",
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

        // ── Add Do Item Dialog matching Image 3 ──────────────────────────────
        if (showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}
                    .zIndex(70f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F0829))
                        .border(2.dp, PurplePrimary, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        // Title: Name
                        Text(
                            text = "Name",
                            color = Color(0xFFA855F7),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Text Field Input Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C0F45))
                                .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (taskName.isEmpty()) {
                                Text(
                                    text = "Enter task...",
                                    color = Color(0xFF6B7280),
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = taskName,
                                onValueChange = { taskName = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(PurplePrimary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Specialized (Belongs to: studying, workout, work, school, etc.)
                        Text(
                            text = "Specialized (e.g. Studying, Workout, School)",
                            color = Color(0xFFA855F7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C0F45))
                                .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (specializedInput.isEmpty()) {
                                Text(
                                    text = "Category / Belongs to...",
                                    color = Color(0xFF6B7280),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = specializedInput,
                                onValueChange = { specializedInput = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(PurplePrimary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Time / Schedule ON/OFF toggle (default OFF)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF140A30))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Time / Schedule",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasTimeSchedule) "Time schedule enabled" else "No time constraint (OFF)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hasTimeSchedule) Color(0xFF16A34A) else Color(0xFF2A1B54))
                                    .border(
                                        width = 1.dp,
                                        color = if (hasTimeSchedule) Color(0xFF4ADE80) else Color(0xFF4C2889),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { hasTimeSchedule = !hasTimeSchedule }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasTimeSchedule) "ON" else "OFF",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // When Time Schedule is ON, user can select Deadline / Specific Date / Daily
                        if (hasTimeSchedule) {
                            Spacer(Modifier.height(12.dp))

                            // Mode Selector: [ Deadline ] vs [ Specific Date ] vs [ Daily ]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF140A30))
                                    .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "Deadline" to "Deadline",
                                    "Specific Date" to "Specific Date",
                                    "Daily" to "Daily"
                                ).forEach { (modeKey, modeTitle) ->
                                    val isSelected = doMode == modeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) PurplePrimary else Color.Transparent)
                                            .clickable { doMode = modeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = modeTitle,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            if (doMode == "Deadline") {
                                // Days Required Counter
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Days required",
                                            color = Color(0xFFA855F7),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Remind prior to deadline",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1C0F45))
                                                .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(8.dp))
                                                .clickable { if (daysRequired > 1) daysRequired-- },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "$daysRequired",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1C0F45))
                                                .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(8.dp))
                                                .clickable { if (daysRequired < 30) daysRequired++ },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            if (doMode == "Daily") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF19103C))
                                        .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                    ) {
                                    Text(
                                        text = "⚡ Daily Task: Displays every day until you mark it completed!",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Bottom Row: Date Picker Button on left | [Done] on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (hasTimeSchedule && doMode != "Daily") {
                                Column {
                                    Text(
                                        text = if (doMode == "Specific Date") "Target Date" else "Deadline Date",
                                        color = Color(0xFFA855F7),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF221152))
                                            .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                                            .clickable { showCalendarPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = deadlineText,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.width(8.dp))
                            }

                            // Done button (Rectangular border with curved edges)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1D4ED8))
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (taskName.isNotBlank()) {
                                            onAddDoItem(
                                                taskName.trim(),
                                                if (hasTimeSchedule) {
                                                    if (doMode == "Daily") "Daily until complete" else deadlineText
                                                } else "",
                                                if (hasTimeSchedule && doMode == "Deadline") daysRequired else 1,
                                                hasTimeSchedule && doMode == "Specific Date",
                                                hasTimeSchedule && doMode == "Daily",
                                                hasTimeSchedule,
                                                specializedInput.trim()
                                            )
                                            taskName = ""
                                            specializedInput = ""
                                            hasTimeSchedule = false
                                            showAddDialog = false
                                        }
                                    }
                                    .padding(horizontal = 28.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Done",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Interactive Calendar Date Picker for Deadline (Image 3) ─────────
        if (showCalendarPicker) {
            CalendarPanel(
                onDateSelected = { year, month, day ->
                    val monthStr = monthNames.getOrElse(month) { "SEP" }
                    deadlineText = "$day $monthStr"
                    showCalendarPicker = false
                },
                onDismiss = { showCalendarPicker = false }
            )
        }
    }
}
