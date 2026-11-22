package com.vni.app.ui.home.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.NoteCategory
import com.vni.app.data.model.NoteTopic
import com.vni.app.ui.security.PatternUnlockDialog
import com.vni.app.ui.security.HiddenPatternUnlockDialog
import com.vni.app.ui.security.HiddenNotesManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

data class NoteSearchResult(
    val categoryId: Long,
    val topicId: Long?,
    val title: String,
    val subtitle: String
)

/**
 * Notes Section V2 matching Images 1, 2, 3, 4:
 * 1. Image 1: First page with [NOTES] [GENERAL] [PRIMARY] [PRIVATE] tabs,
 *    glowing violet cards (e.g. MOTIVE, QUOTES), bottom search pill.
 * 2. Image 2: Category content page with breadcrumb [NOTES] [GENERAL] > MOTIVE,
 *    glowing violet sub-topic cards (e.g. GOOD WORDS, PHYLOSOPIC).
 * 3. Image 3: Note Editor page with breadcrumb [NOTES] [GENERAL] > MOTIVE > GOOD WORDS,
 *    big glowing purple container, toolbar formatting pills [• Points] [1. Number] [Normal],
 *    and free text editing with auto-bulleting/numbering on Enter.
 * 4. Image 4: Add Input Bar triggered by scrolling up from the bottom (or pull up),
 *    with placeholder "enter the new category" and vibrant purple circular [ > ] submit button.
 * 5. Private section pattern protection: Tapping [PRIVATE] requires the 9-dot pattern unlock.
 * 6. Global note search across all categories, topics, and contents.
 */
