package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DateTasksBar(
    completedCount: Int,
    totalCount: Int,
    isNotices: Boolean = false,
    noticeCount: Int = 0,
    showMoneyHeader: Boolean = false,
    moneyBalance: Int = 237,
    isUniTaskMode: Boolean = false,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: () -> Unit = {},
    onTasksDoubleClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onMoneyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val now = Calendar.getInstance()
    val dayName   = SimpleDateFormat("EEEE",   Locale.ENGLISH).format(now.time).uppercase()
    val dayNum    = SimpleDateFormat("dd",     Locale.ENGLISH).format(now.time)
    val monthYear = SimpleDateFormat("MMM\nyyyy", Locale.ENGLISH).format(now.time).uppercase()
    val pct = if (totalCount > 0) (completedCount * 100 / totalCount) else 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Date Card
        Row(
            modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .background(SurfaceNavy, RoundedCornerShape(16.dp))
                .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDateClick() }
            ) {
                Text(text = dayName, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = dayNum, color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(text = monthYear, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Calendar",
                tint = TextMuted,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCalendarClick() }
            )
        }

        // Right Card: Can be Money Card (Image 4), Notices Card, or Tasks Card!
        if (showMoneyHeader) {
            // MONEY Card matching Image 4!
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp)
                    .background(SurfaceNavy, RoundedCornerShape(16.dp))
                    .border(1.dp, PurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { onMoneyClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MONEY",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "₹ $moneyBalance",
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Total balance",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF221555), CircleShape)
                        .border(1.dp, PurplePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = "Money Wallet",
                        tint = PurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (isNotices) {
            // NOTICES Card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp)
                    .background(SurfaceNavy, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "NOTICES", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(text = String.format("%02d", noticeCount), color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(text = "Things to take care of", color = TextMuted, fontSize = 9.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (notificationsEnabled) Color(0xFF130B30) else Color(0xFF2C0B18))
                        .border(
                            width = 1.dp,
                            color = if (notificationsEnabled) PurpleLight.copy(0.4f) else Color(0xFFEF4444).copy(0.6f),
                            shape = CircleShape
                        )
                        .clickable { onToggleNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                        contentDescription = if (notificationsEnabled) "Notifications On" else "Notifications Off",
                        tint = if (notificationsEnabled) PurpleLight else Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // TASKS / Uni-Task Card (Double click to switch mode - matching Image 1)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp)
                    .background(SurfaceNavy, RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isUniTaskMode) PurplePrimary.copy(alpha = 0.6f) else BorderNavy,
                        RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onTasksDoubleClick() }
                        )
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isUniTaskMode) "Uni-Task" else "TASKS",
                        fontSize = if (isUniTaskMode) 13.sp else 10.sp,
                        color = if (isUniTaskMode) TextWhite else TextMuted,
                        fontWeight = if (isUniTaskMode) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = if (isUniTaskMode) 0.5.sp else 2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d", completedCount),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = " / " + String.format("%02d", totalCount),
                            fontSize = 18.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "COMPLETED TODAY",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }

                // Small circular progress
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFF2A1060),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = PurplePrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * (pct / 100f),
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$pct%",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
