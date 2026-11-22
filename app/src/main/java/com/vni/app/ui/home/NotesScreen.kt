package com.vni.app.ui.home

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.NoteItem
import com.vni.app.ui.security.PatternUnlockDialog
import com.vni.app.ui.theme.PurplePrimary

/**
 * Full-screen Notes Page:
 * - Top middle: Section Name ("Notes")
 * - Below it: [+ ADD NOTE] on left, [✕ Close] on right
 * - Headings and Subtopics implementation
 * - Collapsible: only heading shown by default, clicking opens subtopics
 * - Edit pencil on heading to modify heading & subcontent (protected notes require pattern)
 * - "Private" toggle in Add/Edit Note: access requires 9-dot pattern
 * - Bottom right dock with 3 sections: (Plan, Do, Money)
 */
@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    onAddNote: (title: String, content: String, subtopics: List<String>, isPrivate: Boolean) -> Unit,
    onUpdateNote: (id: Long, title: String, content: String, subtopics: List<String>, isPrivate: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteNote: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenPlanSection: () -> Unit = {},
    onOpenDoSection: () -> Unit = {},
    onOpenMoneySection: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var subtopicsText by remember { mutableStateOf("") }
    var isPrivateNote by remember { mutableStateOf(false) }

    // Expanded notes set (only heading shown by default, click to open)
    var expandedNoteIds by remember { mutableStateOf(setOf<Long>()) }

    // Set of unlocked private note IDs in this session
    var unlockedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    var unlockingNoteId by remember { mutableStateOf<Long?>(null) }
    var unlockActionTarget by remember { mutableStateOf("EXPAND") } // "EXPAND" or "EDIT"

    // Edit Note dialog state
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editSubtopicsText by remember { mutableStateOf("") }
    var editIsPrivate by remember { mutableStateOf(false) }

    val indicatorColors = listOf(
        Color(0xFFF97316), // Orange
        Color(0xFF8B5CF6), // Purple
        Color(0xFF3B82F6), // Blue
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── Top Middle Section Name & Below it: Add and Close buttons in each corner ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Middle: "Notes"
                Text(
                    text = "Notes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(8.dp))

                // Below it: Add on left, Close on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [+ ADD NOTE] button
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0F3C))
                            .border(1.2.dp, PurplePrimary, RoundedCornerShape(8.dp))
                            .clickable {
                                noteTitle = ""
                                noteContent = ""
                                subtopicsText = ""
                                isPrivateNote = false
                                showAddNoteDialog = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Note",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "ADD NOTE",
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
                            .clickable(onClick = onDismiss)
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

            Spacer(Modifier.height(6.dp))

            // Notes List
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes yet. Tap [+ ADD NOTE] to create headings & subtopics!",
                        color = Color.Gray,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val barColor = indicatorColors[(note.id % indicatorColors.size).toInt()]
                        val isUnlocked = !note.isPrivate || (note.id in unlockedNoteIds)
                        val isExpanded = (note.id in expandedNoteIds)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF120A2B))
                                .border(1.dp, if (note.isPrivate) Color(0xFF7C3AED).copy(alpha = 0.6f) else Color(0xFF26194E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header row: Accent bar, Title/Heading, Lock badge, Edit button, Date, Delete button, Expand chevron
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (note.isPrivate && note.id !in unlockedNoteIds) {
                                                unlockingNoteId = note.id
                                                unlockActionTarget = "EXPAND"
                                            } else {
                                                expandedNoteIds = if (isExpanded) expandedNoteIds - note.id else expandedNoteIds + note.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left vertical accent indicator bar
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (note.isPrivate) Color(0xFFA855F7) else barColor)
                                    )
                                    Spacer(Modifier.width(10.dp))

                                    // Heading / Title
                                    Text(
                                        text = note.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (note.isPrivate) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF2D165B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isUnlocked) Icons.Outlined.LockOpen else Icons.Default.Lock,
                                                    contentDescription = "Private",
                                                    tint = Color(0xFFC084FC),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = if (isUnlocked) "UNLOCKED" else "PRIVATE",
                                                    color = Color(0xFFC084FC),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Edit Pencil button (Requirement 1: modifies heading and subcontent, protected notes ask pattern)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25164B))
                                            .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), CircleShape)
                                            .clickable {
                                                if (note.isPrivate && note.id !in unlockedNoteIds) {
                                                    unlockingNoteId = note.id
                                                    unlockActionTarget = "EDIT"
                                                } else {
                                                    editingNote = note
                                                    editTitle = note.title
                                                    editContent = note.content
                                                    editSubtopicsText = note.subtopics.joinToString("\n")
                                                    editIsPrivate = note.isPrivate
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Note",
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Date Badge pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1D1240))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = note.dateText,
                                            color = Color(0xFFC084FC),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // Delete "X" button
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25164B))
                                            .border(1.dp, Color(0xFF4C1D95).copy(alpha = 0.5f), CircleShape)
                                            .clickable { onDeleteNote(note.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    // Expand / Collapse Chevron indicator
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Subtopics and Content (Requirement 2: Only heading shown by default, click to open subtopics)
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        if (isUnlocked) {
                                            // Render subtopics
                                            if (note.subtopics.isNotEmpty()) {
                                                Spacer(Modifier.height(8.dp))
                                                Column(
                                                    modifier = Modifier.padding(start = 14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    note.subtopics.forEach { sub ->
                                                        Row(verticalAlignment = Alignment.Top) {
                                                            Text("•", color = Color(0xFFA78BFA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = sub,
                                                                color = Color(0xFFE2E8F0),
                                                                fontSize = 12.5.sp,
                                                                lineHeight = 16.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (note.content.isNotBlank()) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = note.content,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    modifier = Modifier.padding(start = 14.dp)
                                                )
                                            }

                                            if (note.subtopics.isEmpty() && note.content.isBlank()) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "No subtopics added yet. Tap ✏️ to add subtopics!",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(start = 14.dp)
                                                )
                                            }
                                        } else {
                                            // Locked placeholder
                                            Spacer(Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF1E1045))
                                                    .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        unlockingNoteId = note.id
                                                        unlockActionTarget = "EXPAND"
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "🔒 Subtopics protected • Tap to unlock",
                                                        color = Color(0xFFDDD6FE),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "Unlock >",
                                                        color = Color(0xFFA78BFA),
                                                        fontSize = 11.5.sp,
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
                }
            }

            // Pinned Bottom Dock with 3 sections: Plan, Do, Money
            NoticeBottomDock(
                selectedItem = "Notes",
                onSelect = {},
                onOpenMoneySection = onOpenMoneySection,
                onOpenDoSection = onOpenDoSection,
                onOpenPlanSection = onOpenPlanSection
            )
        }

        // ── Pattern Unlock Dialog ──
        if (unlockingNoteId != null) {
            PatternUnlockDialog(
                title = if (unlockActionTarget == "EDIT") "Unlock Note to Edit" else "Unlock Private Note",
                onSuccess = {
                    val targetId = unlockingNoteId!!
                    unlockedNoteIds = unlockedNoteIds + targetId
                    unlockingNoteId = null
                    if (unlockActionTarget == "EDIT") {
                        val targetNote = notes.find { it.id == targetId }
                        if (targetNote != null) {
                            editingNote = targetNote
                            editTitle = targetNote.title
                            editContent = targetNote.content
                            editSubtopicsText = targetNote.subtopics.joinToString("\n")
                            editIsPrivate = targetNote.isPrivate
                        }
                    } else {
                        expandedNoteIds = expandedNoteIds + targetId
                    }
                },
                onOpenSettings = {
                    unlockingNoteId = null
                    onOpenSettings()
                },
                onDismiss = { unlockingNoteId = null }
            )
        }

        // ── Add Note Dialog with Headings, Subtopics & Private Option ──
        if (showAddNoteDialog) {
            Dialog(onDismissRequest = { showAddNoteDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0926))
                        .border(1.5.dp, Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ADD NEW NOTE",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showAddNoteDialog = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Heading input
                        Text("HEADING", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = noteTitle,
                                onValueChange = { noteTitle = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (noteTitle.isEmpty()) {
                                Text("e.g. Chapter 4: Calculus", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Subtopics input (line-separated)
                        Text("SUBTOPICS (one per line)", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = subtopicsText,
                                onValueChange = { subtopicsText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                            if (subtopicsText.isEmpty()) {
                                Text("Derivatives rule\nChain rule\nPractice questions...", color = Color(0xFF64748B), fontSize = 12.5.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Extra detail / Notes (optional)
                        Text("EXTRA DETAILS (optional)", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF180E38))
                                .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            BasicTextField(
                                value = noteContent,
                                onValueChange = { noteContent = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                            if (noteContent.isEmpty()) {
                                Text("Additional notes or formulas...", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Private Option Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPrivateNote) Color(0xFF261250) else Color(0xFF170E30))
                                .border(1.dp, if (isPrivateNote) Color(0xFFA855F7) else Color(0xFF33206A), RoundedCornerShape(10.dp))
                                .clickable { isPrivateNote = !isPrivateNote }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPrivateNote) Icons.Default.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = if (isPrivateNote) Color(0xFFA855F7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Private Note", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Requires pattern lock to view subtopics", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                }
                            }
                            Switch(
                                checked = isPrivateNote,
                                onCheckedChange = { isPrivateNote = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF7C3AED)
                                )
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val subs = subtopicsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                onAddNote(noteTitle, noteContent, subs, isPrivateNote)
                                showAddNoteDialog = false
                            },
                            enabled = noteTitle.isNotBlank() || subtopicsText.isNotBlank() || noteContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("SAVE NOTE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Edit Note Dialog with Heading, Subcontent & Private Option (Requirement 1) ──
        if (editingNote != null) {
            Dialog(onDismissRequest = { editingNote = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0926))
                        .border(1.5.dp, Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EDIT NOTE",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { editingNote = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Heading input
                        Text("HEADING", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (editTitle.isEmpty()) {
                                Text("Heading / Title...", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Subtopics input (one per line)
                        Text("SUBTOPICS (one per line)", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = editSubtopicsText,
                                onValueChange = { editSubtopicsText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                            if (editSubtopicsText.isEmpty()) {
                                Text("Enter subtopics (one per line)...", color = Color(0xFF64748B), fontSize = 12.5.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Extra detail / Notes (optional)
                        Text("EXTRA DETAILS (optional)", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF180E38))
                                .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            BasicTextField(
                                value = editContent,
                                onValueChange = { editContent = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                cursorBrush = SolidColor(PurplePrimary),
                                modifier = Modifier.fillMaxSize()
                            )
                            if (editContent.isEmpty()) {
                                Text("Additional notes or formulas...", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Private Option Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (editIsPrivate) Color(0xFF261250) else Color(0xFF170E30))
                                .border(1.dp, if (editIsPrivate) Color(0xFFA855F7) else Color(0xFF33206A), RoundedCornerShape(10.dp))
                                .clickable { editIsPrivate = !editIsPrivate }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (editIsPrivate) Icons.Default.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = if (editIsPrivate) Color(0xFFA855F7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Private Note", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Requires pattern lock to view subtopics", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                }
                            }
                            Switch(
                                checked = editIsPrivate,
                                onCheckedChange = { editIsPrivate = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF7C3AED)
                                )
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val subs = editSubtopicsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                onUpdateNote(editingNote!!.id, editTitle, editContent, subs, editIsPrivate)
                                editingNote = null
                            },
                            enabled = editTitle.isNotBlank() || editSubtopicsText.isNotBlank() || editContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
