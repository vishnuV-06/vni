package com.vni.app.ui.home.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.security.PatternUnlockDialog
import com.vni.app.ui.security.CryptoUtils
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.ChatCategory
import com.vni.app.data.model.ChatMessage
import com.vni.app.data.model.ChatTopic
import kotlinx.coroutines.launch

/**
 * Chat Section V2 matching Image 2:
 * 1. 3 Layers mirroring Notes:
 *    - Level 1: Categories (e.g. GENERAL, NOTES).
 *    - Level 2: Topics/Headings inside category (e.g. UPD).
 *    - Level 3: Conversation Screen (Image 2).
 * 2. Holding a category or topic for 1 second shows dialog to Update (rename) or Delete.
 * 3. Level 3 Conversation Screen (Image 2):
 *    - Top motive quote: "be the one / who never stops you"
 *    - Breadcrumbs: [ NOTES ] [ GENERAL ] > UPD > DELETE CHAT
 *    - White drag handle ━━━ (drag up expands to full screen, drag down restores)
 *    - Purple-bordered bubbles aligned on LEFT or RIGHT
 *    - Segmented [ LEFT | RIGHT ] alignment toggle
 *    - Capsule input pill: ( 💬 chat )
 */
@Composable
fun ChatV2Screen(
    chatCategories: List<ChatCategory>,
    motiveHeadings: List<MotiveHeading> = emptyList(),
    notificationsEnabled: Boolean = true,
    onToggleNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onAddCategory: (section: String, name: String) -> Unit,
    onUpdateCategory: (categoryId: Long, newName: String) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit,
    onAddTopic: (categoryId: Long, title: String) -> Unit,
    onUpdateTopicTitle: (categoryId: Long, topicId: Long, newTitle: String) -> Unit,
    onDeleteTopic: (categoryId: Long, topicId: Long) -> Unit,
    onAddMessage: (categoryId: Long, topicId: Long, text: String, isRight: Boolean) -> Unit,
    onDeleteMessage: (categoryId: Long, topicId: Long, messageId: Long) -> Unit,
    onDismiss: () -> Unit,
    onNavigationStateChanged: (level: Int) -> Unit = {},
    onRegisterBackAction: ((() -> Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Navigation Stack:
    // Level 1: selectedCategoryId == null
    // Level 2: selectedCategoryId != null && selectedTopicId == null
    // Level 3: selectedCategoryId != null && selectedTopicId != null
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }

    val currentLevel = when {
        selectedTopicId != null -> 3
        selectedCategoryId != null -> 2
        else -> 1
    }

    LaunchedEffect(currentLevel) {
        onNavigationStateChanged(currentLevel)
    }

    // Full screen toggle (Requirement 1d: drag bar handle ━━━ up expands full screen, down restores)
    var isChatFullScreen by remember { mutableStateOf(false) }

    // Privacy toggle when adding category: Normal vs Private (Requirement 1c)
    var categoryPrivacyType by remember { mutableStateOf("NORMAL") }
    var pendingPrivateCatToOpen by remember { mutableStateOf<ChatCategory?>(null) }
    var showPatternUnlockForChat by remember { mutableStateOf(false) }

    // Add Mode state (for Category / Topic)
    var isAddModeActive by remember { mutableStateOf(false) }
    var addTargetType by remember { mutableStateOf("CATEGORY") } // "CATEGORY" or "TOPIC"
    var addInputText by remember { mutableStateOf("") }

    // Long press 1s Action Dialog state
    var showActionDialog by remember { mutableStateOf(false) }
    var actionItemType by remember { mutableStateOf("CATEGORY") } // "CATEGORY" or "TOPIC"
    var actionItemId by remember { mutableStateOf(0L) }
    var actionItemParentId by remember { mutableStateOf(0L) }
    var actionItemCurrentName by remember { mutableStateOf("") }

    // Rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }

    // Message delete dialog
    var messageToDeleteId by remember { mutableStateOf<Long?>(null) }

    // Active Category and Topic
    val currentCategory = remember(chatCategories, selectedCategoryId) {
        chatCategories.firstOrNull { it.id == selectedCategoryId }
    }
    val currentTopic = remember(currentCategory, selectedTopicId) {
        currentCategory?.topics?.firstOrNull { it.id == selectedTopicId }
    }

    // Single-step back action (Requirement 2): steps back Level 3 -> Level 2 -> Level 1 -> exit
    val handleChatBack: () -> Boolean = {
        when {
            isAddModeActive -> {
                isAddModeActive = false
                addInputText = ""
                true
            }
            selectedTopicId != null -> {
                selectedTopicId = null // Level 3 -> Level 2
                true
            }
            selectedCategoryId != null -> {
                selectedCategoryId = null // Level 2 -> Level 1
                true
            }
            else -> false // Level 1: Let Home handle back
        }
    }

    LaunchedEffect(handleChatBack) {
        onRegisterBackAction?.invoke(handleChatBack)
    }

    // Handle back button step-by-step
    BackHandler {
        if (!handleChatBack()) {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.8f
                ),
                radius = w * 0.8f,
                center = Offset(w * 0.5f, h * 0.45f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section A: Motive Bar (Requirement 1d: shows motive bar when not full screen)
            if (!isChatFullScreen) {
                HomeTopMotiveBar(
                    motiveHeadings = motiveHeadings,
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = onToggleNotifications,
                    onOpenSettings = onOpenSettings
                )
                Spacer(Modifier.height(6.dp))
            }

            // White Drag Handle Bar ━━━ (Requirement 1d: drag up for full screen, drag down restores)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f }
                        ) { change, dragAmount ->
                            totalDrag += dragAmount
                            if (totalDrag < -20f) {
                                change.consume()
                                isChatFullScreen = true
                            } else if (totalDrag > 20f) {
                                change.consume()
                                isChatFullScreen = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                        .clickable { isChatFullScreen = !isChatFullScreen }
                )
            }

            Spacer(Modifier.height(6.dp))

            when {
                // ── Level 3: Conversation Screen (Image 2) ──
                currentCategory != null && currentTopic != null -> {
                    ChatConversationView(
                        category = currentCategory,
                        topic = currentTopic,
                        onBackToCategories = {
                            selectedCategoryId = null
                            selectedTopicId = null
                        },
                        onBackToTopics = {
                            selectedTopicId = null
                        },
                        onDeleteTopic = {
                            onDeleteTopic(currentCategory.id, currentTopic.id)
                            selectedTopicId = null
                        },
                        onAddMessage = { text, isRight ->
                            onAddMessage(currentCategory.id, currentTopic.id, text, isRight)
                        },
                        onDeleteMessage = { msgId ->
                            onDeleteMessage(currentCategory.id, currentTopic.id, msgId)
                        }
                    )
                }

                // ── Level 2: Inside Category - Topics List ──
                currentCategory != null -> {
                    Spacer(Modifier.height(10.dp))
                    ChatMotiveQuoteHeader()
                    Spacer(Modifier.height(10.dp))

                    ChatBreadcrumbHeader(
                        title1 = "CHAT",
                        title2 = currentCategory.name,
                        title3 = null,
                        onClick1 = { selectedCategoryId = null },
                        onClick2 = {},
                        onAddClick = {
                            addTargetType = "TOPIC"
                            addInputText = ""
                            isAddModeActive = true
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        if (currentCategory.topics.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No topics yet. Tap '+ New' to create topic",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(currentCategory.topics, key = { it.id }) { top ->
                                    GlowingItemCard(
                                        title = top.title,
                                        onClick = { selectedTopicId = top.id },
                                        onLongClick = {
                                            actionItemType = "TOPIC"
                                            actionItemId = top.id
                                            actionItemParentId = currentCategory.id
                                            actionItemCurrentName = top.title
                                            showActionDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom "+ New Topic" button bar
                    ChatBottomButtonBar(
                        label = "+ New Topic",
                        onClick = {
                            addTargetType = "TOPIC"
                            addInputText = ""
                            isAddModeActive = true
                        }
                    )
                }

                // ── Level 1: Categories List ──
                else -> {
                    Spacer(Modifier.height(10.dp))
                    ChatMotiveQuoteHeader()
                    Spacer(Modifier.height(10.dp))

                    ChatBreadcrumbHeader(
                        title1 = "CHAT",
                        title2 = "ALL CATEGORIES",
                        title3 = null,
                        onClick1 = onDismiss,
                        onClick2 = {},
                        onAddClick = {
                            addTargetType = "CATEGORY"
                            addInputText = ""
                            isAddModeActive = true
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        if (chatCategories.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No chat categories yet. Tap '+ New' to add category",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(chatCategories, key = { it.id }) { cat ->
                                    GlowingItemCard(
                                        title = if (cat.section.equals("PRIVATE", ignoreCase = true)) "${cat.name} 🔒" else cat.name,
                                        onClick = {
                                            if (cat.section.equals("PRIVATE", ignoreCase = true)) {
                                                pendingPrivateCatToOpen = cat
                                                showPatternUnlockForChat = true
                                            } else {
                                                selectedCategoryId = cat.id
                                            }
                                        },
                                        onLongClick = {
                                            actionItemType = "CATEGORY"
                                            actionItemId = cat.id
                                            actionItemParentId = 0L
                                            actionItemCurrentName = cat.name
                                            showActionDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom "+ New Category" button bar
                    ChatBottomButtonBar(
                        label = "+ New Category",
                        onClick = {
                            addTargetType = "CATEGORY"
                            addInputText = ""
                            isAddModeActive = true
                        }
                    )
                }
            }
        }

        // Add Category / Topic Input Bar Overlay
        if (isAddModeActive) {
            val addFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                addFocusRequester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isAddModeActive = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF14082D))
                        .border(1.6.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Requirement 1c: When creating category, offer [ Normal ] and [ Private ] buttons
                        if (addTargetType == "CATEGORY") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isNorm = (categoryPrivacyType == "NORMAL")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNorm) Color(0xFF7C3AED) else Color(0xFF1E0E3E))
                                        .border(1.dp, if (isNorm) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                        .clickable { categoryPrivacyType = "NORMAL" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Normal", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }

                                val isPriv = (categoryPrivacyType == "PRIVATE")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isPriv) Color(0xFF7C3AED) else Color(0xFF1E0E3E))
                                        .border(1.dp, if (isPriv) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(10.dp))
                                        .clickable { categoryPrivacyType = "PRIVATE" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text("Private", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = addInputText,
                                onValueChange = { addInputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(addFocusRequester),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val trimmed = addInputText.trim().uppercase()
                                        if (trimmed.isNotEmpty()) {
                                            if (addTargetType == "CATEGORY") {
                                                val sec = if (categoryPrivacyType == "PRIVATE") "PRIVATE" else "GENERAL"
                                                onAddCategory(sec, trimmed)
                                            } else if (currentCategory != null) {
                                                onAddTopic(currentCategory.id, trimmed)
                                            }
                                        }
                                        isAddModeActive = false
                                        addInputText = ""
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (addInputText.isEmpty()) {
                                        Text(
                                            text = if (addTargetType == "CATEGORY") "enter category name" else "enter topic title",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED))
                                    .clickable {
                                        val trimmed = addInputText.trim().uppercase()
                                        if (trimmed.isNotEmpty()) {
                                            if (addTargetType == "CATEGORY") {
                                                val sec = if (categoryPrivacyType == "PRIVATE") "PRIVATE" else "GENERAL"
                                                onAddCategory(sec, trimmed)
                                            } else if (currentCategory != null) {
                                                onAddTopic(currentCategory.id, trimmed)
                                            }
                                        }
                                        isAddModeActive = false
                                        addInputText = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Submit",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-Press 1-Second Action Dialog (Update or Delete)
        if (showActionDialog) {
            AlertDialog(
                onDismissRequest = { showActionDialog = false },
                containerColor = Color(0xFF14082D),
                title = {
                    Text(
                        text = "Manage $actionItemType: \"$actionItemCurrentName\"",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Choose to rename or delete this $actionItemType.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showActionDialog = false
                            renameInputText = actionItemCurrentName
                            showRenameDialog = true
                        }
                    ) {
                        Text("Update (Rename)", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showActionDialog = false
                                if (actionItemType == "CATEGORY") {
                                    onDeleteCategory(actionItemId)
                                } else {
                                    onDeleteTopic(actionItemParentId, actionItemId)
                                }
                            }
                        ) {
                            Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = { showActionDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                }
            )
        }

        // Rename Dialog
        if (showRenameDialog) {
            val renameFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                renameFocus.requestFocus()
            }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                containerColor = Color(0xFF14082D),
                title = {
                    Text(
                        text = "Rename $actionItemType",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        BasicTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0724))
                                .border(1.2.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .focusRequester(renameFocus),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFA855F7))
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val clean = renameInputText.trim().uppercase()
                            if (clean.isNotEmpty()) {
                                if (actionItemType == "CATEGORY") {
                                    onUpdateCategory(actionItemId, clean)
                                } else {
                                    onUpdateTopicTitle(actionItemParentId, actionItemId, clean)
                                }
                            }
                            showRenameDialog = false
                        }
                    ) {
                        Text("Save", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Pattern Unlock Dialog for Private Chat Categories
        if (showPatternUnlockForChat && pendingPrivateCatToOpen != null) {
            PatternUnlockDialog(
                title = "Private Chat Locked",
                onSuccess = {
                    selectedCategoryId = pendingPrivateCatToOpen?.id
                    showPatternUnlockForChat = false
                    pendingPrivateCatToOpen = null
                },
                onOpenSettings = {
                    showPatternUnlockForChat = false
                    onOpenSettings()
                },
                onDismiss = {
                    showPatternUnlockForChat = false
                    pendingPrivateCatToOpen = null
                }
            )
        }
    }
}

/**
 * Level 3: Chat Conversation Screen matching Image 2
 */
@Composable
private fun ChatConversationView(
    category: ChatCategory,
    topic: ChatTopic,
    onBackToCategories: () -> Unit,
    onBackToTopics: () -> Unit,
    onDeleteTopic: () -> Unit,
    onAddMessage: (text: String, isRight: Boolean) -> Unit,
    onDeleteMessage: (messageId: Long) -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) }
    var alignmentMode by remember { mutableStateOf("RIGHT") } // "LEFT" or "RIGHT"
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new message
    LaunchedEffect(topic.messages.size) {
        if (topic.messages.isNotEmpty()) {
            listState.animateScrollToItem(topic.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (!isFullScreen) {
            Spacer(Modifier.height(10.dp))
            ChatMotiveQuoteHeader()
            Spacer(Modifier.height(10.dp))
        }

        // ── Breadcrumb Bar (Image 2) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [ NOTES ]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF14082D))
                    .border(1.2.dp, Color(0xFF6B21A8), RoundedCornerShape(14.dp))
                    .clickable { onBackToCategories() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "NOTES",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            // [ CATEGORY ]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF7C3AED))
                    .clickable { onBackToTopics() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.name,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            // > TOPIC
            Text(
                text = "> ${topic.title}",
                color = Color(0xFFC084FC),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))

            // > DELETE CHAT
            Text(
                text = "> DELETE CHAT",
                color = Color(0xFFF43F5E),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDeleteTopic() }
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── White Drag Handle Bar ━━━ (Image 2) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    ) { change, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < -25f) {
                            change.consume()
                            isFullScreen = true
                        } else if (totalDrag > 25f) {
                            change.consume()
                            isFullScreen = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
                    .clickable { isFullScreen = !isFullScreen }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Chat Messages Stream (Image 2) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            if (topic.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet.\nType below to start the conversation",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(topic.messages, key = { _, msg -> msg.id }) { index, msg ->
                        val isLastOfDay = if (index == topic.messages.lastIndex) {
                            true
                        } else {
                            val c1 = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
                            val c2 = Calendar.getInstance().apply { timeInMillis = topic.messages[index + 1].timestamp }
                            c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) || c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
                        }
                        val dateLabel = SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(msg.timestamp)).lowercase()
                        ChatMessageBubble(
                            message = msg,
                            isLastOfDay = isLastOfDay,
                            dateLabel = dateLabel,
                            onDelete = { onDeleteMessage(msg.id) }
                        )
                    }
                }
            }
        }

        // ── Bottom Section: Alignment Selector + Input Pill (Image 2) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // [ LEFT | RIGHT ] Segmented Alignment Switch (Image 2)
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14082D))
                    .border(1.2.dp, Color(0xFF581C87), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (alignmentMode == "LEFT") Color(0xFF7C3AED) else Color.Transparent)
                        .clickable { alignmentMode = "LEFT" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LEFT",
                        color = if (alignmentMode == "LEFT") Color.White else Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // RIGHT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (alignmentMode == "RIGHT") Color(0xFF7C3AED) else Color.Transparent)
                        .clickable { alignmentMode = "RIGHT" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RIGHT",
                        color = if (alignmentMode == "RIGHT") Color.White else Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Capsule Input Bar: ( 💬 chat ) (Image 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.6.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val clean = inputText.trim()
                                if (clean.isNotEmpty()) {
                                    onAddMessage(clean, alignmentMode == "RIGHT")
                                    inputText = ""
                                    coroutineScope.launch {
                                        if (topic.messages.isNotEmpty()) {
                                            listState.animateScrollToItem(topic.messages.size)
                                        }
                                    }
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "chat",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (inputText.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val clean = inputText.trim()
                                    if (clean.isNotEmpty()) {
                                        onAddMessage(clean, alignmentMode == "RIGHT")
                                        inputText = ""
                                        coroutineScope.launch {
                                            if (topic.messages.isNotEmpty()) {
                                                listState.animateScrollToItem(topic.messages.size)
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Message Bubble matching Image 2:
 * Electric purple border, dark container, aligned to Left or Right.
 */
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isLastOfDay: Boolean = false,
    dateLabel: String = "",
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isRight) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isRight) Arrangement.End else Arrangement.Start
        ) {
            // For right-aligned messages: date displays on the LEFT in grey colour (Instagram style - Requirement 1b)
            if (isLastOfDay && message.isRight) {
                Text(
                    text = dateLabel,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.6.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showDeleteConfirm = true }
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }

            // For left-aligned messages: date displays on the RIGHT in grey colour (Instagram style - Requirement 1b)
            if (isLastOfDay && !message.isRight) {
                Text(
                    text = dateLabel,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF14082D),
            title = {
                Text("Delete Message?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(message.text, color = Color(0xFFCBD5E1), fontSize = 13.5.sp, maxLines = 3)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Top motive quote matching Image 2: "be the one / who never stops you"
 */
@Composable
private fun ChatMotiveQuoteHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "be ",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "the one",
                color = Color(0xFFA855F7),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = "who never stops you",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Breadcrumb header for Levels 1 and 2
 */
@Composable
private fun ChatBreadcrumbHeader(
    title1: String,
    title2: String?,
    title3: String?,
    onClick1: () -> Unit,
    onClick2: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [ CHAT ]
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF7C3AED))
                .clickable { onClick1() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = title1,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (title2 != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF14082D))
                    .border(1.2.dp, Color(0xFF6B21A8), RoundedCornerShape(14.dp))
                    .clickable { onClick2() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title2,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // [+ New] Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7C3AED))
                .clickable { onAddClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "New",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Glowing Item Card for Categories and Topics
 */
@Composable
private fun GlowingItemCard(
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF220E49), Color(0xFF14072C))
                )
            )
            .border(1.6.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Bottom Button Bar for Add
 */
@Composable
private fun ChatBottomButtonBar(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFF7C3AED))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
