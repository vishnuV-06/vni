package com.vni.app.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.HistoryItem
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.home.v2.HomeTopMotiveBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Complete History Page matching Image 1:
 * - Top Motive quote, chevrons logo, notification bell, settings
 * - Categories Tab Row with [+] button on the left (e.g. [+] [All] [Done] [Notes] [Money] + user categories)
 * - Holding a category tab for 1 sec prompts to delete that category (with confirmation dialog)
 * - Grouped by Month banner (e.g. "NOVEMBER 2026")
 * - History cards with icon, title, amount / date badge, and [✕] delete button
 * - [+] button opens dialog to add a new category or add a custom history entry
 */
@Composable
fun HistoryPanel(
    deletedNotices: List<HistoryItem> = emptyList(),
    moneyHistory: List<HistoryItem> = emptyList(),
    historyCategories: List<String> = listOf("All", "Done", "Notes", "Money"),
    motiveHeadings: List<MotiveHeading> = emptyList(),
    notificationsEnabled: Boolean = true,
    onToggleNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRemoveItem: (Long) -> Unit = {},
    onAddCategory: (String) -> Unit = {},
    onRemoveCategory: (String) -> Unit = {},
    onAddHistoryItem: (title: String, category: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Merge deleted items and money history
    val allItems = remember(deletedNotices, moneyHistory) {
        val list = (deletedNotices + moneyHistory).distinctBy { it.id }
        list.sortedByDescending { if (it.dateMillis > 0L) it.dateMillis else it.id }
    }

    val displayItems = remember(selectedCategory, allItems) {
        when {
            selectedCategory.equals("All", ignoreCase = true) -> allItems
            selectedCategory.equals("Done", ignoreCase = true) -> allItems.filter { it.type.equals("Done", ignoreCase = true) }
            selectedCategory.equals("Notes", ignoreCase = true) -> allItems.filter { it.type.equals("Notes", ignoreCase = true) }
            selectedCategory.equals("Money", ignoreCase = true) -> allItems.filter { it.type.equals("Money", ignoreCase = true) }
            else -> allItems.filter { it.type.equals(selectedCategory, ignoreCase = true) || it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    // Group items by month banner (e.g. "NOVEMBER 2026")
    val groupedByMonth = remember(displayItems) {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        displayItems.groupBy { item ->
            val time = if (item.dateMillis > 0L) item.dateMillis else item.id
            monthFormat.format(Date(time)).uppercase()
        }
    }

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
        ) {
            // ── Section A: Top Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            Spacer(Modifier.height(10.dp))

            // ── Categories Row with [+] on the left (Image 1) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [+] Button to add new category or history record
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
                        contentDescription = "Add Category / History",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                historyCategories.forEach { cat ->
                    val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF38157A) else Color(0xFF14082D))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFA855F7) else Color(0xFF3B1A6E),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (isSelected) Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(10.dp), spotColor = Color(0xFFA855F7))
                                else Modifier
                            )
                            .pointerInput(cat) {
                                detectTapGestures(
                                    onTap = { selectedCategory = cat },
                                    onLongPress = {
                                        // 1-sec hold prompts to delete category (except All, Done, Notes, Money)
                                        if (!cat.equals("All", ignoreCase = true) &&
                                            !cat.equals("Done", ignoreCase = true) &&
                                            !cat.equals("Notes", ignoreCase = true) &&
                                            !cat.equals("Money", ignoreCase = true)) {
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
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(10.dp))

            // ── History Items Grouped by Month ──
            if (groupedByMonth.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history records for $selectedCategory",
                        color = Color(0xFF64748B),
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    groupedByMonth.forEach { (monthBanner, itemsInMonth) ->
                        item(key = "banner_$monthBanner") {
                            // Month Banner e.g. [ NOVEMBER 2026 ]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF220E49))
                                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = monthBanner,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color(0xFF381A6B))
                                )
                            }
                        }

                        items(itemsInMonth, key = { it.id }) { item ->
                            HistoryCardItem(
                                item = item,
                                onRemove = { onRemoveItem(item.id) }
                            )
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
                        Text(
                            text = "Delete Category?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Are you sure you want to delete category \"$categoryToDelete\"?",
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
                                    if (selectedCategory == cat) selectedCategory = "All"
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

        // ── Add Category / Add Item Dialog (From [+] Button) ──
        if (showAddCategoryDialog) {
            var isAddingEntry by remember { mutableStateOf(false) }
            var inputText by remember { mutableStateOf("") }
            var entryCategory by remember { mutableStateOf(if (selectedCategory == "All") "Done" else selectedCategory) }

            Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Mode switcher tabs: New Category | Add History
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isAddingEntry) Color(0xFFA855F7) else Color(0xFF160A36))
                                    .clickable { isAddingEntry = false; inputText = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("New Category", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAddingEntry) Color(0xFFA855F7) else Color(0xFF160A36))
                                    .clickable { isAddingEntry = true; inputText = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add History", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isAddingEntry) {
                            // Category selector for entry
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                historyCategories.filter { !it.equals("All", ignoreCase = true) }.forEach { cat ->
                                    val isSel = cat.equals(entryCategory, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF7C3AED) else Color(0xFF160A36))
                                            .clickable { entryCategory = cat }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(cat, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (isAddingEntry) "Enter history title..." else "Category name (e.g. Tasks, Travel)",
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
                                    val clean = inputText.trim()
                                    if (clean.isNotBlank()) {
                                        if (isAddingEntry) {
                                            onAddHistoryItem(clean, entryCategory)
                                            Toast.makeText(context, "Added to $entryCategory history", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onAddCategory(clean)
                                            selectedCategory = clean
                                            Toast.makeText(context, "Added category $clean", Toast.LENGTH_SHORT).show()
                                        }
                                        showAddCategoryDialog = false
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

/**
 * Individual History Card matching Image 1:
 * - Left rounded square badge with icon (Wallet / Notes / Done)
 * - Title and negative/positive amount or subtitle
 * - Date badge on right (e.g. "12 Nov")
 * - Rounded square [✕] delete button on the right
 */
@Composable
private fun HistoryCardItem(
    item: HistoryItem,
    onRemove: () -> Unit
) {
    val isMoney = item.type.equals("Money", ignoreCase = true)
    val isExpense = item.amountInt < 0

    val icon = when {
        isMoney -> Icons.Outlined.AccountBalanceWallet
        item.type.equals("Done", ignoreCase = true) -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.StickyNote2
    }
    val iconTint = when {
        isMoney && isExpense -> Color(0xFF38BDF8)
        isMoney -> Color(0xFF4ADE80)
        else -> Color(0xFFA855F7)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF12072D))
            .border(1.2.dp, Color(0xFF3B1A6E), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Title & Amount/Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B0B3D))
                        .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.amount != null) {
                        Text(
                            text = item.amount,
                            color = if (isExpense) Color(0xFFEF4444) else Color(0xFF22C55E),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = item.type,
                            color = Color(0xFFA78BFA),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right: Date Pill ("12 Nov") + Close [✕] Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A0B38))
                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.date,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF220E49))
                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
