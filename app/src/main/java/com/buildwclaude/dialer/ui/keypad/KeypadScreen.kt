package com.buildwclaude.dialer.ui.keypad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.theme.palette

private data class Key(val digit: String, val letters: String = "")

private val KEYS = listOf(
    Key("1"), Key("2", "A B C"), Key("3", "D E F"),
    Key("4", "G H I"), Key("5", "J K L"), Key("6", "M N O"),
    Key("7", "P Q R S"), Key("8", "T U V"), Key("9", "W X Y Z"),
    Key("*"), Key("0", "+"), Key("#"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
    initialNumber: String = "",
    onPlaceCall: (String) -> Unit,
) {
    var number by remember { mutableStateOf(initialNumber) }
    val view = LocalView.current

    Column(
        modifier = Modifier.fillMaxSize().background(palette.Surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // Dialed number display — large, shrinks as the number gets longer.
        val numberFont = when {
            number.length <= 11 -> 44.sp
            number.length <= 15 -> 34.sp
            number.length <= 20 -> 26.sp
            else -> 20.sp
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = palette.TextPrimary,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = numberFont,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(10.dp))

        // 3 × 4 key grid.
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 44.dp),
        ) {
            KEYS.chunked(3).forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowKeys.forEach { key ->
                        DialKey(key, Modifier.weight(1f)) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            number += key.digit
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Call row: centered green call button with a delete key to its right.
        Box(Modifier.fillMaxWidth().padding(horizontal = 44.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(palette.Positive)
                    .clickable {
                        if (number.isNotEmpty()) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onPlaceCall(number)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_phone_call),
                    contentDescription = "Call",
                    tint = Color.White, // white glyph on the green call button
                    modifier = Modifier.size(32.dp),
                )
            }
            if (number.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        // Tap deletes one digit; long-press clears the whole number.
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                number = number.dropLast(1)
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                number = ""
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_backspace),
                        contentDescription = "Delete",
                        tint = palette.TextPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DialKey(key: Key, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(palette.KeyBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Fixed slots: every key reserves the same digit height and the same
        // letters strip, so digits sit at one consistent height whether or not
        // the key has letters (1, * and # used to float out of centre).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text(
                text = key.digit,
                color = palette.TextPrimary,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(34.dp),
            )
            Box(Modifier.height(12.dp), contentAlignment = Alignment.TopCenter) {
                if (key.letters.isNotEmpty()) {
                    Text(
                        text = key.letters,
                        color = palette.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
