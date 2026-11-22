package com.vni.app.ui.home.v2

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.MotiveHeading
import com.vni.app.data.model.NoteCategory
import com.vni.app.data.model.NoteTopic
import com.vni.app.ui.security.PatternUnlockDialog
import com.vni.app.ui.security.SecurityPatternManager

/**
 * Do Section V2 (Requirements 3 & 4):
 * - 2-Layered Architecture:
 *   - Layer 1: Headings Grid (General & Private tabs), sleek 58.dp height cards.
 *   - Layer 2: Points List inside a heading (straight horizontal blocks).
 * - Private section protected by Pattern Lock.
 * - Straight point blocks: holding for 1 second marks it complete, moves to History, and deletes it from Do.
 * - Input bar at bottom allows adding new headings or new points inside a heading.
 */
@Composable
fun DoV2Screen(
    doCategories: List<NoteCategory>,
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddCategory: (name: String, section: String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onAddPoint: (categoryId: Long, point: String) -> Unit,
    onDeletePoint: (categoryId: Long, topicId: Long, markCompletedInHistory: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var activeSectionTab by remember { mutableStateOf("GENERAL") } // "GENERAL" or "PRIVATE"
    var isPrivateUnlocked by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isAddModeActive by remember { mutableStateOf(false) }
    var addInputText by remember { mutableStateOf("") }

    var categoryToDelete by remember { mutableStateOf<NoteCategory?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val addInputFocusRequester = remember { FocusRequester() }

    BackHandler {
        when {
            isAddModeActive -> {
                isAddModeActive = false
                addInputText = ""
            }
            searchQuery.isNotEmpty() -> {
                searchQuery = ""
            }
            selectedCategoryId != null -> {
                selectedCategoryId = null
            }
            else -> onDismiss()
        }
    }

    val currentCategory = remember(doCategories, selectedCategoryId) {
        doCategories.firstOrNull { it.id == selectedCategoryId }
    }

    // Filter categories based on active tab
    val filteredCategories = remember(doCategories, activeSectionTab) {
        doCategories.filter { it.section.equals(activeSectionTab, ignoreCase = true) }
    }

    // Live search results
    val searchResults = remember(doCategories, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            val list = mutableListOf<DoSearchResult>()
            doCategories.forEach { cat ->
                if (cat.name.lowercase().contains(q)) {
                    list.add(DoSearchResult(cat.id, null, cat.name, "${cat.section} > Heading"))
                }
                cat.topics.forEach { top ->
                    if (top.title.lowercase().contains(q)) {
                        list.add(DoSearchResult(cat.id, top.id, top.title, "${cat.section} > ${cat.name}"))
                    }
                }
            }
            list.distinctBy { "${it.categoryId}-${it.topicId}-${it.title}" }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            Spacer(Modifier.height(6.dp))

            // Breadcrumb & Tabs Row: [ DO ] [ GENERAL ] [ PRIVATE ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // [ DO ] Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7))))
                        .border(1.2.dp, Color(0xFFE879F9), RoundedCornerShape(12.dp))
                        .clickable {
                            selectedCategoryId = null
                            isAddModeActive = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DO",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (selectedCategoryId == null) {
                    // Tab 1: GENERAL
                    val isGenSel = (activeSectionTab == "GENERAL")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGenSel) Color(0xFF6B21A8) else Color(0xFF160930))
                            .border(1.dp, if (isGenSel) Color(0xFFA855F7) else Color(0xFF3B1668), RoundedCornerShape(12.dp))
                            .clickable { activeSectionTab = "GENERAL" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GENERAL",
                            color = if (isGenSel) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tab 2: PRIVATE (Protected by Pattern)
                    val isPrivSel = (activeSectionTab == "PRIVATE")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPrivSel) Color(0xFF6B21A8) else Color(0xFF160930))
                            .border(1.dp, if (isPrivSel) Color(0xFFA855F7) else Color(0xFF3B1668), RoundedCornerShape(12.dp))
                            .clickable {
                                if (isPrivateUnlocked) {
                                    activeSectionTab = "PRIVATE"
                                } else {
                                    if (SecurityPatternManager.isPatternSet(context)) {
                                        showPatternDialog = true
                                    } else {
                                        Toast.makeText(context, "Please set a Pattern in Settings first", Toast.LENGTH_LONG).show()
                                        onOpenSettings()
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Lock, "Private", tint = if (isPrivSel) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            Text(
                                text = "PRIVATE",
                                color = if (isPrivSel) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (currentCategory != null) {
                    Text(
                        text = "> ${currentCategory.name}",
                        color = Color(0xFFE879F9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Thin hairline purple divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF7C3AED).copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(14.dp))

            // Content Area (Layer 1: Headings Grid | Layer 2: Straight Points List)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    searchQuery.isNotBlank() && selectedCategoryId == null -> {
                        // Search Results List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults) { res ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF14082D))
                                        .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedCategoryId = res.categoryId
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Column {
                                        Text(res.title, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                        Text(res.subtitle, color = Color(0xFFA855F7), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Layer 2: Inside a Heading -> Shows Straight Horizontal Points Blocks
                    currentCategory != null -> {
                        if (currentCategory.topics.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No points added yet.\nType below to add points to ${currentCategory.name}.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = "Tip: Hold any block for 1s to mark as completed",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                items(currentCategory.topics, key = { it.id }) { point ->
                                    DoStraightPointBlock(
                                        text = point.title,
                                        onHoldToComplete = {
                                            onDeletePoint(currentCategory.id, point.id, true)
                                            Toast.makeText(context, "Completed! Moved to History", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Layer 1: Headings Grid (Sleek 58.dp height)
                    else -> {
                        if (filteredCategories.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tap + New below to create a $activeSectionTab heading",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.5.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredCategories, key = { it.id }) { cat ->
                                    DoSleekHeadingCard(
                                        title = cat.name,
                                        onClick = { selectedCategoryId = cat.id },
                                        onLongPress = { categoryToDelete = cat }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls:
        // - In Layer 1: [+ New] + Search bar
        // - In Layer 2: Direct point input bar [ enter point ] [ Okay ]
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedCategoryId != null) {
                // Layer 2 Input: Add point to heading
                val submitPointAction = {
                    val clean = addInputText.trim()
                    if (clean.isNotBlank()) {
                        onAddPoint(selectedCategoryId!!, clean)
                    }
                    addInputText = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1E0E3E), Color(0xFF120728))))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
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
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFA855F7)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitPointAction() }),
                            decorationBox = { innerTextField ->
                                if (addInputText.isEmpty()) {
                                    Text("enter point to add...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )

                        if (addInputText.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp).clickable { addInputText = "" }
                            )
                            Spacer(Modifier.width(6.dp))
                        }

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))))
                                .clickable { submitPointAction() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isAddModeActive) {
                // Layer 1 Input: Add new heading
                LaunchedEffect(isAddModeActive) {
                    addInputFocusRequester.requestFocus()
                    keyboardController?.show()
                }

                val submitHeadingAction = {
                    val clean = addInputText.trim()
                    if (clean.isNotBlank()) {
                        onAddCategory(clean, activeSectionTab)
                    }
                    addInputText = ""
                    isAddModeActive = false
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1E0E3E), Color(0xFF120728))))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
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
                            modifier = Modifier.weight(1f).focusRequester(addInputFocusRequester),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFA855F7)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitHeadingAction() }),
                            decorationBox = { innerTextField ->
                                if (addInputText.isEmpty()) {
                                    Text("enter $activeSectionTab heading name", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )

                        Icon(
                            Icons.Default.Close,
                            "Cancel",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp).clickable {
                                addInputText = ""
                                isAddModeActive = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )

                        Spacer(Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))))
                                .clickable { submitHeadingAction() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Okay", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Layer 1 Normal State: [+ New] button & [ search ] bar
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2D1259), Color(0xFF1A0938))))
                        .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0xFFA855F7))
                        .clickable {
                            addInputText = ""
                            isAddModeActive = true
                        }
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Add, "New", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("New", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F0724).copy(alpha = 0.95f))
                        .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(24.dp))
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0xFFA855F7))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, "Search", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFA855F7)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) Text("search headings or points", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Default.Close, "Clear", tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp).clickable { searchQuery = "" })
                        }
                    }
                }
            }
        }

        // Pattern Unlock Dialog for PRIVATE section
        if (showPatternDialog) {
            PatternUnlockDialog(
                title = "Private Do Locked",
                onSuccess = {
                    isPrivateUnlocked = true
                    activeSectionTab = "PRIVATE"
                    showPatternDialog = false
                },
                onOpenSettings = {
                    showPatternDialog = false
                    onOpenSettings()
                },
                onDismiss = { showPatternDialog = false }
            )
        }

        // Delete Heading Confirmation Dialog
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
                        Text("Delete Heading?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Delete \"${categoryToDelete?.name}\" and all points inside?", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { categoryToDelete = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDeleteCategory(categoryToDelete!!.id)
                                    categoryToDelete = null
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
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
    }
}

/**
 * Sleek 58.dp Heading Card for DO Section (Requirement 3: 40% height reduction).
 */
@Composable
private fun DoSleekHeadingCard(
    title: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFFA855F7))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0B3B), Color(0xFF280D58), Color(0xFF3E1478))
                )
            )
            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFFE879F9))), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Straight Horizontal Point Block for DO Section (Requirement 4).
 * Holding the block for 1 second deletes it and moves it to History as completed.
 */
@Composable
private fun DoStraightPointBlock(
    text: String,
    onHoldToComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14082D))
            .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(14.dp))
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFF7C3AED))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onHoldToComplete() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA855F7))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = "Hold to complete",
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private data class DoSearchResult(
    val categoryId: Long,
    val topicId: Long?,
    val title: String,
    val subtitle: String
)
