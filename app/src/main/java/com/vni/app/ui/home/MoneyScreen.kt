package com.vni.app.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.HistoryItem
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.home.v2.HomeTopMotiveBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Money Section matching Image 2:
 * - Top Motive quote, chevrons logo, notification bell, settings
 * - Categories Tab Row with [+] button on the left (e.g. [+] [ALL] [NORMAL] + custom categories)
 *   - Holding a category for 1 sec prompts confirmation to delete it!
 * - Top Row: [INCOME ₹ 0] (green up arrow) and [EXPENSE ₹ 5] (red down arrow)
 * - Money Joystick Bar: [-] --- [ ₹ -5 ] --- [+] (drag to - triggers expense, drag to + triggers income)
 * - Middle Row:
 *   - Left: HISTORY card with [All v] dropdown and entries list
 *   - Right: Time period selector ([Week] / [Month]) listing weeks/months with individual totals:
 *     - When "Week" active: lists week 3, week 2, week 1 with their totals
 *     - When "Month" active: lists current month and past months with their totals
 *     - Default selects current week/month. New week/month starts fresh from 0 income / 0 expense!
 * - Bottom Card: WEEKLY MONEY FLOW line graph with points S M T W T F S and labels (100, 50, 0, -50)
 */
@Composable
fun MoneyScreen(
    moneyHistory: List<HistoryItem> = emptyList(),
    moneyCategories: List<String> = listOf("ALL", "NORMAL"),
    motiveHeadings: List<MotiveHeading> = emptyList(),
    notificationsEnabled: Boolean = true,
    onToggleNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onAddTransaction: (amount: Int, purpose: String, isExpense: Boolean, category: String) -> Unit = { _, _, _, _ -> },
    onAddCategory: (String) -> Unit = {},
    onRemoveCategory: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeCategory by remember { mutableStateOf("ALL") }
    val countPrefs = remember(context) { context.getSharedPreferences("vni_money_count_cats", android.content.Context.MODE_PRIVATE) }
    var countCategories by remember { mutableStateOf(countPrefs.getStringSet("count_set", emptySet()) ?: emptySet()) }
    val shortSavePrefs = remember(context) { context.getSharedPreferences("vni_money_short_save_cats", android.content.Context.MODE_PRIVATE) }
    var shortSaveCategories by remember { mutableStateOf(shortSavePrefs.getStringSet("short_save_set", emptySet()) ?: emptySet()) }

    val isCurrentCatCountOnly = remember(activeCategory, countCategories) {
        countCategories.contains(activeCategory.uppercase())
    }
    val isCurrentCatShortSave = remember(activeCategory, shortSaveCategories) {
        shortSaveCategories.contains(activeCategory.uppercase())
    }
    val currencyPrefix = if (isCurrentCatCountOnly) "" else "₹ "
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    // Period Mode: "Week" or "Month"
    var periodMode by remember { mutableStateOf("Week") }

    // Selected period index (0 = Current week/month, 1 = Previous, etc.)
    var selectedPeriodOffset by remember { mutableIntStateOf(0) }

    // History filter inside the Left History Card
    var historyFilterCategory by remember { mutableStateOf("All") }
    var historyDropdownExpanded by remember { mutableStateOf(false) }

    // Trigger quick transaction dialog from slider
    var pendingTransactionType by remember { mutableStateOf<Boolean?>(null) } // true = Expense (-), false = Income (+)

    val nowCal = remember { Calendar.getInstance() }
    val currentYear = nowCal.get(Calendar.YEAR)
    val currentMonth = nowCal.get(Calendar.MONTH)
    val currentWeekOfYear = nowCal.get(Calendar.WEEK_OF_YEAR)

    // Filter transactions strictly by active category (Requirement 3a: items in count category are NOT displayed in ALL)
    val categoryFilteredHistory = remember(activeCategory, moneyHistory, countCategories) {
        if (activeCategory.equals("ALL", ignoreCase = true)) {
            moneyHistory.filter { !countCategories.contains(it.category.uppercase()) }
        } else {
            moneyHistory.filter { it.category.equals(activeCategory, ignoreCase = true) }
        }
    }

    // Current calendar week of month & dynamic weeks / months list (Requirement 5)
    val currentWeekOfMonth = nowCal.get(Calendar.WEEK_OF_MONTH)
    val maxWeeksInMonth = nowCal.getActualMaximum(Calendar.WEEK_OF_MONTH)

    val weekPeriods = remember(currentWeekOfMonth, maxWeeksInMonth) {
        val list = mutableListOf<Pair<String, Int>>()
        for (w in maxWeeksInMonth downTo 1) {
            list.add("week $w" to (currentWeekOfMonth - w))
        }
        list
    }

    val monthPeriods = remember {
        val fmt = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
        (0..24).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
            fmt.format(c.time) to offset
        }
    }

    // Helper: start & end epoch millis for a given period offset
    fun getPeriodRange(isWeek: Boolean, offset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        if (isWeek) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            cal.add(Calendar.WEEK_OF_YEAR, -offset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val end = cal.timeInMillis
            return Pair(start, end)
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -offset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            return Pair(start, end)
        }
    }

    // Calculate total net for a period
    fun calculatePeriodNet(isWeek: Boolean, offset: Int): Int {
        val (start, end) = getPeriodRange(isWeek, offset)
        return categoryFilteredHistory
            .filter { it.dateMillis in start until end }
            .sumOf { it.amountInt }
    }

    // Active period's transactions
    val activePeriodRange = remember(periodMode, selectedPeriodOffset) {
        getPeriodRange(periodMode == "Week", selectedPeriodOffset)
    }

    val periodTransactions = remember(categoryFilteredHistory, activePeriodRange) {
        categoryFilteredHistory.filter { it.dateMillis in activePeriodRange.first until activePeriodRange.second }
    }

    val periodIncome = remember(periodTransactions) {
        periodTransactions.filter { it.amountInt > 0 }.sumOf { it.amountInt }
    }
    val periodExpense = remember(periodTransactions) {
        periodTransactions.filter { it.amountInt < 0 }.sumOf { kotlin.math.abs(it.amountInt) }
    }
    val periodNet = periodIncome - periodExpense

    // Weekly 7-day flow points for the bottom graph (S M T W T F S)
    val graphDailyPoints = remember(categoryFilteredHistory, periodMode, selectedPeriodOffset) {
        val sundayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            if (periodMode == "Week") {
                add(Calendar.WEEK_OF_YEAR, -selectedPeriodOffset)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        (0..6).map { dayIdx ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = sundayCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayIdx)
            }.timeInMillis
            val dayEnd = dayStart + 24 * 3600 * 1000L

            val dayNet = categoryFilteredHistory
                .filter { it.dateMillis in dayStart until dayEnd }
                .sumOf { it.amountInt }
            dayNet
        }
    }

    // Joystick horizontal drag state
    var joystickOffset by remember { mutableFloatStateOf(0f) }
    val animatedJoystickX by animateFloatAsState(
        targetValue = joystickOffset,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "JoystickX"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Section A: Top Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            // ── Category Tabs with [+] on the left (Image 2) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [+] Add Category Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E0B3E))
                        .border(1.2.dp, Color(0xFFA855F7), CircleShape)
                        .clickable { showAddCategoryDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Money Category",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                moneyCategories.forEach { cat ->
                    val isSel = cat.equals(activeCategory, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFFA855F7) else Color(0xFF14082D))
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) Color(0xFFE879F9) else Color(0xFF381A6B),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .then(
                                if (isSel) Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0xFFA855F7))
                                else Modifier
                            )
                            .pointerInput(cat) {
                                detectTapGestures(
                                    onTap = { activeCategory = cat },
                                    onLongPress = {
                                        // 1-sec hold prompts confirmation to delete custom category
                                        if (!cat.equals("ALL", ignoreCase = true) && !cat.equals("NORMAL", ignoreCase = true)) {
                                            categoryToDelete = cat
                                        } else {
                                            Toast.makeText(context, "System category '$cat' cannot be deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }

            // Thin neon hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF7C3AED).copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            // ── Top Row: INCOME & EXPENSE Cards (Image 2) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF064E3B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ArrowUpward, null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(if (isCurrentCatCountOnly) "COUNT +" else "INCOME", color = Color(0xFF94A3B8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text("$currencyPrefix$periodIncome", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Expense Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7F1D1D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ArrowDownward, null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(if (isCurrentCatCountOnly) "COUNT -" else "EXPENSE", color = Color(0xFF94A3B8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text("$currencyPrefix$periodExpense", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // ── Money Joystick Bar: [-] --- [ ₹ -5 ] --- [+] (Image 2) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE11D48), Color(0xFFA855F7), Color(0xFF10B981))
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: [-] Minus (Expense trigger)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E0E38))
                            .border(1.5.dp, Color(0xFFE11D48), CircleShape)
                            .clickable { pendingTransactionType = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Color(0xFFF87171), modifier = Modifier.size(22.dp))
                    }

                    // Center: Floating net pill [ ₹ -5 ]
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedJoystickX.roundToInt(), 0) }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (joystickOffset < -50f) {
                                            pendingTransactionType = true // Expense
                                        } else if (joystickOffset > 50f) {
                                            pendingTransactionType = false // Income
                                        }
                                        joystickOffset = 0f
                                    },
                                    onDragCancel = { joystickOffset = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        joystickOffset = (joystickOffset + dragAmount).coerceIn(-100f, 100f)
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF190B38))
                            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val sign = if (periodNet < 0) "- " else if (periodNet > 0) "+ " else ""
                        Text(
                            text = "$currencyPrefix$sign${kotlin.math.abs(periodNet)}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Right: [+] Plus (Income trigger)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E0E38))
                            .border(1.5.dp, Color(0xFF10B981), CircleShape)
                            .clickable { pendingTransactionType = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── Middle Row: Left HISTORY Card | Right Week/Month Selector Card (Image 2) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Left Card: HISTORY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.History, null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                                Text("HISTORY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            // Category Dropdown Filter inside History
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1B0B3D))
                                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(8.dp))
                                        .clickable { historyDropdownExpanded = true }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(historyFilterCategory, color = Color(0xFFCBD5E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                                }

                                DropdownMenu(
                                    expanded = historyDropdownExpanded,
                                    onDismissRequest = { historyDropdownExpanded = false }
                                ) {
                                    moneyCategories.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c) },
                                            onClick = {
                                                historyFilterCategory = c
                                                historyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val filteredHist = remember(historyFilterCategory, periodTransactions, isCurrentCatCountOnly, isCurrentCatShortSave, selectedPeriodOffset, periodMode) {
                            val base = if (historyFilterCategory.equals("All", ignoreCase = true)) periodTransactions
                            else periodTransactions.filter { it.category.equals(historyFilterCategory, ignoreCase = true) }

                            if (isCurrentCatCountOnly && isCurrentCatShortSave) {
                                if (periodMode == "Week" && selectedPeriodOffset > 0) {
                                    // Once the week is completed, all data shown as week data condensed (Requirement 3b)
                                    val totalAmount = base.sumOf { it.amountInt }
                                    if (base.isEmpty()) emptyList()
                                    else listOf(
                                        HistoryItem(
                                            id = -selectedPeriodOffset.toLong(),
                                            title = "Week ${maxWeeksInMonth - selectedPeriodOffset} Total",
                                            date = base.firstOrNull()?.date ?: "Past Week",
                                            type = "Money",
                                            amount = "$totalAmount",
                                            amountInt = totalAmount,
                                            dateMillis = base.firstOrNull()?.dateMillis ?: 0L,
                                            category = activeCategory
                                        )
                                    )
                                } else if (periodMode == "Month" && selectedPeriodOffset > 0) {
                                    // Once month is completed, all weeks are combined as one month single data (Requirement 3b)
                                    val totalAmount = base.sumOf { it.amountInt }
                                    if (base.isEmpty()) emptyList()
                                    else listOf(
                                        HistoryItem(
                                            id = -100L - selectedPeriodOffset,
                                            title = "${monthPeriods.getOrNull(selectedPeriodOffset)?.first ?: "Month"} Total",
                                            date = base.firstOrNull()?.date ?: "Past Month",
                                            type = "Money",
                                            amount = "$totalAmount",
                                            amountInt = totalAmount,
                                            dateMillis = base.firstOrNull()?.dateMillis ?: 0L,
                                            category = activeCategory
                                        )
                                    )
                                } else {
                                    // Present week / month shows all data of the week
                                    base
                                }
                            } else {
                                base
                            }
                        }

                        if (filteredHist.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No records in this period", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredHist.take(15), key = { it.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF14072C))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.InsertDriveFile, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                                            Text(
                                                item.title,
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "${if (item.amountInt < 0) "" else "+"}${item.amountInt}",
                                                color = if (item.amountInt < 0) Color(0xFFEF4444) else Color(0xFF22C55E),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(item.date, color = Color(0xFF64748B), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Right Card: Week / Month Period Selector (Image 2)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Sliding Pill: [ Week | Month ] (Image 2)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(Color(0xFF150A2E))
                                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(17.dp))
                                .padding(2.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val isWeek = (periodMode == "Week")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (isWeek) Color(0xFFA855F7) else Color.Transparent)
                                        .clickable {
                                            periodMode = "Week"
                                            selectedPeriodOffset = 0
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Week", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (!isWeek) Color(0xFFA855F7) else Color.Transparent)
                                        .clickable {
                                            periodMode = "Month"
                                            selectedPeriodOffset = 0
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Month", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Scrollable list of periods (all weeks in month or all past months) with individual totals
                        val periodsList = if (periodMode == "Week") weekPeriods else monthPeriods
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            periodsList.forEach { (label, offset) ->
                                val isSelected = (selectedPeriodOffset == offset)
                                val netAmount = calculatePeriodNet(periodMode == "Week", offset)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF26104E) else Color.Transparent)
                                        .clickable { selectedPeriodOffset = offset }
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )

                                    val sign = if (netAmount < 0) "- " else if (netAmount > 0) "+ " else ""
                                    val cur = if (isCurrentCatCountOnly) "" else "₹ "
                                    Text(
                                        text = "$cur$sign${kotlin.math.abs(netAmount)}",
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom Card: WEEKLY MONEY FLOW Graph (Image 2) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF261448), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(12.dp)
                                .background(Color(0xFFA855F7))
                        )
                        Text(
                            text = if (periodMode == "Week") "WEEKLY MONEY FLOW" else "MONTHLY MONEY FLOW",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left Y-axis labels: 100, 50, 0, -50
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            listOf("100", "50", "0", "-50").forEach { yl ->
                                Text(yl, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Graph Canvas with smooth spline line + purple dots + area glow
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw horizontal 0-line
                                val zeroY = h * 0.62f
                                drawLine(
                                    color = Color(0xFF1E0E38),
                                    start = Offset(0f, zeroY),
                                    end = Offset(w, zeroY),
                                    strokeWidth = 1.dp.toPx()
                                )

                                val points = graphDailyPoints
                                val stepX = w / (points.size - 1).coerceAtLeast(1)

                                val coords = points.mapIndexed { idx, value ->
                                    val clamped = value.coerceIn(-50, 100)
                                    // Map 100 -> 0.1f * h, 0 -> zeroY, -50 -> 0.9f * h
                                    val y = if (clamped >= 0) {
                                        zeroY - (clamped / 100f) * (zeroY - 0.1f * h)
                                    } else {
                                        zeroY + (-clamped / 50f) * (0.9f * h - zeroY)
                                    }
                                    Offset(idx * stepX, y)
                                }

                                // Fill under curve
                                val fillPath = Path().apply {
                                    moveTo(coords.first().x, coords.first().y)
                                    for (i in 1 until coords.size) {
                                        val prev = coords[i - 1]
                                        val cur = coords[i]
                                        cubicTo((prev.x + cur.x) / 2f, prev.y, (prev.x + cur.x) / 2f, cur.y, cur.x, cur.y)
                                    }
                                    lineTo(coords.last().x, h)
                                    lineTo(coords.first().x, h)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFA855F7).copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )

                                // Stroke curve
                                val strokePath = Path().apply {
                                    moveTo(coords.first().x, coords.first().y)
                                    for (i in 1 until coords.size) {
                                        val prev = coords[i - 1]
                                        val cur = coords[i]
                                        cubicTo((prev.x + cur.x) / 2f, prev.y, (prev.x + cur.x) / 2f, cur.y, cur.x, cur.y)
                                    }
                                }
                                drawPath(
                                    path = strokePath,
                                    color = Color(0xFFA855F7),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Glowing dots on data points
                                coords.forEach { pt ->
                                    drawCircle(
                                        color = Color(0xFFC084FC),
                                        radius = 3.5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.8.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }

                            // Bottom X-axis labels (S M T W T F S)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { l ->
                                    Text(l, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Transaction Quick Input Dialog (from [-] or [+]) ──
        if (pendingTransactionType != null) {
            val isExpense = pendingTransactionType == true
            var amountText by remember { mutableStateOf("") }
            var purposeText by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { pendingTransactionType = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, if (isExpense) Color(0xFFE11D48) else Color(0xFF10B981), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isExpense) "Add Expense (-)" else "Add Income (+)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text("Amount (₹):", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF160A36))
                                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = amountText,
                                onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (amountText.isEmpty()) {
                                Text("e.g. 50, 100", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }

                        Text("Title / Purpose:", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF160A36))
                                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = purposeText,
                                onValueChange = { purposeText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (purposeText.isEmpty()) {
                                Text("e.g. banana, coffee", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { pendingTransactionType = null }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = amountText.toIntOrNull() ?: 0
                                    if (amt > 0) {
                                        val title = purposeText.trim().ifEmpty { if (isExpense) "Spent" else "Income" }
                                        onAddTransaction(amt, title, isExpense, activeCategory)
                                        pendingTransactionType = null
                                        Toast.makeText(context, "${if (isExpense) "Expense" else "Income"} recorded", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isExpense) Color(0xFFE11D48) else Color(0xFF10B981)
                                )
                            ) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Delete Category Confirmation Dialog (1-second hold) ──
        if (categoryToDelete != null) {
            Dialog(onDismissRequest = { categoryToDelete = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Delete Category?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Are you sure you want to delete category \"$categoryToDelete\"?",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { categoryToDelete = null }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cat = categoryToDelete!!
                                    onRemoveCategory(cat)
                                    if (activeCategory == cat) activeCategory = "ALL"
                                    categoryToDelete = null
                                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Add Category Dialog (from [+] button) ──
        if (showAddCategoryDialog) {
            var newCatInput by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf("MONEY") } // "MONEY" or "COUNT"
            var saveMode by remember { mutableStateOf("SHORT_SAVE") } // "ALL_SAVE" or "SHORT_SAVE" (Requirement 3c)

            Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Add Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        // Requirement 6: Provide two buttons while creating: a. Money b. Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // [ a. Money ]
                            val isMoney = (selectedType == "MONEY")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMoney) Color(0xFFA855F7) else Color(0xFF1E0E3E))
                                    .border(1.dp, if (isMoney) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                    .clickable { selectedType = "MONEY" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Money (₹)",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isMoney) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            // [ b. Count ]
                            val isCount = (selectedType == "COUNT")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCount) Color(0xFFA855F7) else Color(0xFF1E0E3E))
                                    .border(1.dp, if (isCount) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                    .clickable { selectedType = "COUNT" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Count (123)",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCount) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Requirement 3c: When creating count category, provide [ All save ] and [ Short save ] buttons
                        if (selectedType == "COUNT") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isAllSave = (saveMode == "ALL_SAVE")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAllSave) Color(0xFF7C3AED) else Color(0xFF1E0E3E))
                                        .border(1.dp, if (isAllSave) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(8.dp))
                                        .clickable { saveMode = "ALL_SAVE" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("All save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                val isShortSave = (saveMode == "SHORT_SAVE")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isShortSave) Color(0xFF7C3AED) else Color(0xFF1E0E3E))
                                        .border(1.dp, if (isShortSave) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(8.dp))
                                        .clickable { saveMode = "SHORT_SAVE" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Short save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF160A36))
                                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = newCatInput,
                                onValueChange = { newCatInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (newCatInput.isEmpty()) {
                                Text(
                                    text = if (selectedType == "COUNT") "Category name (e.g. APPLES, PUSHUPS)" else "Category name (e.g. FOOD, TRAVEL)",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddCategoryDialog = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val clean = newCatInput.trim().uppercase()
                                    if (clean.isNotBlank()) {
                                        onAddCategory(clean)
                                        if (selectedType == "COUNT") {
                                            val updated = countCategories + clean
                                            countPrefs.edit().putStringSet("count_set", updated).apply()
                                            countCategories = updated

                                            if (saveMode == "SHORT_SAVE") {
                                                val updatedShort = shortSaveCategories + clean
                                                shortSavePrefs.edit().putStringSet("short_save_set", updatedShort).apply()
                                                shortSaveCategories = updatedShort
                                            } else {
                                                val updatedShort = shortSaveCategories - clean
                                                shortSavePrefs.edit().putStringSet("short_save_set", updatedShort).apply()
                                                shortSaveCategories = updatedShort
                                            }
                                        } else {
                                            val updated = countCategories - clean
                                            countPrefs.edit().putStringSet("count_set", updated).apply()
                                            countCategories = updated
                                        }
                                        activeCategory = clean
                                        showAddCategoryDialog = false
                                        Toast.makeText(context, "Category added ($selectedType)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                            ) {
                                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
