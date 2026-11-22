package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Notice
import com.vni.app.ui.theme.BorderNavy
import com.vni.app.ui.theme.DangerRed
import com.vni.app.ui.theme.PurplePrimary
import com.vni.app.ui.theme.SurfaceNavy
import com.vni.app.ui.theme.TextMuted
import com.vni.app.ui.theme.TextWhite

@Composable
fun NoticeGridSection(
    notices: List<Notice>,
    doItems: List<com.vni.app.data.model.DoItem> = emptyList(),
    onRemoveNotice: (Long) -> Unit,
    onRemoveDoItem: (Long) -> Unit = {},
    onAddNotice: () -> Unit = {},
    onAddRemind: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayNotices = notices.filter { it.isToday }
    val allNotices = notices.filter { !it.isToday }

    // Requirement 6: Surface Do items with deadline today or tomorrow in Reminders
    val cal = java.util.Calendar.getInstance()
    val todayDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonthStr = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time).uppercase()
    val todayStr1 = "$todayDay $todayMonthStr"
    val todayStr2 = String.format("%02d %s", todayDay, todayMonthStr)

    val tomCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
    val tomDay = tomCal.get(java.util.Calendar.DAY_OF_MONTH)
    val tomMonthStr = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(tomCal.time).uppercase()
    val tomStr1 = "$tomDay $tomMonthStr"
    val tomStr2 = String.format("%02d %s", tomDay, tomMonthStr)

    val doReminders = doItems.filter { item ->
        val text = item.deadlineDateText.trim().uppercase()
        text == "TODAY" || text == todayStr1 || text == todayStr2 ||
        text == "TOMORROW" || text == tomStr1 || text == tomStr2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom panel
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        // --- REMIND ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            RemindSectionHeader("--- REMIND ---", onAdd = onAddRemind, onHistory = onHistoryClick)
        }
        if (todayNotices.isNotEmpty()) {
            items(todayNotices, span = { GridItemSpan(maxLineSpan) }) { notice ->
                TodayNoticeCard(notice = notice, onRemove = { onRemoveNotice(notice.id) })
            }
        }

        // --- NOTICE ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            NoticeSectionHeaderWithAdd("--- NOTICE ---", onAdd = onAddNotice)
        }
        
        items(allNotices) { notice ->
            NoticeItemCard(notice = notice, onRemove = { onRemoveNotice(notice.id) })
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(title, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun NoticeSectionHeaderWithAdd(title: String, onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title, 
            color = Color(0xFF64748B), 
            fontSize = 13.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        // Add Button
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color(0xFF221555), RoundedCornerShape(8.dp))
                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                .clickable { onAdd() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = TextWhite, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("ADD", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TodayNoticeCard(
    notice: Notice,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceNavy)
            .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = notice.title,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notice.subtitle,
                    color = Color(0xFFC084FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    val deadlineFull = if (!notice.timeRange.isNullOrEmpty()) "${notice.deadlineText} | ${notice.timeRange}" else notice.deadlineText
                    Text(
                        text = deadlineFull,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF130B30))
                    .border(1.dp, Color(0xFF2E1C6A), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = TextWhite,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun NoticeItemCard(
    notice: Notice,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceNavy)
            .border(1.dp, PurplePrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = notice.title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (notice.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notice.subtitle,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                if (notice.deadlineText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF221555))
                            .border(0.8.dp, PurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = notice.deadlineText,
                            color = Color(0xFFA78BFA),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF130B30))
                    .border(1.dp, Color(0xFF2E1C6A), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = TextWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}


@Composable
fun RemindSectionHeader(title: String, onAdd: () -> Unit, onHistory: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // History button on left (rectangular with curved edges)
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(Color(0xFF1C1040), RoundedCornerShape(8.dp))
                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                .clickable { onHistory() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.History, contentDescription=null, tint=TextWhite, modifier=Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("History >", color=TextWhite, fontSize=12.sp, fontWeight=FontWeight.Bold)
        }
        
        Text(
            text = title, 
            color = TextMuted, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        // Add Button (rectangular with curved edges)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color(0xFF1C1040), RoundedCornerShape(8.dp))
                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                .clickable { onAdd() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription=null, tint=TextWhite, modifier=Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("ADD", color=TextWhite, fontSize=12.sp, fontWeight=FontWeight.Bold)
        }
    }
}
