package com.vni.app.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.*
import com.vni.app.ui.theme.*

/**
 * Day Schedule Planner Page matching Image 3 (media_1790761608642.jpg):
 * - Top row: Day plan templates (clg, holiday, home, +), [Edit] button, and [X] close button.
 * - 24-Hour Vertical Time Map: Center solid white line, 3-hour ticks (- 6:00, - 9:00, etc.),
 *   AM/PM indicators, and full 12:00 AM - 11:59 PM continuous smooth scrolling.
 * - Section division: Horizontal colored crossing lines, left-side capsule pills,
 *   [Wake up] / [Sleep] speech bubbles.
 * - Staggered task layout: Odd priorities on the left (e.g. 1 Piano, 3 App Dev),
 *   Even priorities on the right (e.g. 2 Study).
 * - Distinct colors for Tasks (Purple) and Uni-Tasks (Teal).
 * - Section creation controls (Start, End, Name, [Set]).
 * - Scrollable Time Picker Popup (Image 2 + scroll wheel).
 * - Edit Sections Panel (beside +) to view & delete sections.
 */
@Composable
fun DayTimelineScreen(
    plans: List<DaySchedulePlan>,
    selectedPlanName: String,
    tasks: List<Task>,
    uniTasks: List<UniTask>,
    onSelectPlan: (String) -> Unit,
    onAddPlan: (String) -> Unit,
    onAddSection: (name: String, startTime: String, endTime: String, isWakeUp: Boolean, isSleep: Boolean, startHour: Int, startMin: Int, endHour: Int, endMin: Int) -> Unit,
    onDeleteSection: (Long) -> Unit,
    onAllocateTask: (sectionId: Long, taskId: Long, isUniTask: Boolean, name: String, priority: Int) -> Unit,
    onRemoveAllocation: (sectionId: Long, taskId: Long, isUniTask: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlan = plans.find { it.name.equals(selectedPlanName, ignoreCase = true) }
        ?: plans.firstOrNull() ?: DaySchedulePlan(1, "clg")

    // Section input states
    var startInputText by remember { mutableStateOf("07:00 AM") }
    var endInputText by remember { mutableStateOf("09:20 AM") }
    var nameInputText by remember { mutableStateOf("") }

    var isStartWakeUp by remember { mutableStateOf(true) }
    var isEndSleep by remember { mutableStateOf(false) }
    var startHourVal by remember { mutableIntStateOf(7) }
    var startMinVal by remember { mutableIntStateOf(0) }
    var endHourVal by remember { mutableIntStateOf(9) }
    var endMinVal by remember { mutableIntStateOf(20) }

    // Dialog states
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }
    var showNewPlanDialog by remember { mutableStateOf(false) }
    var showEditSectionsDialog by remember { mutableStateOf(false) }
    var selectedTaskToAllocate by remember { mutableStateOf<Pair<Boolean, Long>?>(null) } // isUniTask to taskId

    val sectionColors = listOf(
        Color(0xFF2563EB), // Mrng Blue
        Color(0xFFBE185D), // College Magenta / Coral
        Color(0xFFD97706), // Evening Amber
        Color(0xFF7C3AED)  // Night Purple
    )

    // Collect all assigned task allocations in this plan
    val assignedAllocations = currentPlan.sections.flatMap { sec ->
        sec.allocatedTasks.map { it.isUniTask to it.taskId }
    }.toSet()

    // Separate tasks into unassigned (vibrant) and assigned (grey, moved to end)
    val unassignedTasks = tasks.filter { (false to it.id) !in assignedAllocations }
    val assignedTasks = tasks.filter { (false to it.id) in assignedAllocations }

    val unassignedUniTasks = uniTasks.filter { (true to it.id) !in assignedAllocations }
    val assignedUniTasks = uniTasks.filter { (true to it.id) in assignedAllocations }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vni_prefs", Context.MODE_PRIVATE) }
    var isDayPlannerEnabled by remember { mutableStateOf(prefs.getBoolean("day_planner_enabled", true)) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070417))
            .systemBarsPadding()
            .zIndex(85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ── 1. Top Header: [ON/OFF] on left | "Day planner" in middle | [✕ Close] on right ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left corner: ON/OFF toggle option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDayPlannerEnabled) Color(0xFF143820) else Color(0xFF26194E))
                        .border(
                            width = 1.dp,
                            color = if (isDayPlannerEnabled) Color(0xFF22C55E) else Color(0xFF4C2889),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            val newState = !isDayPlannerEnabled
                            isDayPlannerEnabled = newState
                            prefs.edit().putBoolean("day_planner_enabled", newState).apply()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDayPlannerEnabled) Color(0xFF22C55E) else Color(0xFF94A3B8))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isDayPlannerEnabled) "ON" else "OFF",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Top Middle: "Day planner"
                Text(
                    text = "Day planner",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Top Right: Close Button "✕"
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF100926))
                        .border(1.dp, Color(0xFF26194E), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Plan Tabs Row + Edit Button ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(plans) { plan ->
                        val isSelected = plan.name.equals(currentPlan.name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E1045) else Color(0xFF100926))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PurplePrimary else Color(0xFF26194E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectPlan(plan.name) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = plan.name,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    item {
                        // "+" Add new plan button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF100926))
                                .border(1.dp, Color(0xFF26194E), RoundedCornerShape(8.dp))
                                .clickable { showNewPlanDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Plan",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // "Edit" button beside "+" to edit/delete sections (User requirement 2)
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF140D30))
                        .border(1.dp, Color(0xFF3B1E7E), RoundedCornerShape(8.dp))
                        .clickable { showEditSectionsDialog = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Sections",
                            tint = PurpleLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 2. 24-Hour Vertical Time Map Container (Image 3) ────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF09051D))
                    .border(1.2.dp, Color(0xFF24154B), RoundedCornerShape(18.dp))
            ) {
                val containerWidth = maxWidth
                val centerX = containerWidth * 0.5f
                val hourHeight = 72.dp
                val totalTimelineHeight = hourHeight * 24 // 1728.dp
                val scrollState = rememberScrollState()

                // Smooth scroll to 6:00 AM on initial display
                LaunchedEffect(Unit) {
                    val targetPx = with(density) { (hourHeight * 5.8f).toPx().toInt() }
                    scrollState.scrollTo(targetPx)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalTimelineHeight)
                        .verticalScroll(scrollState)
                ) {
                    // Continuous Solid White Center Vertical Line (Image 3)
                    Box(
                        modifier = Modifier
                            .offset(x = centerX - 1.25.dp, y = 0.dp)
                            .width(2.5.dp)
                            .height(totalTimelineHeight)
                            .background(Color.White)
                    )

                    // 3-Hour Time Marks along the vertical line
                    val timeTicks = listOf(
                        0 to "12:00 AM",
                        3 to "3:00",
                        6 to "6:00",
                        9 to "9:00",
                        12 to "12:00",
                        15 to "3:00",
                        18 to "6:00",
                        21 to "9:00",
                        24 to "12:00 AM"
                    )

                    timeTicks.forEach { (hour, label) ->
                        val yOffset = hourHeight * hour

                        // Horizontal tick mark crossing the center line
                        Box(
                            modifier = Modifier
                                .offset(x = centerX - 8.dp, y = yOffset - 1.dp)
                                .width(16.dp)
                                .height(2.dp)
                                .background(Color.White)
                        )

                        // Text Label right beside center line
                        Text(
                            text = "- $label",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(x = centerX + 12.dp, y = yOffset - 11.dp)
                        )

                        if (hour == 6) {
                            Text(
                                text = "AM",
                                color = Color(0xFFA855F7),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .offset(x = containerWidth - 44.dp, y = yOffset - 11.dp)
                            )
                        } else if (hour == 18) {
                            Text(
                                text = "PM",
                                color = Color(0xFFA855F7),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .offset(x = containerWidth - 44.dp, y = yOffset - 11.dp)
                            )
                        }
                    }

                    // Section Dividers (Dividing Lines, Left Pills, Speech Badges, Tasks)
                    currentPlan.sections.forEachIndexed { sIdx, section ->
                        val startFrac = section.startHour + (section.startMinute / 60f)
                        val endFrac = section.endHour + (section.endMinute / 60f)
                        val startY = hourHeight * startFrac
                        val endY = hourHeight * endFrac
                        val sectionHeight = (endY - startY).coerceAtLeast(60.dp)
                        val sectionColor = sectionColors.getOrElse(section.colorIndex % sectionColors.size) { Color(0xFF2563EB) }

                        // 1. Horizontal Starting Line crossing timeline from left pill to right edge
                        Box(
                            modifier = Modifier
                                .offset(x = 36.dp, y = startY)
                                .width(containerWidth - 36.dp)
                                .height(2.dp)
                                .background(sectionColor)
                        )

                        // 2. Horizontal Ending Line crossing timeline from left pill to right edge
                        Box(
                            modifier = Modifier
                                .offset(x = 36.dp, y = endY)
                                .width(containerWidth - 36.dp)
                                .height(2.dp)
                                .background(sectionColor)
                        )

                        // 3. Vertical Section Indicator Capsule on the left
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = startY + 3.dp)
                                .width(28.dp)
                                .height((sectionHeight - 6.dp).coerceAtLeast(44.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(sectionColor.copy(alpha = 0.35f))
                                .border(1.dp, sectionColor, RoundedCornerShape(12.dp))
                                .clickable { onDeleteSection(section.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }

                        // 4. [ Wake up ] Badge (Image 3)
                        if (section.isWakeUpStart) {
                            Box(
                                modifier = Modifier
                                    .offset(x = (centerX - 110.dp).coerceAtLeast(40.dp), y = startY - 38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF140D2F))
                                    .border(1.dp, Color(0xFF381E72), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Wake up",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 5. [ Sleep ] Badge (Image 3)
                        if (section.isSleepEnd) {
                            Box(
                                modifier = Modifier
                                    .offset(x = (centerX - 110.dp).coerceAtLeast(40.dp), y = endY + 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF140D2F))
                                    .border(1.dp, Color(0xFF381E72), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Sleep",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 6. Assigned Tasks within this section (Staggered Layout Image 3)
                        // Left side: odd priorities (1, 3, 5...)
                        // Right side: even priorities (2, 4, 6...)
                        val sortedAllocations = section.allocatedTasks.sortedBy { it.priority }
                        sortedAllocations.forEach { alloc ->
                            val isLeft = alloc.priority % 2 != 0
                            val row = if (isLeft) (alloc.priority - 1) / 2 else (alloc.priority - 2) / 2
                            val taskYOffset = startY + 12.dp + (44.dp * row)
                            val cardWidth = 118.dp
                            val xOffset = if (isLeft) (centerX - cardWidth - 8.dp) else (centerX + 8.dp)

                            val isUni = alloc.isUniTask
                            val cardBg = if (isUni) {
                                Brush.horizontalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F766E)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF381480), Color(0xFF5B21B6)))
                            }
                            val cardBorder = if (isUni) Color(0xFF14B8A6) else Color(0xFF8B5CF6)

                            Box(
                                modifier = Modifier
                                    .offset(x = xOffset, y = taskYOffset)
                                    .width(cardWidth)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg)
                                    .border(1.dp, cardBorder.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        onRemoveAllocation(section.id, alloc.taskId, alloc.isUniTask)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "${alloc.priority}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = alloc.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Disabled overlay if Day Planner is OFF
                if (!isDayPlannerEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0070417))
                            .clickable(enabled = false) {}
                            .zIndex(60f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1045))
                                    .border(1.dp, Color(0xFF4C2889), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PowerSettingsNew,
                                    contentDescription = "Disabled",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Day planner is OFF",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "This section is deactivated. No notification suggestions or reminders will be sent according to day plans while turned off.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF16A34A))
                                    .clickable {
                                        isDayPlannerEnabled = true
                                        prefs.edit().putBoolean("day_planner_enabled", true).apply()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Turn ON",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── 3. Section Division Inputs Row (Start, End, Name, Set) (Image 3) ─
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Start Input Box
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF130A2A))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                            .clickable { showTimePickerForStart = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (isStartWakeUp) "Wake up" else startInputText,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // End Input Box
                Column(modifier = Modifier.weight(1f)) {
                    Text("End", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF130A2A))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                            .clickable { showTimePickerForEnd = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (isEndSleep) "Sleep" else endInputText,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Name Input Box
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("Name", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF130A2A))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = nameInputText,
                            onValueChange = { nameInputText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (nameInputText.isEmpty()) {
                                    Text("e.g. Mrng", color = TextMuted.copy(0.6f), fontSize = 12.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                // [ Set ] Button (Image 3)
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(PurplePrimary, Color(0xFF6D28D9))
                            )
                        )
                        .clickable {
                            if (nameInputText.isNotBlank()) {
                                onAddSection(
                                    nameInputText.trim(),
                                    if (isStartWakeUp) "Wake up" else startInputText,
                                    if (isEndSleep) "Sleep" else endInputText,
                                    isStartWakeUp,
                                    isEndSleep,
                                    startHourVal,
                                    startMinVal,
                                    endHourVal,
                                    endMinVal
                                )
                                nameInputText = ""
                            }
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 4. Task & Uni-Task Allocation Chips (Image 3) ───────────────────
            // Tasks Section
            Text(
                text = "Tasks",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Tasks Chips Row (Unassigned vibrant purple, Assigned at end in grey)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unassignedTasks) { task ->
                    TaskAllocationChip(
                        name = task.name.lowercase(),
                        isAssigned = false,
                        isUniTask = false,
                        onClick = {
                            selectedTaskToAllocate = false to task.id
                        }
                    )
                }
                items(assignedTasks) { task ->
                    TaskAllocationChip(
                        name = task.name.lowercase(),
                        isAssigned = true,
                        isUniTask = false,
                        onClick = {
                            selectedTaskToAllocate = false to task.id
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Uni-Tasks Section (Distinct Teal Color Scheme - User Requirement 2)
            Text(
                text = "Uni-Tasks",
                color = Color(0xFF2DD4BF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Uni-Tasks Chips Row (Unassigned vibrant teal, Assigned at end in grey)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unassignedUniTasks) { ut ->
                    TaskAllocationChip(
                        name = ut.name.lowercase(),
                        isAssigned = false,
                        isUniTask = true,
                        onClick = {
                            selectedTaskToAllocate = true to ut.id
                        }
                    )
                }
                items(assignedUniTasks) { ut ->
                    TaskAllocationChip(
                        name = ut.name.lowercase(),
                        isAssigned = true,
                        isUniTask = true,
                        onClick = {
                            selectedTaskToAllocate = true to ut.id
                        }
                    )
                }
            }
        }

        // ── 5. Time Picker Popup matching Image 2 + Scrollable wheel ────────
        if (showTimePickerForStart) {
            ScrollableTimePickerPopup(
                isStart = true,
                initialHour = startHourVal,
                initialMinute = startMinVal,
                initialIsWakeUpOrSleep = isStartWakeUp,
                onDismiss = { showTimePickerForStart = false },
                onConfirm = { hour24, hour12, min, isPm, isSpecial ->
                    isStartWakeUp = isSpecial
                    startHourVal = hour24
                    startMinVal = min
                    val amPm = if (isPm) "PM" else "AM"
                    startInputText = String.format("%02d:%02d %s", hour12, min, amPm)
                    showTimePickerForStart = false
                }
            )
        }

        if (showTimePickerForEnd) {
            ScrollableTimePickerPopup(
                isStart = false,
                initialHour = endHourVal,
                initialMinute = endMinVal,
                initialIsWakeUpOrSleep = isEndSleep,
                onDismiss = { showTimePickerForEnd = false },
                onConfirm = { hour24, hour12, min, isPm, isSpecial ->
                    isEndSleep = isSpecial
                    endHourVal = hour24
                    endMinVal = min
                    val amPm = if (isPm) "PM" else "AM"
                    endInputText = String.format("%02d:%02d %s", hour12, min, amPm)
                    showTimePickerForEnd = false
                }
            )
        }

        // ── 6. Priority & Section Allocation Dialog (Image 3) ───────────────
        if (selectedTaskToAllocate != null) {
            val (isUni, taskId) = selectedTaskToAllocate!!
            val taskName = if (isUni) {
                uniTasks.find { it.id == taskId }?.name ?: "Task"
            } else {
                tasks.find { it.id == taskId }?.name ?: "Task"
            }

            TaskAllocationDialog(
                taskName = taskName,
                sections = currentPlan.sections,
                onDismiss = { selectedTaskToAllocate = null },
                onConfirm = { secId, priority ->
                    onAllocateTask(secId, taskId, isUni, taskName, priority)
                    selectedTaskToAllocate = null
                }
            )
        }

        // ── 7. Edit Sections Dialog (Top beside +) (User requirement 2) ───────
        if (showEditSectionsDialog) {
            Dialog(onDismissRequest = { showEditSectionsDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0A28))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edit Sections",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1F1245))
                                    .clickable { showEditSectionsDialog = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (currentPlan.sections.isEmpty()) {
                            Text(
                                text = "No sections in this plan yet.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentPlan.sections) { sec ->
                                    val secColor = sectionColors.getOrElse(sec.colorIndex % sectionColors.size) { Color(0xFF2563EB) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF160E36))
                                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(secColor)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = sec.name,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${sec.startTime} - ${sec.endTime}",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Delete Button
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF3B1020))
                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .clickable { onDeleteSection(sec.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
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

        // ── 8. Add Plan Dialog ──────────────────────────────────────────────
        if (showNewPlanDialog) {
            var newPlanNameText by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showNewPlanDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0A28))
                        .border(1.dp, PurplePrimary, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Add New Plan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF170E38))
                                .border(1.dp, Color(0xFF381E72), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = newPlanNameText,
                                onValueChange = { newPlanNameText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (newPlanNameText.isEmpty()) Text("e.g. exams, holiday...", color = TextMuted, fontSize = 12.sp)
                                    inner()
                                }
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showNewPlanDialog = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Cancel", color = TextMuted, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurplePrimary)
                                    .clickable {
                                        if (newPlanNameText.isNotBlank()) {
                                            onAddPlan(newPlanNameText.trim())
                                        }
                                        showNewPlanDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip for Tasks (Purple) and Uni-Tasks (Teal):
 * - Distinct colors for headings & chips.
 * - Unassigned: vibrant color with crisp solid border.
 * - Assigned: Greyed out, moved to end.
 */
@Composable
private fun TaskAllocationChip(
    name: String,
    isAssigned: Boolean,
    isUniTask: Boolean,
    onClick: () -> Unit
) {
    val activeBg = if (isUniTask) Color(0xFF042F2C) else Color(0xFF1E1045)
    val activeBorder = if (isUniTask) Color(0xFF14B8A6) else PurplePrimary
    val activeText = if (isUniTask) Color(0xFFCCFBF1) else Color.White

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAssigned) Color(0xFF151026) else activeBg)
            .border(
                width = 1.dp,
                color = if (isAssigned) Color(0xFF261D42) else activeBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isAssigned) Color(0xFF64748B) else activeText,
            fontSize = 12.sp,
            fontWeight = if (isAssigned) FontWeight.Normal else FontWeight.Medium
        )
    }
}

/**
 * Image 2 Time Picker Popup with Scrollable Wheels (User requirement 2):
 * - Left side: [ Wake up ] button (for Start) or [ Sleep ] button (for End).
 * - Middle: Hour scrollable wheel & Minute scrollable wheel (plus up/down buttons).
 * - Right side: AM / PM toggle pill.
 * - Tapping outside assigns and dismisses.
 */
@Composable
private fun ScrollableTimePickerPopup(
    isStart: Boolean,
    initialHour: Int,
    initialMinute: Int,
    initialIsWakeUpOrSleep: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, hour12: Int, min: Int, isPm: Boolean, isSpecial: Boolean) -> Unit
) {
    var isSpecial by remember { mutableStateOf(initialIsWakeUpOrSleep) }
    var hour12 by remember {
        val h = if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour
        mutableIntStateOf(h)
    }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }

    Dialog(onDismissRequest = {
        val hour24 = if (isPm) {
            if (hour12 == 12) 12 else hour12 + 12
        } else {
            if (hour12 == 12) 0 else hour12
        }
        onConfirm(hour24, hour12, minute, isPm, isSpecial)
    }) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F0A28))
                .border(1.5.dp, Color(0xFF5B21B6), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: [ Wake up ] or [ Sleep ] Button
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSpecial) Color(0xFF2E1065) else Color(0xFF150D33))
                        .border(1.dp, if (isSpecial) PurplePrimary else Color(0xFF3B1E7E), RoundedCornerShape(10.dp))
                        .clickable {
                            isSpecial = !isSpecial
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isStart) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay,
                            contentDescription = null,
                            tint = if (isSpecial) Color.White else PurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isStart) "Wake up" else "Sleep",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color(0xFF2E1C6A))
                )

                // Middle: Hour & Minute Spinners with Up/Down Arrows + Scrollable Wheel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hour Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    hour12 = if (hour12 >= 12) 1 else hour12 + 1
                                    isSpecial = false
                                }
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF170E38))
                                .border(1.dp, Color(0xFF381E72), RoundedCornerShape(8.dp))
                                .clickable {
                                    // Cycle on tap or click
                                    hour12 = if (hour12 >= 12) 1 else hour12 + 1
                                    isSpecial = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", hour12),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Down",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    hour12 = if (hour12 <= 1) 12 else hour12 - 1
                                    isSpecial = false
                                }
                        )
                    }

                    Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    // Minute Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    minute = (minute + 5) % 60
                                    isSpecial = false
                                }
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF170E38))
                                .border(1.dp, Color(0xFF381E72), RoundedCornerShape(8.dp))
                                .clickable {
                                    minute = (minute + 5) % 60
                                    isSpecial = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", minute),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Down",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    minute = if (minute - 5 < 0) 55 else minute - 5
                                    isSpecial = false
                                }
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color(0xFF2E1C6A))
                )

                // Right: AM / PM Switcher
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF150D33))
                        .border(1.dp, Color(0xFF3B1E7E), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isPm) PurplePrimary else Color.Transparent)
                            .clickable { isPm = false; isSpecial = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            color = if (!isPm) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPm) PurplePrimary else Color.Transparent)
                            .clickable { isPm = true; isSpecial = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            color = if (isPm) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Image 3 Priority & Section Allocation Dialog:
 * - Left side: Section buttons (Mrng, College, Evening, Night, etc.)
 * - Right side: Priority numbers (1, 2, 3...)
 * - Bottom: [ Cancel ] and [ OKAY ] (crisp rounded rectangle, no glowing border)
 */
@Composable
private fun TaskAllocationDialog(
    taskName: String,
    sections: List<DayTimeSection>,
    onDismiss: () -> Unit,
    onConfirm: (sectionId: Long, priority: Int) -> Unit
) {
    val activeSections = if (sections.isNotEmpty()) sections else listOf(
        DayTimeSection(1, "Mrng", "07:00 AM", "09:20 AM"),
        DayTimeSection(2, "College", "09:20 AM", "04:40 PM")
    )

    var selectedSectionId by remember { mutableStateOf(activeSections.first().id) }
    var selectedPriority by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F0A28))
                .border(1.5.dp, Color(0xFF6B21A8), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = "Assign \"$taskName\"",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Sections
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Section",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        activeSections.forEach { sec ->
                            val isSel = sec.id == selectedSectionId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PurplePrimary else Color(0xFF160E36))
                                    .border(1.dp, if (isSel) Color.White.copy(0.6f) else Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                                    .clickable { selectedSectionId = sec.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = sec.name,
                                    color = if (isSel) Color.White else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(130.dp)
                            .background(Color(0xFF26194E))
                    )

                    // Right Column: Priority Numbers
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Priority",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val priorities = listOf(1, 2, 3, 4, 5)
                        priorities.forEach { p ->
                            val isSel = p == selectedPriority
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PurplePrimary else Color(0xFF160E36))
                                    .border(1.dp, if (isSel) Color.White.copy(0.6f) else Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                                    .clickable { selectedPriority = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$p",
                                    color = if (isSel) Color.White else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Bottom Buttons: [ Cancel ] [ OKAY ] (matching Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurplePrimary)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                onConfirm(selectedSectionId, selectedPriority)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OKAY", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
