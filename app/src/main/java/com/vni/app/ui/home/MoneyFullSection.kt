package com.vni.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.DailyMoneyFlow
import com.vni.app.HistoryItem
import com.vni.app.RepeatSpentItem
import com.vni.app.ui.theme.*

/**
 * Full Money Section matching Image 1 & Image 2:
 * - HISTORY list (left, scrollable) with "All" filter dropdown
 * - REPEAT SPENT panel (right, collapsible with "-") with x to mark as deleted
 * - WEEKLY MONEY FLOW graph (Week/Month toggle)
 * - Bottom action bar: CLEAR COMPLETED | MONEY VIEW ALL ENTRIES | ADD NEW ENTRY
 */
@Composable
fun MoneyFullSection(
    moneyHistory: List<HistoryItem>,
    repeatSpentItems: List<RepeatSpentItem>,
    weeklyMoneyFlow: List<DailyMoneyFlow>,
    monthlyMoneyFlow: List<DailyMoneyFlow>,
    onMarkRepeatDeleted: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onAddNewEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRepeatPanel by remember { mutableStateOf(true) }
    var graphMode by remember { mutableStateOf("Week") } // "Week" or "Month"

    val activeFlowData = if (graphMode == "Week") weeklyMoneyFlow else monthlyMoneyFlow

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // ── Top Section: HISTORY + REPEAT SPENT ──────────────────────────
        if (showRepeatPanel) {
            // Image 1: Side by side layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: HISTORY
                HistoryPanel(
                    moneyHistory = moneyHistory,
                    modifier = Modifier.weight(1f)
                )

                // Right: REPEAT SPENT
                RepeatSpentPanel(
                    items = repeatSpentItems,
                    onMarkDeleted = onMarkRepeatDeleted,
                    onCollapse = { showRepeatPanel = false },
                    modifier = Modifier.weight(0.85f)
                )
            }
        } else {
            // Image 2: History takes full width
            HistoryPanelFull(
                moneyHistory = moneyHistory,
                onExpandRepeat = { showRepeatPanel = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── WEEKLY MONEY FLOW Graph ──────────────────────────────────────
        MoneyFlowGraph(
            data = activeFlowData,
            graphMode = graphMode,
            onModeChange = { graphMode = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(Modifier.height(14.dp))

        // ── Bottom Action Bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CLEAR COMPLETED
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onClearCompleted() }
            ) {
                Text("CLEAR", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text("COMPLETED", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }

            // MONEY VIEW ALL ENTRIES
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF221555))
                    .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MONEY", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("VIEW ALL ENTRIES", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }

            // Line + ADD NEW ENTRY
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(Color(0xFF2E1C6A))
                )
                Spacer(Modifier.width(6.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAddNewEntry() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221555))
                            .border(1.dp, PurplePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("ADD", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("NEW ENTRY", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── HISTORY Panel (compact, for side-by-side) ────────────────────────────
@Composable
private fun HistoryPanel(
    moneyHistory: List<HistoryItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("HISTORY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                // All dropdown
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF130B30))
                        .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable history items
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(moneyHistory.filter { it.type == "Money" }) { item ->
                    HistoryRow(item)
                }
            }
        }
    }
}

// ── HISTORY Panel (full width, for Image 2 when Repeat Spent is collapsed) ──
@Composable
private fun HistoryPanelFull(
    moneyHistory: List<HistoryItem>,
    onExpandRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("HISTORY", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // All dropdown
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF130B30))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Calendar / expand repeat button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF221555))
                            .border(1.dp, PurplePrimary, RoundedCornerShape(10.dp))
                            .clickable { onExpandRepeat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Expand", tint = PurpleLight, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable history items - full width
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(moneyHistory.filter { it.type == "Money" }) { item ->
                    HistoryRowFull(item)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem) {
    val icon = getIconForTitle(item.title)
    val isPositive = item.amountInt >= 0
    val amountColor = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val amountText = if (isPositive) "+${item.amountInt}" else "${item.amountInt}"
    val displayName = if (item.isDeleted) "deleted" else item.title

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = displayName,
                color = if (item.isDeleted) Color(0xFF64748B) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
        }
        Text(
            text = amountText,
            color = amountColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.date,
            color = Color(0xFF64748B),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HistoryRowFull(item: HistoryItem) {
    val icon = getIconForTitle(item.title)
    val isPositive = item.amountInt >= 0
    val amountColor = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val amountText = if (isPositive) "+${item.amountInt}" else "${item.amountInt}"
    val displayName = if (item.isDeleted) "deleted" else item.title

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = displayName,
                color = if (item.isDeleted) Color(0xFF64748B) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
        }
        Text(
            text = amountText,
            color = amountColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.date,
            color = Color(0xFF64748B),
            fontSize = 11.sp
        )
    }
}

