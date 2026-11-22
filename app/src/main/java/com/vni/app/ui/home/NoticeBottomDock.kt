package com.vni.app.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.*

data class DockItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Notice Bottom Dock matching Image 4:
 * - Permanent bottom dock is removed from the Notice section.
 * - Pinned rectangular side button at the bottom-right corner reveals/hides the 5-item dock.
 * - 5 items: Motive, Plan, Money, Do, Notes.
 * - Rectangular buttons with curved edges (RoundedCornerShape(8.dp)-10.dp), clean solid borders, no glowing halos.
 * - Tapping any item opens the corresponding full-screen page.
 */
@Composable
fun NoticeBottomDock(
    selectedItem: String,
    onSelect: (String) -> Unit = {},
    onOpenMoneySection: () -> Unit = {},
    onOpenDoSection: () -> Unit = {},
    onOpenMotiveSection: () -> Unit = {},
    onOpenPlanSection: () -> Unit = {},
    onOpenNotesSection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DockItem("Plan",   "Plan",   Icons.Outlined.CalendarMonth),
        DockItem("Do",     "Do",     Icons.Outlined.TaskAlt),
        DockItem("Money",  "Money",  Icons.Outlined.AccountBalanceWallet)
    )

    var isDockRevealed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(50f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Revealed 5-item bottom dock matching Image 4
            AnimatedVisibility(
                visible = isDockRevealed,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C071E))
                        .border(
                            width = 1.dp,
                            color = PurplePrimary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val isSelected = selectedItem == item.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E1045) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PurplePrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSelect(item.id)
                                        when (item.id) {
                                            "Plan"   -> onOpenPlanSection()
                                            "Do"     -> onOpenDoSection()
                                            "Money"  -> onOpenMoneySection()
                                        }
                                        isDockRevealed = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) PurpleLight else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Rectangular Side Button at Bottom-Right matching Image 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7C3AED))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFA78BFA),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { isDockRevealed = !isDockRevealed },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDockRevealed) Icons.Outlined.ExpandMore else Icons.Outlined.Widgets,
                        contentDescription = "Toggle Dock",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
