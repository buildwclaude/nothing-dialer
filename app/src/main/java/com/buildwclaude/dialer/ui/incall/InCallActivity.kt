package com.buildwclaude.dialer.ui.incall

import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.MonoAvatar
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.PhoneTheme
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.telecom.CallNotifier
import com.buildwclaude.dialer.telecom.CallRegistry
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the lock screen and wake the display for incoming calls.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        runCatching {
            getSystemService(android.app.KeyguardManager::class.java)
                ?.requestDismissKeyguard(this, null)
        }
        setContent { PhoneTheme { CallScreen(onFinish = { finishAndRemoveTask() }) } }
    }
}

@Composable
private fun CallScreen(onFinish: () -> Unit) {
    val calls by CallRegistry.calls.collectAsStateWithLifecycle()
    val muted by CallRegistry.muted.collectAsStateWithLifecycle()
    val speakerOn by CallRegistry.speakerOn.collectAsStateWithLifecycle()
    val call = calls.firstOrNull { it.details?.state == Call.STATE_RINGING } ?: calls.lastOrNull()

    if (call == null) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var state by remember { mutableIntStateOf(call.details?.state ?: Call.STATE_NEW) }
    DisposableEffect(call) {
        val cb = object : Call.Callback() {
            override fun onStateChanged(c: Call, newState: Int) { state = newState }
        }
        call.registerCallback(cb)
        state = call.details?.state ?: Call.STATE_NEW
        onDispose { call.unregisterCallback(cb) }
    }

    // Live call duration once connected.
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        if (state == Call.STATE_ACTIVE) {
            val start = call.details?.connectTimeMillis?.takeIf { it > 0 } ?: System.currentTimeMillis()
            while (true) {
                elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L) / 1000
                delay(1000)
            }
        }
    }
    LaunchedEffect(state) {
        if (state == Call.STATE_DISCONNECTED) { delay(600); onFinish() }
    }

    val ringing = state == Call.STATE_RINGING
    val name = CallNotifier.callerLabel(call)
    var showKeypad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        MonoAvatar(name, null, 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            name,
            style = DesignType.screenTitle,
            color = palette.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (state) {
                Call.STATE_ACTIVE -> formatDuration(elapsed)
                else -> stateLabel(state)
            },
            style = DesignType.body,
            color = palette.TextSecondary,
        )

        Spacer(Modifier.weight(1f))

        if (showKeypad && !ringing) {
            InCallKeypad(
                onDigit = { d -> call.playDtmfTone(d); call.stopDtmfTone() },
                onClose = { showKeypad = false },
            )
            Spacer(Modifier.height(16.dp))
        } else if (!ringing) {
            // Control grid: mute / keypad / speaker, then hold / add.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ControlButton(R.drawable.ic_mic_off, "Mute", active = muted) { CallRegistry.toggleMute() }
                ControlButton(R.drawable.ic_tab_keypad, "Keypad") { showKeypad = true }
                ControlButton(R.drawable.ic_speaker, "Speaker", active = speakerOn) { CallRegistry.toggleSpeaker() }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val onHold = state == Call.STATE_HOLDING
                ControlButton(R.drawable.ic_pause, if (onHold) "Resume" else "Hold", active = onHold) {
                    if (onHold) call.unhold() else call.hold()
                }
                ControlButton(R.drawable.ic_add_call, "Add") { /* multi-call comes later */ }
                Spacer(Modifier.size(64.dp)) // keeps the row balanced
            }
            Spacer(Modifier.height(28.dp))
        }

        // Answer / decline while ringing; single end button once connected.
        Row(
            Modifier.fillMaxWidth().padding(bottom = 44.dp),
            horizontalArrangement = if (ringing) Arrangement.SpaceEvenly else Arrangement.Center,
        ) {
            if (ringing) {
                BigAction(R.drawable.ic_call_end, "Decline", palette.Negative, rotate = true) {
                    call.reject(false, null); onFinish()
                }
                BigAction(R.drawable.ic_phone_call, "Answer", Color(0xFF30D158)) {
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            } else {
                BigAction(R.drawable.ic_call_end, "End", palette.Negative, rotate = true) {
                    call.disconnect(); onFinish()
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) palette.TextPrimary else palette.KeyBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                tint = if (active) palette.Surface else palette.TextPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = palette.TextSecondary)
    }
}

@Composable
private fun BigAction(
    icon: Int,
    label: String,
    color: Color,
    rotate: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .then(if (rotate) Modifier.rotate(135f) else Modifier),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = palette.TextSecondary)
    }
}

@Composable
private fun InCallKeypad(onDigit: (Char) -> Unit, onClose: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { k ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(palette.KeyBg)
                            .clickable { onDigit(k.first()) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(k, color = palette.TextPrimary, fontSize = 26.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Hide",
            color = palette.TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onClose).padding(8.dp),
        )
    }
}

private fun formatDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

private fun stateLabel(state: Int): String = when (state) {
    Call.STATE_RINGING -> "Incoming call"
    Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
    Call.STATE_ACTIVE -> "In call"
    Call.STATE_HOLDING -> "On hold"
    Call.STATE_DISCONNECTED -> "Call ended"
    else -> ""
}