// ── REPEAT SPENT Panel ───────────────────────────────────────────────────
@Composable
private fun RepeatSpentPanel(
    items: List<RepeatSpentItem>,
    onMarkDeleted: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("REPEAT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("SPENT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                // Collapse (-) button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF130B30))
                        .border(1.dp, PurplePrimary, CircleShape)
                        .clickable { onCollapse() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Repeat spent items
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items) { item ->
                    RepeatSpentRow(
                        item = item,
                        onDelete = { onMarkDeleted(item.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatSpentRow(
    item: RepeatSpentItem,
    onDelete: () -> Unit
) {
    val icon = getIconForTitle(item.name)
    val displayName = if (item.isDeleted) "deleted" else "${item.name} - ${item.totalSpent}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = displayName,
                color = if (item.isDeleted) Color(0xFF64748B) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
        }
        if (!item.isDeleted) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF130B30))
                    .border(1.dp, Color(0xFF2E1C6A), CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
    }
}

// ── Money Flow Graph ─────────────────────────────────────────────────────
@Composable
private fun MoneyFlowGraph(
    data: List<DailyMoneyFlow>,
    graphMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        // Header row with Week/Month toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BarChart, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (graphMode == "Week") "WEEKLY MONEY FLOW" else "MONTHLY MONEY FLOW",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF130B30))
                    .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(14.dp))
            ) {
                listOf("Week", "Month").forEach { mode ->
                    val isActive = graphMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) PurplePrimary else Color.Transparent)
                            .clickable { onModeChange(mode) }
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = mode,
                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Graph Canvas
        val maxVal = data.maxOfOrNull { kotlin.math.abs(it.amount) }?.coerceAtLeast(100) ?: 400
        val yRange = maxVal * 1.3f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0825))
                .border(0.5.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
        ) {
            // Y-axis labels + Graph
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val yLabels = listOf(maxVal.toString(), (maxVal / 2).toString(), "0", (-maxVal / 2).toString())
                    yLabels.forEach { label ->
                        Text(
                            text = label,
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Canvas area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 24.dp, end = 8.dp)) {
                        val w = size.width
                        val h = size.height
                        val midY = h * 0.5f

                        // Zero line (dashed)
                        drawLine(
                            color = Color(0xFF2E1C6A),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )

                        // Draw data points and lines
                        val dataWithPos = data.filter { it.hasData }
                        if (dataWithPos.isEmpty()) return@Canvas

                        val step = if (data.size > 1) w / (data.size - 1).toFloat() else w / 2f

                        // Build point coordinates for ALL data
                        val allPoints = data.mapIndexed { idx, d ->
                            val x = idx * step
                            val normalizedY = if (yRange > 0) d.amount.toFloat() / yRange else 0f
                            val y = midY - (normalizedY * h * 0.45f)
                            Triple(x, y, d)
                        }

                        // Draw gradient fill under the line
                        val pointsWithData = allPoints.filter { it.third.hasData }
                        if (pointsWithData.size >= 2) {
                            val fillPath = Path().apply {
                                moveTo(pointsWithData.first().first, midY)
                                pointsWithData.forEach { (x, y, _) -> lineTo(x, y) }
                                lineTo(pointsWithData.last().first, midY)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFC084FC).copy(alpha = 0.25f),
                                        Color(0xFFC084FC).copy(alpha = 0.02f)
                                    )
                                )
                            )
                        }

                        // Draw connecting lines between data points
                        for (i in 0 until pointsWithData.size - 1) {
                            val (x1, y1, _) = pointsWithData[i]
                            val (x2, y2, _) = pointsWithData[i + 1]
                            drawLine(
                                color = Color(0xFFC084FC),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Draw circles on data points
                        allPoints.forEach { (x, y, d) ->
                            if (d.hasData) {
                                // Outer glow
                                drawCircle(
                                    color = Color(0xFFC084FC).copy(alpha = 0.3f),
                                    radius = 8f,
                                    center = Offset(x, y)
                                )
                                // Solid circle
                                drawCircle(
                                    color = Color(0xFFC084FC),
                                    radius = 4.5f,
                                    center = Offset(x, y)
                                )
                                // Inner white dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 2f,
                                    center = Offset(x, y)
                                )
                            }
                        }

                        // Find max point for label
                        val maxPoint = pointsWithData.maxByOrNull { it.third.amount }
                        if (maxPoint != null && maxPoint.third.amount != 0) {
                            // Draw label background + text is handled by overlay composable
                        }
                    }

                    // Max value label overlay
                    val dataWithPos = data.filter { it.hasData }
                    val maxItem = dataWithPos.maxByOrNull { it.amount }
                    if (maxItem != null && maxItem.amount != 0) {
                        val step = if (data.size > 1) 1f / (data.size - 1).toFloat() else 0.5f
                        val idx = data.indexOf(maxItem)
                        val xFraction = idx * step

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp, bottom = 24.dp, end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (xFraction * 280).dp) // approximate positioning
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+${maxItem.amount}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // X-axis labels at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        data.forEach { d ->
                            Text(
                                text = d.dayLabel,
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helper: icon for transaction title ───────────────────────────────────
private fun getIconForTitle(title: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (title.lowercase()) {
        "work" -> Icons.Outlined.Work
        "biscuit" -> Icons.Outlined.Cookie
        "dinner" -> Icons.Outlined.Restaurant
        "milk" -> Icons.Outlined.LocalDrink
        "movie" -> Icons.Outlined.Movie
        "home" -> Icons.Outlined.Home
        "coffee" -> Icons.Outlined.Coffee
        "grocery", "grocery shopping" -> Icons.Outlined.ShoppingCart
        "book", "book store" -> Icons.Outlined.MenuBook
        else -> Icons.Outlined.Receipt
    }
}
