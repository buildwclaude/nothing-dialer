package com.buildwclaude.dialer.ui.recents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.CallFormat
import com.buildwclaude.dialer.core.ui.ContactActionSheet
import com.buildwclaude.dialer.core.ui.MonoAvatar
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.data.CallLogRepository
import com.buildwclaude.dialer.domain.CallDirection
import com.buildwclaude.dialer.domain.RecentCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val repo: CallLogRepository,
) : ViewModel() {
    val calls = MutableStateFlow<List<RecentCall>>(emptyList())
    init { refresh() }
    fun refresh() = viewModelScope.launch { calls.value = repo.recentCalls() }
    fun delete(ids: Set<Long>) = viewModelScope.launch {
        repo.delete(ids)
        calls.value = repo.recentCalls()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    onPlaceCall: (String) -> Unit,
    viewModel: RecentsViewModel = hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var sheetFor by remember { mutableStateOf<RecentCall?>(null) }

    fun exitSelection() { selecting = false; selected = emptySet() }

    Column(Modifier.fillMaxSize().background(palette.Surface)) {
        // Header doubles as the selection action bar.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                if (selecting) "${selected.size} selected" else "Recents",
                style = DesignType.screenTitle,
                color = palette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selecting) {
                Text(
                    "All",
                    style = DesignType.itemTitle,
                    color = palette.Accent,
                    modifier = Modifier
                        .clickable { selected = calls.map { it.id }.toSet() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    "Delete",
                    style = DesignType.itemTitle,
                    color = if (selected.isEmpty()) palette.Muted else palette.Negative,
                    modifier = Modifier
                        .clickable(enabled = selected.isNotEmpty()) {
                            viewModel.delete(selected)
                            exitSelection()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    "Done",
                    style = DesignType.itemTitle,
                    color = palette.TextSecondary,
                    modifier = Modifier
                        .clickable { exitSelection() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            } else if (calls.isNotEmpty()) {
                Text(
                    "Select",
                    style = DesignType.itemTitle,
                    color = palette.Accent,
                    modifier = Modifier
                        .clickable { selecting = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        if (calls.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No recent calls", style = DesignType.body, color = palette.TextSecondary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(calls, key = { it.id }) { call ->
                    RecentRow(
                        call = call,
                        selecting = selecting,
                        checked = call.id in selected,
                        onToggle = {
                            selected = if (call.id in selected) selected - call.id else selected + call.id
                        },
                        onOpen = { sheetFor = call },
                        onLongPress = {
                            if (!selecting) { selecting = true; selected = setOf(call.id) }
                        },
                        onCall = { onPlaceCall(call.number) },
                    )
                    HorizontalDivider(
                        color = palette.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 84.dp),
                    )
                }
            }
        }
    }

    sheetFor?.let { call ->
        ContactActionSheet(
            title = call.display,
            number = call.number,
            photoUri = call.photoUri,
            subtitle = "${directionLabel(call.direction)} · ${CallFormat.time(context, call.date)}" +
                (CallFormat.duration(call.durationSec).takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""),
            onDismiss = { sheetFor = null },
            onCall = { sheetFor = null; onPlaceCall(call.number) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    call: RecentCall,
    selecting: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onCall: () -> Unit,
) {
    val context = LocalContext.current
    val missed = call.direction == CallDirection.MISSED || call.direction == CallDirection.REJECTED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selecting) onToggle() else onOpen() },
                onLongClick = onLongPress,
            )
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        if (selecting) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.Accent,
                    uncheckedColor = palette.Muted,
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            MonoAvatar(call.name, call.photoUri, 44.dp)
            Spacer(Modifier.width(16.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(
                call.display,
                style = DesignType.itemTitle,
                color = if (missed) palette.Negative else palette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(directionIcon(call.direction)),
                    contentDescription = null,
                    tint = if (missed) palette.Negative else palette.Muted,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    directionLabel(call.direction),
                    fontSize = 12.sp,
                    color = palette.TextSecondary,
                    maxLines = 1,
                )
            }
        }

        // Timestamp is width-capped so a long date can never push the call
        // button off the row (which is why it vanished further down the list).
        Text(
            CallFormat.time(context, call.date),
            fontSize = 12.sp,
            color = palette.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 84.dp).padding(end = 4.dp),
        )

        if (!selecting) {
            // Only this button places the call — tapping the row opens actions.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_phone_call),
                    contentDescription = "Call ${call.display}",
                    tint = palette.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun directionIcon(d: CallDirection): Int = when (d) {
    CallDirection.OUTGOING -> R.drawable.ic_call_made
    CallDirection.MISSED, CallDirection.REJECTED -> R.drawable.ic_call_missed
    else -> R.drawable.ic_call_received
}

private fun directionLabel(d: CallDirection): String = when (d) {
    CallDirection.OUTGOING -> "Outgoing"
    CallDirection.INCOMING -> "Incoming"
    CallDirection.MISSED -> "Missed"
    CallDirection.REJECTED -> "Declined"
    CallDirection.BLOCKED -> "Blocked"
    CallDirection.VOICEMAIL -> "Voicemail"
}