@Composable
fun NotesV2Screen(
    noteCategories: List<NoteCategory>,
    onAddCategory: (section: String, name: String) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit,
    onAddTopic: (categoryId: Long, title: String) -> Unit,
    onDeleteTopic: (categoryId: Long, topicId: Long) -> Unit,
    onUpdateTopic: (categoryId: Long, topicId: Long, content: String, formatType: String) -> Unit,
    onDismiss: () -> Unit,
    onUpdateCategory: ((categoryId: Long, newName: String) -> Unit)? = null,
    onUpdateTopicTitle: ((categoryId: Long, topicId: Long, newTitle: String) -> Unit)? = null,
    onNavigationStateChanged: ((level: Int) -> Unit)? = null,
    onRegisterBackAction: ((() -> Boolean) -> Unit)? = null,
    isEmbeddedInPager: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Current Active Tab: "GENERAL", "PRIMARY", "PRIVATE", "HIDDEN"
    var activeSectionTab by remember { mutableStateOf("GENERAL") }

    // Navigation Stack:
    // selectedCategoryId: null -> Level 1
    // selectedCategoryId: not null, selectedTopicId: null -> Level 2
    // selectedCategoryId: not null, selectedTopicId: not null -> Level 3
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }

    // Pattern Lock state for PRIVATE section
    var isPrivateUnlocked by remember { mutableStateOf(false) }
    var showPrivateUnlockDialog by remember { mutableStateOf(false) }

    // Ultra-Private hidden section state (triggered by 3-second silent hold on PRIVATE tab)
    var isUltraPrivateMode by remember { mutableStateOf(false) }
    var hiddenCategories by remember { mutableStateOf<List<NoteCategory>>(emptyList()) }
    var showHiddenUnlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isUltraPrivateMode) {
        if (isUltraPrivateMode) {
            hiddenCategories = HiddenNotesManager.loadCategories(context)
        }
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Add Mode ("CATEGORY" or "TOPIC") triggered by clicking [NOTES] button or [+ New]
    var isAddModeActive by remember { mutableStateOf(false) }
    var addTargetType by remember { mutableStateOf("CATEGORY") }
    var addInputText by remember { mutableStateOf("") }

    val currentLevel = when {
        selectedTopicId != null -> 3
        selectedCategoryId != null -> 2
        else -> 1
    }
    LaunchedEffect(currentLevel) {
        onNavigationStateChanged?.invoke(currentLevel)
    }

    var isFullScreenEditor by remember { mutableStateOf(false) }

    // Long press 1s Action Dialog state
    var showActionDialog by remember { mutableStateOf(false) }
    var actionItemType by remember { mutableStateOf("CATEGORY") }
    var actionItemId by remember { mutableStateOf(0L) }
    var actionItemParentId by remember { mutableStateOf(0L) }
    var actionItemCurrentName by remember { mutableStateOf("") }

    // Rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val addInputFocusRequester = remember { FocusRequester() }

    // Single-step back action (Requirement 2): General -> Motive -> Good words goes back to General -> Motive, etc.
    val handleNotesBack: () -> Boolean = {
        when {
            isAddModeActive -> {
                isAddModeActive = false
                addInputText = ""
                true
            }
            isSearchActive && searchQuery.isNotEmpty() -> {
                searchQuery = ""
                isSearchActive = false
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
            isUltraPrivateMode -> {
                isUltraPrivateMode = false
                activeSectionTab = "GENERAL"
                true
            }
            else -> false // Root Level 1: Let Home handle back
        }
    }

    LaunchedEffect(handleNotesBack) {
        onRegisterBackAction?.invoke(handleNotesBack)
    }

    // Handle back button internally when inside nested levels/inputs; if embedded in pager, let Home handle root back
    val canPopInternal = isAddModeActive || (isSearchActive && searchQuery.isNotEmpty()) || selectedTopicId != null || selectedCategoryId != null || isUltraPrivateMode
    BackHandler(enabled = !isEmbeddedInPager || canPopInternal) {
        if (!handleNotesBack()) {
            onDismiss()
        }
    }

    // Active Category and Topic objects
    val effectiveCategories = if (isUltraPrivateMode) hiddenCategories else noteCategories
    val currentCategory = remember(effectiveCategories, selectedCategoryId) {
        effectiveCategories.firstOrNull { it.id == selectedCategoryId }
    }
    val currentTopic = remember(currentCategory, selectedTopicId) {
        currentCategory?.topics?.firstOrNull { it.id == selectedTopicId }
    }

    // Filtered categories for active tab (Level 1)
    val categoriesForActiveTab = remember(effectiveCategories, activeSectionTab, isUltraPrivateMode) {
        if (isUltraPrivateMode) {
            hiddenCategories
        } else {
            effectiveCategories.filter { it.section.equals(activeSectionTab, ignoreCase = true) }
        }
    }

    // Global Search Matches
    val searchResults = remember(noteCategories, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            val list = mutableListOf<NoteSearchResult>()
            noteCategories.forEach { cat ->
                if (cat.section.equals("PRIVATE", ignoreCase = true) && (!isPrivateUnlocked || activeSectionTab != "PRIVATE")) {
                    return@forEach
                }
                // If category name matches
                if (cat.name.lowercase().contains(q)) {
                    list.add(
                        NoteSearchResult(
                            categoryId = cat.id,
                            topicId = null,
                            title = cat.name,
                            subtitle = "${cat.section} > Category"
                        )
                    )
                }
                // Check topics
                cat.topics.forEach { top ->
                    if (top.title.lowercase().contains(q) || top.content.lowercase().contains(q)) {
                        list.add(
                            NoteSearchResult(
                                categoryId = cat.id,
                                topicId = top.id,
                                title = top.title,
                                subtitle = "${cat.section} > ${cat.name}"
                            )
                        )
                    }
                }
            }
            list.distinctBy { "${it.categoryId}-${it.topicId}-${it.title}" }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isEmbeddedInPager) Modifier
                else Modifier
                    .background(Color(0xFF070314))
                    .zIndex(65f)
                    .statusBarsPadding()
            )
    ) {
        if (!isEmbeddedInPager) {
            // Subtle ambient neon purple glow when shown standalone
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
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (selectedTopicId == null) 128.dp else 16.dp), // Leaves space for + New + search/add bar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFullScreenEditor || selectedTopicId == null) {
                if (!isEmbeddedInPager) {
                    TopMotiveQuoteHeader()
                    Spacer(Modifier.height(10.dp))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ── 2. Breadcrumb / Tabs Row ──
            NotesBreadcrumbTabRow(
                activeTab = activeSectionTab,
                categoryName = currentCategory?.name,
                topicName = currentTopic?.title,
                onSelectTab = { tab ->
                    activeSectionTab = tab
                    selectedCategoryId = null
                    selectedTopicId = null
                    isAddModeActive = false
                },
                onHoldPrivate3s = {
                    showHiddenUnlockDialog = true
                },
                onNavigateToRoot = {
                    // Clicking [NOTES] pill opens bar + keyboard with "enter the name" + "Okay" to create a new category!
                    addTargetType = "CATEGORY"
                    addInputText = ""
                    isAddModeActive = true
                },
                onNavigateToCategoryList = {
                    selectedCategoryId = null
                    selectedTopicId = null
                    isAddModeActive = false
                },
                onNavigateToTopicList = {
                    selectedTopicId = null
                    isAddModeActive = false
                }
            )

            Spacer(Modifier.height(8.dp))

            // Thin hairline purple divider matching uploaded Notes UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF7C3AED).copy(alpha = 0.7f),
                                Color(0xFF7C3AED).copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(Modifier.height(16.dp))

            // ── 3. Main Content Area ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    // ── Search Results Active ──
                    searchQuery.isNotBlank() -> {
                        SearchResultsGrid(
                            results = searchResults,
                            onSelectResult = { res ->
                                selectedCategoryId = res.categoryId
                                selectedTopicId = res.topicId
                                searchQuery = ""
                                isSearchActive = false
                            }
                        )
                    }

                    // ── Level 3: Note Editor (Image 3) ──
                    currentCategory != null && currentTopic != null -> {
                        NoteEditorView(
                            category = currentCategory,
                            topic = currentTopic,
                            isFullScreen = isFullScreenEditor,
                            onToggleFullScreen = { isFullScreenEditor = !isFullScreenEditor },
                            onUpdateContent = { text, format ->
                                if (isUltraPrivateMode) {
                                    HiddenNotesManager.updateTopic(context, currentCategory.id, currentTopic.id, text, format)
                                    hiddenCategories = HiddenNotesManager.loadCategories(context)
                                } else {
                                    onUpdateTopic(currentCategory.id, currentTopic.id, text, format)
                                }
                            }
                        )
                    }

                    // ── Level 2: Inside Category - Topics Grid (Image 2) ──
                    currentCategory != null -> {
                        TopicsGridView(
                            topics = currentCategory.topics,
                            onSelectTopic = { topic ->
                                selectedTopicId = topic.id
                            },
                            onDeleteTopic = { topicId ->
                                if (isUltraPrivateMode) {
                                    HiddenNotesManager.deleteTopic(context, currentCategory.id, topicId)
                                    hiddenCategories = HiddenNotesManager.loadCategories(context)
                                } else {
                                    onDeleteTopic(currentCategory.id, topicId)
                                }
                            },
                            onLongClickTopic = { top ->
                                actionItemType = "TOPIC"
                                actionItemId = top.id
                                actionItemParentId = currentCategory.id
                                actionItemCurrentName = top.title
                                showActionDialog = true
                            }
                        )
                    }

                    // ── Level 1: Categories Grid for Active Tab (Image 1) ──
                    else -> {
                        CategoriesGridView(
                            categories = categoriesForActiveTab,
                            onSelectCategory = { cat ->
                                selectedCategoryId = cat.id
                            },
                            onDeleteCategory = { catId ->
                                if (isUltraPrivateMode) {
                                    HiddenNotesManager.deleteCategory(context, catId)
                                    hiddenCategories = HiddenNotesManager.loadCategories(context)
                                } else {
                                    onDeleteCategory(catId)
                                }
                            },
                            onLongClickCategory = { cat ->
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

        // ── 4. Bottom Area: [+ New] + Search Pill or Add Input Bar ("enter the name" + "Okay") ──
        if (selectedTopicId == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isAddModeActive) {
                    LaunchedEffect(isAddModeActive) {
                        addInputFocusRequester.requestFocus()
                        keyboardController?.show()
                    }

                    val submitCurrentInput = {
                        val clean = addInputText.trim()
                        if (clean.isNotBlank()) {
                            if (isUltraPrivateMode) {
                                if (addTargetType == "CATEGORY" || selectedCategoryId == null) {
                                    HiddenNotesManager.addCategory(context, clean)
                                    selectedCategoryId = null
                                } else {
                                    HiddenNotesManager.addTopic(context, selectedCategoryId!!, clean)
                                }
                                hiddenCategories = HiddenNotesManager.loadCategories(context)
                            } else {
                                if (addTargetType == "CATEGORY" || selectedCategoryId == null) {
                                    onAddCategory(activeSectionTab, clean)
                                    selectedCategoryId = null
                                } else {
                                    onAddTopic(selectedCategoryId!!, clean)
                                }
                            }
                        }
                        addInputText = ""
                        isAddModeActive = false
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1E0E3E), Color(0xFF120728))
                                )
                            )
                            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(27.dp))
                            .shadow(elevation = 16.dp, shape = RoundedCornerShape(27.dp), spotColor = Color(0xFFA855F7))
                            .padding(start = 18.dp, end = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = addInputText,
                                onValueChange = { addInputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(addInputFocusRequester),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submitCurrentInput() }),
                                decorationBox = { innerTextField ->
                                    if (addInputText.isEmpty()) {
                                        Text(
                                            text = "enter the name",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.5.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            // Cancel [✕] small icon
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        addInputText = ""
                                        isAddModeActive = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            // "Okay" Confirm Pill Button
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                                        )
                                    )
                                    .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(20.dp))
                                    .clickable { submitCurrentInput() }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Okay",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Glowing (+ New) Capsule Button matching the uploaded image
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2D1259), Color(0xFF1A0938))
                                )
                            )
                            .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                            .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                addTargetType = if (selectedCategoryId != null) "TOPIC" else "CATEGORY"
                                addInputText = ""
                                isAddModeActive = true
                            }
                            .padding(horizontal = 24.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "New",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom Search Pill Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF0F0724).copy(alpha = 0.95f))
                            .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(25.dp))
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(25.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                isSearchActive = true
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "search",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-Press 1-Second Action Dialog (Update or Delete) (Requirement 3a)
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
                                    onUpdateCategory?.invoke(actionItemId, clean)
                                } else {
                                    onUpdateTopicTitle?.invoke(actionItemParentId, actionItemId, clean)
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

        // ── 5. Pattern Unlock Dialog for PRIVATE section ──
        if (showPrivateUnlockDialog) {
            PatternUnlockDialog(
                title = "Private Notes Locked",
                onSuccess = {
                    isPrivateUnlocked = true
                    showPrivateUnlockDialog = false
                    activeSectionTab = "PRIVATE"
                    selectedCategoryId = null
                    selectedTopicId = null
                },
                onOpenSettings = {
                    showPrivateUnlockDialog = false
                },
                onDismiss = {
                    showPrivateUnlockDialog = false
                }
            )
        }

        // ── 6. Hidden Pattern Unlock Dialog for Ultra-Private section (Triggered by 3-second silent hold) ──
        if (showHiddenUnlockDialog) {
            HiddenPatternUnlockDialog(
                onSuccess = {
                    showHiddenUnlockDialog = false
                    isUltraPrivateMode = true
                    activeSectionTab = "HIDDEN"
                    hiddenCategories = HiddenNotesManager.loadCategories(context)
                    selectedCategoryId = null
                    selectedTopicId = null
                    Toast.makeText(context, "Ultra-Private workspace unlocked", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showHiddenUnlockDialog = false
                }
            )
        }
    }
}

