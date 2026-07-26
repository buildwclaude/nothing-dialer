package com.buildwclaude.dialer.ui.contacts

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.buildwclaude.dialer.core.ui.theme.palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * HUD edge scrubber — "1b magnified wheel" from the HUD Edge Volume Controller
 * design project (hud-edge-scrubber-spec.md).
 *
 * A short fixed strip on the right edge, vertically centred. Dragging inside it
 * scrubs all 26 letters by *relative* drag distance (like a picker wheel), the
 * letters magnify around the current fractional index, and a fixed HUD capsule
 * reads out the letter. Steps tick haptically and jump the list instantly.
 *
 * Geometry is per spec (44×180 zone, 30dp rows, 64×44 capsule at 36dp inset);
 * colours are adapted to this app's monochrome dark palette rather than the
 * design's Material 3 baseline.
 */
private const val ROW_DP = 30f
private const val FALLOFF_ROWS = 3.2f
private const val AUTO_HIDE_MS = 420L

private val EnterEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)

@Composable
fun BoxScope.EdgeAlphabetWheel(
    letters: List<Char>,
    counts: Map<Char, Int>,
    listState: LazyListState,
    sectionIndex: (Char) -> Int?,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return

    // Fractional index persists between engagements — like a real wheel it does
    // not reset to A each time.
    var frac by rememberSaveable { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var lastRounded by remember { mutableStateOf<Char?>(null) }
    var hudVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val rowPx = with(density) { ROW_DP.dp.toPx() }
    val pop = remember { Animatable(1f) }

    // Auto-hide the HUD after release.
    LaunchedEffect(dragging) {
        if (dragging) {
            hudVisible = true
        } else {
            kotlinx.coroutines.delay(AUTO_HIDE_MS)
            hudVisible = false
        }
    }

    /** Nearest letter that actually has contacts (empty letters are skipped). */
    fun nearestPopulated(index: Int): Char? {
        if (letters.isEmpty()) return null
        val i = index.coerceIn(0, letters.lastIndex)
        if ((counts[letters[i]] ?: 0) > 0) return letters[i]
        for (step in 1..letters.size) {
            (i - step).takeIf { it >= 0 }?.let { if ((counts[letters[it]] ?: 0) > 0) return letters[it] }
            (i + step).takeIf { it <= letters.lastIndex }?.let { if ((counts[letters[it]] ?: 0) > 0) return letters[it] }
        }
        return null
    }

    fun onStep(rounded: Char) {
        lastRounded = rounded
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        scope.launch { pop.animateTo(1.12f, tween(70)); pop.animateTo(1f, tween(70)) }
        sectionIndex(rounded)?.let { idx ->
            scope.launch { listState.scrollToItem(idx) } // instant, no easing (real index behaviour)
        }
    }

    val currentLetter = nearestPopulated(frac.roundToInt()) ?: letters.first()

    // The HUD capsule: fixed at the zone's centre, 36dp in from the right edge.
    AnimatedVisibility(
        visible = hudVisible,
        enter = fadeIn(tween(180, easing = EnterEasing)) +
            scaleIn(tween(180, easing = EnterEasing), initialScale = 0.84f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.84f),
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 36.dp),
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(44.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(22.dp))
                .background(palette.KeyBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                currentLetter.toString(),
                color = palette.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    // The rail: a short fixed strip, always faintly visible, drag to scrub.
    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .width(44.dp)
            .height(180.dp)
            .pointerInput(letters) {
                var startY = 0f
                var startFrac = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        startFrac = frac
                        dragging = true
                        lastRounded = null
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    frac = (startFrac + (change.position.y - startY) / rowPx)
                        .coerceIn(0f, letters.lastIndex.toFloat())
                    val rounded = nearestPopulated(frac.roundToInt())
                    if (rounded != null && rounded != lastRounded) onStep(rounded)
                }
            },
    ) {
        letters.forEachIndexed { i, letter ->
            val distance = abs(i - frac)
            val f = (1f - distance / FALLOFF_ROWS).coerceAtLeast(0f)
            val empty = (counts[letter] ?: 0) == 0
            val isCurrent = letter == currentLetter && dragging
            Text(
                text = letter.toString(),
                color = if (isCurrent) palette.TextPrimary else palette.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04f.em,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 9.dp)
                    .graphicsLayer {
                        translationY = (i - frac) * rowPx
                        val s = 1f + f * 0.9f
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        alpha = when {
                            empty -> 0.3f + 0.2f * f
                            dragging -> 0.4f + 0.6f * f
                            else -> 0.4f
                        }
                    },
            )
        }
    }
}
