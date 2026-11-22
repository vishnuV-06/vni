package com.vni.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Motive
import com.vni.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Top motive section — cycles through [motives] every 8 seconds
 * with a slide-bottom-to-top animation. Corner bracket decorations included.
 * Clicking opens the full Motive management page!
 */
@Composable
fun MotiveSection(
    motives: List<Motive>,
    onMotiveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance every 8 seconds
    LaunchedEffect(motives) {
        if (motives.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            currentIndex = (currentIndex + 1) % motives.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onMotiveClick() }
            .drawBehind { drawBrackets(this) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                slideInVertically(
                    animationSpec = tween(600, easing = EaseOutCubic),
                    initialOffsetY = { it }
                ) togetherWith slideOutVertically(
                    animationSpec = tween(600, easing = EaseInCubic),
                    targetOffsetY = { -it }
                )
            },
            label = "motive_anim"
        ) { idx ->
            val motive = motives.getOrNull(idx) ?: return@AnimatedContent
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Build annotated string with purple highlights
                val highlighted = motive.highlightWords.split(",").map { it.trim() }
                val parts = motive.text.split(",", limit = 2)
                val mainPhrase = parts[0].trim()
                val subtitle = if (parts.size > 1) ",${parts[1]}" else ""

                val mainWords = mainPhrase.split(" ")
                val annotated = buildAnnotatedString {
                    mainWords.forEachIndexed { i, word ->
                        val clean = word.trim().trimEnd('.', '!', ',')
                        if (highlighted.any { it.equals(clean, ignoreCase = true) }) {
                            withStyle(SpanStyle(color = PurpleLight, fontWeight = FontWeight.ExtraBold)) {
                                append(word)
                            }
                        } else {
                            withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        }
                        if (i < mainWords.lastIndex) append(" ")
                    }
                }

                Text(
                    text = annotated,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = TextGrey,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Draws the corner bracket decoration exactly as in the reference image */
private fun drawBrackets(scope: androidx.compose.ui.graphics.drawscope.DrawScope) {
    with(scope) {
        val stroke = 2.dp.toPx()
        val len = 20.dp.toPx()
        val pad = 8.dp.toPx()
        val color = Color(0xFF4A4A8A)

        // Top-left bracket
        drawLine(color, Offset(pad, pad), Offset(pad + len, pad), strokeWidth = stroke)
        drawLine(color, Offset(pad, pad), Offset(pad, pad + len), strokeWidth = stroke)
        // Bottom-right bracket
        val r = size.width - pad
        val b = size.height - pad
        drawLine(color, Offset(r - len, b), Offset(r, b), strokeWidth = stroke)
        drawLine(color, Offset(r, b - len), Offset(r, b), strokeWidth = stroke)
    }
}

private val EaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
private val EaseInCubic  = CubicBezierEasing(0.55f, 0.055f, 0.675f, 0.19f)
