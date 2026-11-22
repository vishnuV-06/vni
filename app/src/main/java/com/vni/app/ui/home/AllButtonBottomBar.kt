package com.vni.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.ui.theme.PurplePrimary
import com.vni.app.ui.theme.TextMuted
import com.vni.app.ui.theme.TextWhite
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SectionNavigationBottomBar(
    currentSection: String,
    onSectionSelected: (String) -> Unit,
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWheelMenu by remember { mutableStateOf(false) }
    
    val sections = listOf("WORK", "LEARN", "MONEY", "STUDY", "ALL")
    val initialIndex = sections.indexOf(currentSection).coerceAtLeast(0)
    
    // We will use a vertical drag to change an offset
    var scrollOffset by remember { mutableFloatStateOf(initialIndex * 60f) }
    val itemHeight = 60f

    // Update selectedIndex based on scroll
    val currentIndex = (scrollOffset / itemHeight).toInt().coerceIn(0, sections.size - 1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp) // Aligned some more top
            .wrapContentHeight(), // Remove 280dp fixed height to eliminate empty space
        contentAlignment = Alignment.BottomCenter
    ) {
        
        // The floating curved words behind/above the main button
        if (showWheelMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 28.dp), // offset to align wheel center with the button center
                contentAlignment = Alignment.BottomCenter
            ) {
                sections.forEachIndexed { index, sectionText ->
                    val distance = index * itemHeight - scrollOffset
                    val maxDist = 200f
                    if (abs(distance) < maxDist) {
                        val curveAmount = (distance / maxDist) * (distance / maxDist)
                        val offsetX = curveAmount * 120f
                        val rotation = (distance / maxDist) * -30f
                        val alpha = 1f - (abs(distance) / maxDist)
                        val scale = 1f - (abs(distance) / maxDist) * 0.4f
                        
                        if (index != currentIndex) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationY = distance
                                        translationX = offsetX
                                        rotationZ = rotation
                                        this.alpha = alpha.coerceIn(0f, 1f)
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sectionText,
                                    color = TextWhite.copy(alpha = 0.5f),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // The Bottom Bar row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Text depending on section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.width(32.dp).height(2.dp).background(Color.White))
                Spacer(Modifier.height(8.dp))
                if (currentSection == "ALL") {
                    Text("CLEAR\nCOMPLETED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                } else {
                    Text("DRAG UP\nTO CHANGE\nSECTION", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                }
            }

            // Center: The dynamic button
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(56.dp)
                    .background(Color(0xFF0C071E), RoundedCornerShape(16.dp))
                    .border(2.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { showWheelMenu = true },
                            onDragEnd = {
                                val targetIndex = (scrollOffset / itemHeight).toInt().coerceIn(0, sections.size - 1)
                                scrollOffset = targetIndex * itemHeight
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val multiplier = if (dragAmount < 0) 0.4f else 1.5f
                                scrollOffset = (scrollOffset + dragAmount * multiplier).coerceIn(0f, (sections.size - 1) * itemHeight)
                            }
                        )
                    }
                    .clickable { 
                        if (showWheelMenu) {
                            showWheelMenu = false
                            onSectionSelected(sections[currentIndex])
                        } else {
                            showWheelMenu = true 
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sections[currentIndex], color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    if (sections[currentIndex] == "ALL" && !showWheelMenu) {
                        Text("VIEW ALL TASKS", color = TextMuted, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
            }

            // Right: + ADD
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color.White))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, PurplePrimary, CircleShape)
                            .clickable { onAddClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (currentSection == "ALL") {
                    Text("ADD", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("ADD\nNEW ENTRY", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                }
            }
        }
    }
}
