package com.buildwclaude.dialer.ui.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
}

@Composable
fun RecentsScreen(
    onPlaceCall: (String) -> Unit,
    viewModel: RecentsViewModel = hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(palette.Surface)) {
        Text(
            "Recents",
            style = DesignType.screenTitle,
            color = palette.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        )
        if (calls.isEmpty()) {
            EmptyHint("No recent calls")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(calls, key = { it.id }) { call ->
                    RecentRow(call, onClick = { onPlaceCall(call.number) })
                    HorizontalDivider(
                        color = palette.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 84.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(call: RecentCall, onClick: () -> Unit) {
    val context = LocalContext.current
    val missed = call.direction == CallDirection.MISSED || call.direction == CallDirection.REJECTED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        MonoAvatar(call.name, call.photoUri, 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                call.display,
                style = DesignType.itemTitle,
                color = if (missed) palette.TextPrimary else palette.TextPrimary,
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
                    color = if (missed) palette.Negative else palette.TextSecondary,
                )
            }
        }
        Text(
            CallFormat.time(context, call.date),
            fontSize = 12.sp,
            color = palette.Muted,
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            painterResource(R.drawable.ic_phone_call),
            contentDescription = "Call",
            tint = palette.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = DesignType.body, color = palette.TextSecondary)
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
