package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.ui.theme.*

@Composable
fun AddExpensePanel(
    isIncome: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    val titleStr = if (isIncome) "Add Income" else "Add Expense"
    val glowColor = if (isIncome) SuccessGreen else PurplePrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth()
                .background(Color(0xFF0C071E), RoundedCornerShape(24.dp))
                .border(2.dp, glowColor.copy(alpha=0.6f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {} // Consume clicks
                .padding(24.dp)
        ) {
            Column {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color(0xFF130B30), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = glowColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(titleStr, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Track every rupee, build a better you", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF130B30), RoundedCornerShape(16.dp))
                                .padding(horizontal=12.dp, vertical=8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(12.dp)) // diamond icon roughly
                            Spacer(Modifier.width(4.dp))
                            Text("Small spends\nmake big awareness", color = TextMuted, fontSize = 9.sp, lineHeight = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.size(36.dp).border(1.dp, TextMuted.copy(0.3f), CircleShape).clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Row 1: Amount, What, Time
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Amount", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF130B30), RoundedCornerShape(12.dp)).padding(horizontal=16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹", color = TextMuted, fontSize = 20.sp)
                                        Spacer(Modifier.width(8.dp))
                                        if (amount.isEmpty()) Text("0", color = TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        inner()
                                    }
                                }
                            }
                        )
                    }
                    
                    Column(modifier = Modifier.weight(2f)) {
                        Text("What did you spend on?", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF130B30), RoundedCornerShape(12.dp)).border(1.dp, glowColor.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(horizontal=16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        if (note.isEmpty()) Text("e.g. Food, Movie, Travel...", color = TextMuted, fontSize = 14.sp)
                                        inner()
                                    }
                                }
                            }
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF130B30), RoundedCornerShape(12.dp)).border(1.dp, TextMuted.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(horizontal=12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("6:49", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(TextMuted.copy(0.3f)))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("AM", color = TextWhite, fontSize = 14.sp)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Row 2: Categories
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val cats = listOf(
                        "Food" to Icons.Outlined.Restaurant,
                        "Movie" to Icons.Outlined.Movie,
                        "Shopping" to Icons.Outlined.ShoppingBag,
                        "Travel" to Icons.Outlined.DirectionsBus,
                        "Games" to Icons.Outlined.SportsEsports,
                        "More" to Icons.Default.MoreHoriz
                    )
                    cats.forEach { (name, icon) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(name, color = TextWhite, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Divider(color = TextMuted.copy(alpha=0.2f))
                Spacer(Modifier.height(24.dp))
                
                // Row 3: Add to Today / Save Template
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFF130B30), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Add to Today", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("23 Sep 2026", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(TextMuted.copy(0.2f)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save as Template", color = TextMuted, fontSize = 14.sp)
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.width(36.dp).height(20.dp).background(Color(0xFF130B30), RoundedCornerShape(10.dp)), contentAlignment = Alignment.CenterEnd) {
                            Box(modifier = Modifier.padding(2.dp).size(16.dp).background(Color.White.copy(0.2f), CircleShape))
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Bottom Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(1.dp, TextMuted.copy(0.3f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp)
                            .background(glowColor, RoundedCornerShape(16.dp))
                            .border(2.dp, glowColor.copy(alpha=0.6f), RoundedCornerShape(16.dp)) // Glow illusion
                            .clickable(onClick = onSave),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TextWhite)
                            Spacer(Modifier.width(8.dp))
                            Text(titleStr, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
