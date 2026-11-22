package com.vni.app.ui.home.v2

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.MotiveHeading

/**
 * Categories / Sentences Section V2 (Matches Notes V2 UI):
 * - Top HomeTopMotiveBar (displays sentences from these headings live with the heading below!)
 * - [ CATEGORIES ] pill + breadcrumb (> Heading)
 * - 2-column glowing violet card grids for Headings (e.g. MOTIVE, GOOD WORDS, MINDSET)
 * - Clicking [ CATEGORIES ] or [+ New] opens bottom input bar ("enter the name" + "Okay") to add a new heading
 * - Clicking any Heading card opens the Point/Sentence Editor (• Points | 1. Number | Normal)
 * - Each line / point added here appears in the app's Top Motive Bar with its heading below it
 * - Long-pressing any Heading card for 1 sec prompts to delete it
 * - Live Search across all headings and sentences
 */
@Composable
fun SentenceV2Screen(
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddHeading: (String) -> Unit,
    onDeleteHeading: (Long) -> Unit,
    onUpdateHeadingSentences: (headingId: Long, rawContent: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedHeadingId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isAddModeActive by remember { mutableStateOf(false) }
    var addInputText by remember { mutableStateOf("") }
    var headingToDelete by remember { mutableStateOf<MotiveHeading?>(null) }

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
            selectedHeadingId != null -> {
                selectedHeadingId = null
            }
            else -> onDismiss()
        }
    }

    val currentHeading = remember(motiveHeadings, selectedHeadingId) {
        motiveHeadings.firstOrNull { it.id == selectedHeadingId }
    }

    val searchResults = remember(motiveHeadings, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            motiveHeadings.filter { mh ->
                mh.heading.lowercase().contains(q) ||
                    mh.sentences.any { it.lowercase().contains(q) }
            }
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
                .padding(bottom = if (selectedHeadingId == null) 128.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            Spacer(Modifier.height(6.dp))

            // Breadcrumb Row: [ CATEGORIES ] > Heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7)))
                        )
                        .border(1.2.dp, Color(0xFFE879F9), RoundedCornerShape(12.dp))
                        .clickable {
                            if (selectedHeadingId != null) {
                                selectedHeadingId = null
                            } else {
                                addInputText = ""
                                isAddModeActive = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CATEGORIES",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (currentHeading != null) {
                    Text(
                        text = "> ${currentHeading.heading}",
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

            Spacer(Modifier.height(16.dp))

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    searchQuery.isNotBlank() && selectedHeadingId == null -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults, key = { it.id }) { mh ->
                                SentenceGlowingCard(
                                    title = mh.heading,
                                    onClick = {
                                        selectedHeadingId = mh.id
                                        searchQuery = ""
                                    },
                                    onLongPress = { headingToDelete = mh }
                                )
                            }
                        }
                    }

                    currentHeading != null -> {
                        SentencePointsEditorView(
                            heading = currentHeading,
                            onUpdateContent = { rawContent ->
                                onUpdateHeadingSentences(currentHeading.id, rawContent)
                            }
                        )
                    }

                    else -> {
                        if (motiveHeadings.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Tap CATEGORIES or + New to add a heading",
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
                                items(motiveHeadings, key = { it.id }) { mh ->
                                    SentenceGlowingCard(
                                        title = mh.heading,
                                        onClick = { selectedHeadingId = mh.id },
                                        onLongPress = { headingToDelete = mh }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom [+ New] + Search / Add Bar (shown on Root Grid)
        if (selectedHeadingId == null) {
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

                    val submitAction = {
                        val clean = addInputText.trim()
                        if (clean.isNotBlank()) {
                            onAddHeading(clean)
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
                            .background(Brush.horizontalGradient(listOf(Color(0xFF1E0E3E), Color(0xFF120728))))
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
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submitAction() }),
                                decorationBox = { innerTextField ->
                                    if (addInputText.isEmpty()) {
                                        Text("enter the name", color = Color(0xFF94A3B8), fontSize = 14.5.sp)
                                    }
                                    innerTextField()
                                }
                            )

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
                                Icon(Icons.Default.Close, "Cancel", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }

                            Spacer(Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))))
                                    .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(20.dp))
                                    .clickable { submitAction() }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Okay", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF2D1259), Color(0xFF1A0938))))
                            .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                            .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                addInputText = ""
                                isAddModeActive = true
                            }
                            .padding(horizontal = 24.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Add, "New", tint = Color.White, modifier = Modifier.size(17.dp))
                            Text("New", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF0F0724).copy(alpha = 0.95f))
                            .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(25.dp))
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(25.dp), spotColor = Color(0xFFA855F7))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, "Search", tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("search", color = Color(0xFF94A3B8), fontSize = 15.sp)
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
        }

        // Delete Heading Confirmation
        if (headingToDelete != null) {
            Dialog(onDismissRequest = { headingToDelete = null }) {
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
                        Text("Delete \"${headingToDelete?.heading}\" and all sentences inside?", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { headingToDelete = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDeleteHeading(headingToDelete!!.id)
                                    headingToDelete = null
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

@Composable
private fun SentenceGlowingCard(
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SentencePointsEditorView(
    heading: MotiveHeading,
    onUpdateContent: (rawContent: String) -> Unit
) {
    val initialText = remember(heading.id) {
        if (heading.sentences.isEmpty()) "• "
        else heading.sentences.joinToString("\n") { "• $it" }
    }
    var textFieldValue by remember(heading.id) {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0419))
            .border(1.4.dp, Color(0xFF7C3AED), RoundedCornerShape(24.dp))
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0xFF7C3AED))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Formatting Header: clean points indicator (number/normal options removed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tip: Title : Subtitle",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7C3AED))
                        .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("• Points", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val newText = newValue.text
                    if (newText.length == oldText.length + 1 && newValue.selection.start > 0 && newText[newValue.selection.start - 1] == '\n') {
                        val cursorPos = newValue.selection.start
                        val beforeNewline = newText.substring(0, cursorPos - 1)
                        val afterNewline = newText.substring(cursorPos)
                        val prefixToAdd = "• "
                        val updatedText = beforeNewline + "\n" + prefixToAdd + afterNewline
                        val updatedCursor = cursorPos + prefixToAdd.length
                        textFieldValue = TextFieldValue(text = updatedText, selection = TextRange(updatedCursor))
                        onUpdateContent(updatedText)
                        return@BasicTextField
                    }
                    textFieldValue = newValue
                    onUpdateContent(newValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Color(0xFFA855F7)),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text("Write each point on a new line...", color = Color(0xFF64748B), fontSize = 14.5.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}
