package com.vni.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.Motive
import com.vni.app.ui.theme.PurplePrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Motive Management Bottom Panel matching Image 5:
 * - Slide-from-bottom panel with glowing purple border.
 * - Header: [+ ADD] pill button on left, [✕ Close] pill button on right.
 * - Cards: Left colored vertical thick pill indicator, neon colored border (purple, cyan, orange),
 *   sentence text, and [✕] delete button.
 * - Deleting a sentence DOES NOT go to history!
 * - Added sentences are displayed at the top of the app in MotiveSection!
 */
@Composable
fun MotiveManagementPanel(
    motives: List<Motive>,
    onAddMotive: (String) -> Unit,
    onRemoveMotive: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var sentenceText by remember { mutableStateOf("") }

    val density = LocalDensity.current
    val sheetHeightPx = with(density) { 700.dp.toPx() }
    val offsetY = remember { Animatable(sheetHeightPx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun dismiss() {
        scope.launch {
            offsetY.animateTo(sheetHeightPx, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(55f)
    ) {
        // Scrim background (dismiss only via Close button)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Bottom Sheet matching Image 5
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF070417))
                .border(
                    width = 2.dp,
                    color = PurplePrimary,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Purple Top Handle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(54.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )

                Spacer(Modifier.height(14.dp))

                // Header Row: [+ ADD] on left | [✕ Close] on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + ADD Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.5.dp, PurplePrimary, RoundedCornerShape(18.dp))
                            .clickable {
                                sentenceText = ""
                                showAddDialog = true
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ADD",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // ✕ Close Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.5.dp, PurplePrimary, RoundedCornerShape(18.dp))
                            .clickable { dismiss() }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // List of Motives matching Image 5
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(motives, key = { _, m -> m.id }) { index, motive ->
                        val itemColor = when (index % 3) {
                            0 -> Color(0xFFA855F7) // Purple
                            1 -> Color(0xFF06B6D4) // Cyan
                            else -> Color(0xFFF97316) // Orange
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0D0824))
                                .border(1.5.dp, itemColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left thick colored vertical pill indicator
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(itemColor)
                                )

                                Spacer(Modifier.width(14.dp))

                                // Motive sentence text
                                Text(
                                    text = motive.text,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(Modifier.width(10.dp))

                                // Delete ✕ Button (DOES NOT go to history!)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF170E33))
                                        .border(1.dp, Color(0xFF33206A), CircleShape)
                                        .clickable { onRemoveMotive(motive.id) },
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
            }
        }

        // ── Add Motive Dialog ────────────────────────────────────────────────
        if (showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}
                    .zIndex(70f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F0829))
                        .border(2.dp, PurplePrimary, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            text = "Add Motive Sentence",
                            color = Color(0xFFA855F7),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C0F45))
                                .border(1.dp, Color(0xFF3E1F8C), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            if (sentenceText.isEmpty()) {
                                Text(
                                    text = "e.g. Consistency ,is the only key for the success",
                                    color = Color(0xFF6B7280),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = sentenceText,
                                onValueChange = { sentenceText = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1045))
                                    .clickable { showAddDialog = false }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1D4ED8))
                                    .clickable {
                                        if (sentenceText.isNotBlank()) {
                                            onAddMotive(sentenceText.trim())
                                            showAddDialog = false
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
