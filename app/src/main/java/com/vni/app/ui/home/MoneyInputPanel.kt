package com.vni.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.PurplePrimary

/**
 * Money input panel matching Image 3:
 * Appears above keyboard with "Money", "purpose" fields and a date selector.
 */
@Composable
fun MoneyInputPanel(
    isExpense: Boolean,
    onConfirm: (amount: Int, purpose: String, dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var purposeText by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val themeColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF22C55E)

    val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
    val displayDate = dateFormat.format(java.util.Date(selectedDateMillis))

    // Simple date cycling: tap to go back one day
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss)
            .imePadding()
            .zIndex(75f)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0C071E))
                    .border(1.5.dp, themeColor.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row: Title + Close X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpense) "SPENT MONEY (-)" else "ADD INCOME (+)",
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF130B30))
                                .border(1.dp, Color(0xFF2E1C6A), CircleShape)
                                .clickable {
                                    keyboardController?.hide()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Date selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF130B30))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(10.dp))
                                .clickable {
                                    // Cycle backwards by one day on each click
                                    val c = java.util.Calendar.getInstance()
                                    c.timeInMillis = selectedDateMillis
                                    c.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                    selectedDateMillis = c.timeInMillis
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Pick date",
                                tint = themeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = displayDate,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Reset to today button
                        Text(
                            text = "Today",
                            color = PurplePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF221555))
                                .clickable { selectedDateMillis = System.currentTimeMillis() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Row matching Image 3: Money input on left, Purpose input on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Field: Money
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Money",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF130B30))
                                    .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹ ",
                                        color = themeColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    BasicTextField(
                                        value = amountText,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                                amountText = it
                                            }
                                        },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(themeColor),
                                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                        decorationBox = { inner ->
                                            if (amountText.isEmpty()) {
                                                Text("0", color = Color(0xFF64748B), fontSize = 18.sp)
                                            }
                                            inner()
                                        }
                                    )
                                }
                            }
                        }

                        // Right Field: Purpose
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "purpose",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF130B30))
                                    .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = purposeText,
                                    onValueChange = { if (it.length <= 30) purposeText = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val amt = amountText.toIntOrNull() ?: 0
                                        if (amt > 0) {
                                            onConfirm(amt, purposeText, selectedDateMillis)
                                            keyboardController?.hide()
                                        }
                                    }),
                                    singleLine = true,
                                    cursorBrush = SolidColor(themeColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (purposeText.isEmpty()) {
                                            Text(
                                                text = if (isExpense) "e.g. Coffee" else "e.g. Salary",
                                                color = Color(0xFF64748B),
                                                fontSize = 15.sp
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }
                        }

                        // Confirm Checkmark button
                        Box(
                            modifier = Modifier
                                .padding(top = 22.dp)
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(themeColor)
                                .clickable {
                                    val amt = amountText.toIntOrNull() ?: 0
                                    if (amt > 0) {
                                        onConfirm(amt, purposeText, selectedDateMillis)
                                        keyboardController?.hide()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
