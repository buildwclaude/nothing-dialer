package com.buildwclaude.dialer.core.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buildwclaude.dialer.core.ui.theme.palette
import java.util.Calendar
import kotlin.math.absoluteValue

@Composable
fun MonoAvatar(name: String?, photoUri: String?, size: Dp, modifier: Modifier = Modifier) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        // iMessage-style gradient avatars, same set as the Messages app.
        val (start, end) = AvatarGradients[
            (name?.hashCode() ?: 0).absoluteValue % AvatarGradients.size,
        ]
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(start, end),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
        ) {
            Text(
                text = initials(name),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}

private val AvatarGradients = listOf(
    Color(0xFF56CCF2) to Color(0xFF2F80ED), // blue
    Color(0xFFB06AB3) to Color(0xFF4568DC), // purple
    Color(0xFFF093FB) to Color(0xFFF5576C), // pink
    Color(0xFF43E97B) to Color(0xFF38B2A3), // green
    Color(0xFFFDC830) to Color(0xFFF37335), // orange
    Color(0xFF4FACFE) to Color(0xFF00C2FE), // cyan
    Color(0xFFFF758C) to Color(0xFFFF7EB3), // rose
    Color(0xFFA18CD1) to Color(0xFF6A5ACD), // violet
)

private fun initials(name: String?): String {
    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return "#"
    val parts = n.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}

object CallFormat {
    fun time(context: Context, millis: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return android.text.format.DateFormat.getTimeFormat(context).format(millis)
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
    }

    fun duration(sec: Int): String {
        if (sec <= 0) return ""
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
