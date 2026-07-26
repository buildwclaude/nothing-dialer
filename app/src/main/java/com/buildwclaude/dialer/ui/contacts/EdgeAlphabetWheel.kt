package com.buildwclaude.dialer.ui.contacts

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * HUD edge scrubber — direction **1b "Magnified wheel"** from the
 * "HUD Edge Volume Controller" design project (hud-edge-scrubber-spec.md).
 *
 * A short fixed strip on the right edge, vertically centred, with the letter
 * track **clipped to the zone** so only a handful of letters show at once.
 * Dragging scrubs all 26 letters by relative distance (a picker wheel), letters
 * magnify around the fractional index, and a fixed capsule reads out the letter.
 *
 * Spec geometry: zone 44×180 (top 50%−90) · row 30dp · letter 9sp/600/+0.04em,
 * right-aligned at 9dp inset · idle opacity .4 populated / .3 empty · capsule
 * 64×44 r22 surface, elevation 3, letter 20sp/600, fixed at 36dp right inset.
 * Motion: in 180ms (.2,.9,.3,1), out 140ms, step pop →1.12 over 140ms,
 * auto-hide 420ms, list jump instant.
 */
private const val ROW_DP = 30f
private const val FALLOFF_ROWS = 3.2f
private const val AUTO_HIDE_MS = 420L
private const val ZONE_W_DP = 44
private const val ZONE_H_DP = 180

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

    // HUD shows while scrubbing, then auto-hides after the spec's delay.
    LaunchedEffect(dragging) {
        if (dragging) {
            hudVisible = true
        } else {
            delay(AUTO_HIDE_MS)
            hudVisible = false
        }
    }

    // Per the 1b prototype the readout follows round(frac) directly; the list
    // only jumps when that letter actually has a section.
    val rounded = letters[frac.roundToInt().coerceIn(0, letters.lastIndex)]

    fun onStep(letter: Char) {
        lastRounded = letter
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        scope.launch {
            pop.animateTo(1.12f, tween(70))
            pop.animateTo(1f, tween(70))
        }
        sectionIndex(letter)?.let { idx ->
            scope.launch { listState.scrollToItem(idx) } // instant, like a real index
        }
    }

    // ---- HUD capsule: fixed at the zone's centre, 36dp in from the edge ----
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
                .shadow(3.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                // Spec: `surface` fill at elevation 3 — KeyBg is our elevated
                // surface token, so it reads correctly in light and dark.
                .background(palette.KeyBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                rounded.toString(),
                color = palette.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    // ---- The rail: fixed 44×180 window, letter track clipped inside it ----
    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .width(ZONE_W_DP.dp)
            .height(ZONE_H_DP.dp)
            .clipToBounds() // the design's overflow:hidden — only ~6 letters show
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
                    val r = letters[frac.roundToInt().coerceIn(0, letters.lastIndex)]
                    if (r != lastRounded) onStep(r)
                }
            },
    ) {
        letters.forEachIndexed { i, letter ->
            val distance = abs(i - frac)
            // Skip anything far outside the clipped window.
            if (distance > (ZONE_H_DP / ROW_DP) / 2f + 1.5f) return@forEachIndexed
            val f = (1f - distance / FALLOFF_ROWS).coerceAtLeast(0f)
            val empty = (counts[letter] ?: 0) == 0
            Text(
                text = letter.toString(),
                color = if (dragging && letter == rounded) palette.Accent else palette.Muted,
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
                        alpha = if (empty) 0.3f + 0.2f * f else 0.4f + 0.6f * f
                    },
            )
        }
    }
}
