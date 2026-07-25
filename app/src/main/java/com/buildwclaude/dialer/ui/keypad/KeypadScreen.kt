package com.buildwclaude.dialer.ui.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun KeypadScreen(
    initialNumber: String = "",
    onPlaceCall: (String) -> Unit,
) {
    var number by remember { mutableStateOf(initialNumber) }

    Column(
        modifier = Modifier.fillMaxSize().background(palette.Surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // Dialed number display.
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = palette.TextPrimary,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))

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
                        DialKey(key, Modifier.weight(1f)) { number += key.digit }
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
                    .clickable { if (number.isNotEmpty()) onPlaceCall(number) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_phone_call),
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (number.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { number = number.dropLast(1) },
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.digit,
                color = palette.TextPrimary,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    color = palette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}
