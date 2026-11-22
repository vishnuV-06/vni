package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Suggestion
import com.vni.app.ui.theme.PurplePrimary
import com.vni.app.ui.theme.TextMuted
import com.vni.app.ui.theme.TextWhite

@Composable
fun SuggestionsBottomPanel(
    suggestions: List<Suggestion>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF0C071E))
            .border(1.dp, PurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(20.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SUGGESTIONS FOR YOU", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF130B30), CircleShape)
                    .border(1.dp, PurplePrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Items
        suggestions.forEachIndexed { index, suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Purple vertical line indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(PurplePrimary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(16.dp))
                
                val annotatedText = buildAnnotatedString {
                    val parts = suggestion.text.split(suggestion.highlightText, ignoreCase = true)
                    if (parts.size == 2) {
                        append(parts[0])
                        withStyle(style = SpanStyle(color = PurplePrimary, fontWeight = FontWeight.Bold)) {
                            append(suggestion.text.substring(parts[0].length, parts[0].length + suggestion.highlightText.length))
                        }
                        append(parts[1])
                    } else {
                        append(suggestion.text)
                    }
                }
                
                Text(annotatedText, color = TextWhite, fontSize = 14.sp)
            }
            if (index < suggestions.size - 1) {
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 20.dp))
            }
        }
    }
}
