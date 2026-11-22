package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.theme.PurplePrimary

/**
 * Full-screen Motive Page:
 * - Insert headings as containers.
 * - Inside each heading, insert sentences.
 * - Each sentence has an "✕" button to remove it.
 * - If a heading contains 0 sentences, it is automatically deleted.
 * - Edit pencil icon next to heading name to rename it.
 * - Compact design with reduced padding and slim heights matching Notes/Do/Plan.
 */
@Composable
fun MotiveScreen(
    motiveHeadings: List<MotiveHeading>,
    onToggleExpandHeading: (Long) -> Unit,
    onAddHeading: (String) -> Unit,
    onAddSentence: (headingId: Long, sentence: String) -> Unit,
    onRemoveSentence: (headingId: Long, index: Int) -> Unit,
    onUpdateHeading: (headingId: Long, newName: String) -> Unit = { _, _ -> },
    onDeleteHeading: (headingId: Long) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddHeadingDialog by remember { mutableStateOf(false) }
    var newHeadingName by remember { mutableStateOf("") }

    var showEditHeadingDialog by remember { mutableStateOf(false) }
    var editingHeadingId by remember { mutableStateOf<Long?>(null) }
    var editingHeadingName by remember { mutableStateOf("") }

    var showAddSentenceDialogForHeadingId by remember { mutableStateOf<Long?>(null) }
    var newSentenceText by remember { mutableStateOf("") }

    val indicatorColors = listOf(
        Color(0xFF8B5CF6), // Purple
        Color(0xFF3B82F6), // Blue
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Green
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4)  // Cyan
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B061A))
            .zIndex(60f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── Top Middle: "Motive" Section Name & Below it: Corner Buttons ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Motive",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [+ ADD HEADING] button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0F3C))
                            .border(1.2.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable {
                                newHeadingName = ""
                                showAddHeadingDialog = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Heading",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ADD HEADING",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // [✕ Close] button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0F3C))
                            .border(1.2.dp, PurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Close",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Headings List ──
            if (motiveHeadings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No motive headings yet.\nTap [+ ADD HEADING] to create one!",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(motiveHeadings, key = { it.id }) { headingItem ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF100926))
                                .border(1.2.dp, Color(0xFF3B1E78), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Heading Card Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = if (headingItem.isExpanded) 10.dp else 0.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(18.dp)
                                                .clip(CircleShape)
                                                .background(PurplePrimary)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = headingItem.heading,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { onToggleExpandHeading(headingItem.id) }
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        // Edit pencil logo button to change heading name
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1C123E))
                                                .border(1.dp, PurplePrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    editingHeadingId = headingItem.id
                                                    editingHeadingName = headingItem.heading
                                                    showEditHeadingDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Heading Name",
                                                tint = Color(0xFFC084FC),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(6.dp))

                                        // Delete heading "X" button
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1C123E))
                                                .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable { onDeleteHeading(headingItem.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Heading",
                                                tint = Color(0xFFF43F5E),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onToggleExpandHeading(headingItem.id) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1C123E))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${headingItem.sentences.size} Sentences",
                                                color = Color(0xFFC084FC),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (headingItem.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (headingItem.isExpanded) {
                                    // Sentences list under this heading
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        headingItem.sentences.forEachIndexed { sIndex, sentence ->
                                            val barColor = indicatorColors[sIndex % indicatorColors.size]

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF150D33))
                                                    .border(1.dp, Color(0xFF26194E), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Left indicator pill
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.5.dp)
                                                            .height(20.dp)
                                                            .clip(CircleShape)
                                                            .background(barColor)
                                                    )
                                                    Spacer(Modifier.width(10.dp))

                                                    // Sentence Text
                                                    Text(
                                                        text = sentence,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    Spacer(Modifier.width(8.dp))

                                                    // [✕] delete button
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF1E1340))
                                                            .clickable {
                                                                onRemoveSentence(headingItem.id, sIndex)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete Sentence",
                                                            tint = Color(0xFFF43F5E),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // "+ Add New Sentence" button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF130B2A))
                                            .border(
                                                width = 1.dp,
                                                color = PurplePrimary.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                newSentenceText = ""
                                                showAddSentenceDialogForHeadingId = headingItem.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "Add New Sentence",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog: Add New Heading ──
        if (showAddHeadingDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showAddHeadingDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160D35))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "New Motive Category",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = newHeadingName,
                                onValueChange = { newHeadingName = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (newHeadingName.isEmpty()) {
                                        Text(text = "e.g., Coding, Fitness, Quotes...", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddHeadingDialog = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        if (newHeadingName.isNotBlank()) {
                                            onAddHeading(newHeadingName)
                                            showAddHeadingDialog = false
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog: Edit Heading Name (Edit pencil logo) ──
        if (showEditHeadingDialog && editingHeadingId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showEditHeadingDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160D35))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Edit Category Name",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = editingHeadingName,
                                onValueChange = { editingHeadingName = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showEditHeadingDialog = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        if (editingHeadingName.isNotBlank()) {
                                            onUpdateHeading(editingHeadingId!!, editingHeadingName)
                                            showEditHeadingDialog = false
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Save",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog: Add New Sentence to a Heading ──
        val targetHeadingId = showAddSentenceDialogForHeadingId
        if (targetHeadingId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showAddSentenceDialogForHeadingId = null },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160D35))
                        .border(1.5.dp, PurplePrimary, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add Motive Sentence",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0826))
                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = newSentenceText,
                                onValueChange = { newSentenceText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (newSentenceText.isEmpty()) {
                                        Text(text = "e.g., Small steps every day lead to giant leaps...", color = Color.Gray, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                },
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddSentenceDialogForHeadingId = null }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        if (newSentenceText.isNotBlank()) {
                                            onAddSentence(targetHeadingId, newSentenceText)
                                            showAddSentenceDialogForHeadingId = null
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add Sentence",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
