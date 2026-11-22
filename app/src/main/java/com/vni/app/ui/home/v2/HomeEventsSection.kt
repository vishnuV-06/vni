package com.vni.app.ui.home.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Section C: Events Screen (triggered by dragging the notice button to the left).
 * - Contains [ Week | Month ] toggle to filter events according to it.
 * - Displays all events added from calendar (by holding a day for 1 second).
 */
@Composable
fun HomeEventsSection(
    events: List<CalendarEvent>,
    onDeleteEvent: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onSidebarOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var periodFilter by remember { mutableStateOf("WEEK") } // "WEEK" or "MONTH"
    val cal = remember { Calendar.getInstance() }
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1
    val currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)

    val filteredEvents = remember(events, periodFilter) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        events.filter { event ->
            try {
                val d = sdf.parse(event.dateKey)
                if (d != null) {
                    val c = Calendar.getInstance().apply { time = d }
                    if (periodFilter == "WEEK") {
                        c.get(Calendar.YEAR) == currentYear && c.get(Calendar.WEEK_OF_YEAR) == currentWeekOfYear
                    } else {
                        c.get(Calendar.YEAR) == currentYear && (c.get(Calendar.MONTH) + 1) == currentMonth
                    }
                } else true
            } catch (e: Exception) {
                true
            }
        }.sortedBy { it.dateKey }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Header Row with [ Week | Month ] Filter ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF220E49))
                            .border(1.2.dp, Color(0xFFA855F7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ALL EVENTS",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // [ Week | Month ] Segmented Toggle
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF14082D))
                        .border(1.2.dp, Color(0xFF581C87), RoundedCornerShape(17.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Week
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (periodFilter == "WEEK") Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { periodFilter = "WEEK" }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Week",
                            color = if (periodFilter == "WEEK") Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Month
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (periodFilter == "MONTH") Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { periodFilter = "MONTH" }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Month",
                            color = if (periodFilter == "MONTH") Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Events List ──
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0724).copy(alpha = 0.7f))
                        .border(1.2.dp, Color(0xFF3B156A), RoundedCornerShape(20.dp))
                        .clickable { onOpenCalendar() }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No events scheduled for this $periodFilter.",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hold any day for 1 second in Calendar to add an event!",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredEvents.forEach { event ->
                        EventItemCard(
                            event = event,
                            onDelete = { onDeleteEvent(event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItemCard(
    event: CalendarEvent,
    onDelete: () -> Unit
) {
    // Format dateKey ("yyyy-MM-dd") to e.g. "17 Nov"
    val formattedDate = remember(event.dateKey) {
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(event.dateKey)
            if (d != null) SimpleDateFormat("dd MMM", Locale.ENGLISH).format(d).uppercase() else event.dateKey
        } catch (e: Exception) {
            event.dateKey
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF13092B))
            .border(1.3.dp, Color(0xFFA855F7).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0xFFA855F7))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Date Badge + Title/Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Neon Date Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF7C3AED), Color(0xFF581C87))
                            )
                        )
                        .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedDate,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.timeStr.isNotBlank()) {
                        Text(
                            text = event.timeStr,
                            color = Color(0xFFC084FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right: Delete button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF220E49))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Event",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
