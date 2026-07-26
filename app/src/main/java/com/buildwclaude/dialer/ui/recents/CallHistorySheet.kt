package com.buildwclaude.dialer.ui.recents

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.CallFormat
import com.buildwclaude.dialer.core.ui.MonoAvatar
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.domain.CallDirection
import com.buildwclaude.dialer.domain.RecentCall

/**
 * Tapping a recent opens this: the full call history for that number — every
 * call with direction, date, time and duration — plus call/message shortcuts.
 */
@Composable
fun CallHistorySheet(
    call: RecentCall,
    history: List<RecentCall>,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onDelete: (Set<Long>) -> Unit,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Tapping the dimmed area above the sheet closes it.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                color = palette.Surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    // Swallow taps on the sheet itself.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoAvatar(call.name, call.photoUri, 52.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                call.display,
                                style = DesignType.screenTitle,
                                color = palette.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                call.number.ifBlank { "Unknown number" },
                                style = DesignType.body,
                                color = palette.TextSecondary,
                                maxLines = 1,
                            )
                        }
                        RoundAction(R.drawable.ic_message, "Message") {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${call.number}")),
                                )
                            }
                            onDismiss()
                        }
                        Spacer(Modifier.width(10.dp))
                        RoundAction(R.drawable.ic_phone_call, "Call", filled = true, onClick = onCall)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Call history",
                        style = DesignType.itemTitle,
                        color = palette.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = palette.Divider)

                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(history, key = { it.id }) { h ->
                            HistoryRow(h)
                            HorizontalDivider(color = palette.Divider, thickness = 0.5.dp)
                        }
                        if (history.isEmpty()) {
                            item {
                                Text(
                                    "No earlier calls",
                                    style = DesignType.body,
                                    color = palette.Muted,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text(
                            "Delete history",
                            style = DesignType.itemTitle,
                            color = palette.Negative,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDelete(history.map { it.id }.toSet().ifEmpty { setOf(call.id) })
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Close",
                            style = DesignType.itemTitle,
                            color = palette.TextSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(h: RecentCall) {
    val context = LocalContext.current
    val missed = h.direction == CallDirection.MISSED || h.direction == CallDirection.REJECTED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Icon(
            painterResource(
                when (h.direction) {
                    CallDirection.OUTGOING -> R.drawable.ic_call_made
                    CallDirection.MISSED, CallDirection.REJECTED -> R.drawable.ic_call_missed
                    else -> R.drawable.ic_call_received
                },
            ),
            contentDescription = null,
            tint = if (missed) palette.Negative else palette.Muted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (h.direction) {
                    CallDirection.OUTGOING -> "Outgoing"
                    CallDirection.INCOMING -> "Incoming"
                    CallDirection.MISSED -> "Missed"
                    CallDirection.REJECTED -> "Declined"
                    CallDirection.BLOCKED -> "Blocked"
                    CallDirection.VOICEMAIL -> "Voicemail"
                },
                style = DesignType.body,
                color = if (missed) palette.Negative else palette.TextPrimary,
            )
            Text(
                CallFormat.fullDateTime(context, h.date),
                fontSize = 12.sp,
                color = palette.Muted,
            )
        }
        Text(
            CallFormat.duration(h.durationSec).ifEmpty { "—" },
            fontSize = 12.sp,
            color = palette.TextSecondary,
        )
    }
}

@Composable
private fun RoundAction(
    icon: Int,
    label: String,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (filled) palette.Positive else palette.KeyBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = if (filled) Color.White else palette.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