private fun submitAddAction(
    text: String,
    categoryId: Long?,
    activeSection: String,
    onAddCategory: (String, String) -> Unit,
    onAddTopic: (Long, String) -> Unit
) {
    val clean = text.trim()
    if (clean.isBlank()) return
    if (categoryId != null) {
        onAddTopic(categoryId, clean)
    } else {
        onAddCategory(activeSection, clean)
    }
}

/**
 * Top Quote Header matching Images 1, 2, 3:
 * "Nobody comes \n , You either change or being struck"
 * With glowing purple on "comes" and [ ◀ Motive ▶ ] tag pill.
 */
@Composable
private fun TopMotiveQuoteHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nobody ",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "comes",
                color = Color(0xFFC084FC),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = ", You either change or being struck",
            color = Color(0xFFCBD5E1),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // Right-aligned [ ◀ Motive ▶ ] category badge (matching Images 1, 2, 3)
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 6.dp)
        ) {
            Text(
                text = "◀ Motive ▶",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Breadcrumb and Tab Row matching Images 1, 2, 3:
 * - At root: [ NOTES ] [ GENERAL ] [ PRIMARY ] [ PRIVATE ]
 * - Inside category: [ NOTES ] [ GENERAL ] > MOTIVE
 * - Inside note editor: [ NOTES ] [ GENERAL ] > MOTIVE > GOOD WORDS
 */
@Composable
private fun NotesBreadcrumbTabRow(
    activeTab: String,
    categoryName: String?,
    topicName: String?,
    onSelectTab: (String) -> Unit,
    onHoldPrivate3s: () -> Unit,
    onNavigateToRoot: () -> Unit,
    onNavigateToCategoryList: () -> Unit,
    onNavigateToTopicList: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // [ NOTES ] pill button (navigates to root/closes notes)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14082D))
                .border(1.2.dp, Color(0xFF6B21A8), RoundedCornerShape(12.dp))
                .clickable { onNavigateToRoot() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NOTES",
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (categoryName == null) {
            // Level 1: Tabs [ GENERAL ] [ PRIMARY ] [ PRIVATE ]
            listOf("GENERAL", "PRIMARY", "PRIVATE").forEach { tab ->
                val isSelected = (activeTab.equals(tab, ignoreCase = true))
                val bgBrush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
                    )
                } else {
                    SolidColor(Color.Transparent)
                }

                val tabCoroutineScope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgBrush)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(12.dp))
                                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7))
                            } else Modifier
                        )
                        .then(
                            if (tab == "PRIVATE") {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            onSelectTab("PRIVATE")
                                        },
                                        onPress = {
                                            val startTime = System.currentTimeMillis()
                                            var reached3Sec = false
                                            val job = tabCoroutineScope.launch {
                                                kotlinx.coroutines.delay(3000L)
                                                reached3Sec = true
                                                onHoldPrivate3s()
                                            }
                                            tryAwaitRelease()
                                            job.cancel()
                                            if (!reached3Sec && (System.currentTimeMillis() - startTime) < 500L) {
                                                onSelectTab("PRIVATE")
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier.clickable { onSelectTab(tab) }
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tab == "PRIVATE") {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private",
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            // Level 2 or 3: Breadcrumb Trail
            // [ ACTIVE_TAB ]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7C3AED))
                    .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToCategoryList() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeTab.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // > Category Name
            Text(
                text = "> $categoryName",
                color = if (topicName == null) Color(0xFFC084FC) else Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onNavigateToTopicList() }
            )

            // > Topic Name (if in Level 3)
            if (topicName != null) {
                Text(
                    text = "> $topicName",
                    color = Color(0xFFE879F9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Level 1: Categories 2-Column Grid (Image 1)
 */
@Composable
private fun CategoriesGridView(
    categories: List<NoteCategory>,
    onSelectCategory: (NoteCategory) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onLongClickCategory: (NoteCategory) -> Unit = {}
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Scroll up from bottom to add category",
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
            items(categories, key = { it.id }) { cat ->
                GlowingNoteCard(
                    title = cat.name,
                    onClick = { onSelectCategory(cat) },
                    onLongClick = { onLongClickCategory(cat) }
                )
            }
        }
    }
}

/**
 * Level 2: Inside Category - Topics 2-Column Grid (Image 2)
 */
@Composable
private fun TopicsGridView(
    topics: List<NoteTopic>,
    onSelectTopic: (NoteTopic) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onLongClickTopic: (NoteTopic) -> Unit = {}
) {
    if (topics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Scroll up from bottom to add topic",
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
            items(topics, key = { it.id }) { topic ->
                GlowingNoteCard(
                    title = topic.title,
                    onClick = { onSelectTopic(topic) },
                    onLongClick = { onLongClickTopic(topic) }
                )
            }
        }
    }
}

/**
 * Glowing Violet Card matching Images 1 & 2:
 * Rounded rectangular / square card with glowing purple border and gradient fill.
 */
@Composable
private fun GlowingNoteCard(
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Level 3: Note Editor View matching Images 2 & 3:
 * - Drag bar handle ━━━ (Requirement 3c: drag up expands full screen, drag down restores)
 * - Format pills: clicking does NOT reformat previously written lines (Requirement 3b)
 * - Freely scrollable text area to the very last line (Requirement 3d)
 */
@Composable
private fun NoteEditorView(
    category: NoteCategory,
    topic: NoteTopic,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onUpdateContent: (content: String, formatType: String) -> Unit
) {
    var formatType by remember(topic.id) { mutableStateOf(topic.formatType.ifEmpty { "POINTS" }) }
    var textFieldValue by remember(topic.id) {
        mutableStateOf(
            TextFieldValue(
                text = topic.content,
                selection = TextRange(topic.content.length)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0724))
            .border(1.6.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0xFFA855F7))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Toolbar (Drag bar handle ━━━ + Format Pills) (Image 2 & 3) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Centered white drag bar handle ━━━ (Requirement 3c)
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
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
                                    if (!isFullScreen) onToggleFullScreen()
                                } else if (totalDrag > 20f) {
                                    change.consume()
                                    if (isFullScreen) onToggleFullScreen()
                                }
                            }
                        }
                        .clickable { onToggleFullScreen() }
                )

                Spacer(Modifier.weight(1f))

                // [ • Points ] - Requirement 3b: does not alter existing lines
                FormatToolbarPill(
                    label = "• Points",
                    isActive = (formatType == "POINTS"),
                    onClick = {
                        formatType = "POINTS"
                        if (textFieldValue.text.isBlank()) {
                            textFieldValue = TextFieldValue(text = "• ", selection = TextRange(2))
                            onUpdateContent("• ", "POINTS")
                        } else {
                            onUpdateContent(textFieldValue.text, "POINTS")
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                // [ 1. Number ] - Requirement 3b: does not alter existing lines
                FormatToolbarPill(
                    label = "1. Number",
                    isActive = (formatType == "NUMBER"),
                    onClick = {
                        formatType = "NUMBER"
                        if (textFieldValue.text.isBlank()) {
                            textFieldValue = TextFieldValue(text = "1. ", selection = TextRange(3))
                            onUpdateContent("1. ", "NUMBER")
                        } else {
                            onUpdateContent(textFieldValue.text, "NUMBER")
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                // [ Normal ] - Requirement 3b: does not alter existing lines
                FormatToolbarPill(
                    label = "Normal",
                    isActive = (formatType == "NORMAL"),
                    onClick = {
                        formatType = "NORMAL"
                        onUpdateContent(textFieldValue.text, "NORMAL")
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Scrollable Text Editor Area (Requirement 3d) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val oldText = textFieldValue.text
                        val newText = newValue.text

                        // Check if a newline was just inserted (user hit Enter)
                        if (newText.length > oldText.length && newText.contains("\n") && !oldText.endsWith("\n")) {
                            val cursor = newValue.selection.start
                            if (cursor > 0 && newText.getOrNull(cursor - 1) == '\n') {
                                when (formatType) {
                                    "POINTS" -> {
                                        val prefix = "• "
                                        val textBefore = newText.substring(0, cursor)
                                        val textAfter = newText.substring(cursor)
                                        val formattedText = textBefore + prefix + textAfter
                                        val newCursor = cursor + prefix.length
                                        textFieldValue = TextFieldValue(formattedText, selection = TextRange(newCursor))
                                        onUpdateContent(formattedText, formatType)
                                        return@BasicTextField
                                    }
                                    "NUMBER" -> {
                                        val textBefore = newText.substring(0, cursor)
                                        val linesBefore = textBefore.lines()
                                        val lastLine = linesBefore.getOrNull(linesBefore.size - 2) ?: ""
                                        val match = Regex("^(\\s*)(\\d+)\\.").find(lastLine)
                                        val nextNum = if (match != null) (match.groupValues[2].toIntOrNull() ?: 1) + 1 else 1
                                        val prefix = "$nextNum. "
                                        val textAfter = newText.substring(cursor)
                                        val formattedText = textBefore + prefix + textAfter
                                        val newCursor = cursor + prefix.length
                                        textFieldValue = TextFieldValue(formattedText, selection = TextRange(newCursor))
                                        onUpdateContent(formattedText, formatType)
                                        return@BasicTextField
                                    }
                                }
                            }
                        }

                        textFieldValue = newValue
                        onUpdateContent(newText, formatType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 260.dp),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color(0xFFA855F7)),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Start typing here...",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun FormatToolbarPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color(0xFF7C3AED) else Color(0xFF14082D))
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFFE879F9) else Color(0xFF4C1D95),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Search Results Grid view across all notes
 */
@Composable
private fun SearchResultsGrid(
    results: List<NoteSearchResult>,
    onSelectResult: (NoteSearchResult) -> Unit
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No matching notes found",
                color = Color(0xFF94A3B8),
                fontSize = 14.5.sp
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { "${it.categoryId}-${it.topicId}-${it.title}" }) { res ->
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
                        .clickable { onSelectResult(res) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = res.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = res.subtitle,
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
