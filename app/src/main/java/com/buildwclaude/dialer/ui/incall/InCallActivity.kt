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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.PhoneTheme
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.telecom.CallRegistry

/**
 * Minimal in-call screen for Milestone 1: shows the number/state and lets you
 * answer/decline/end. The polished screen comes with the Figma design.
 */
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhoneTheme { CallScreen(onFinish = { finish() }) } }
    }
}

@Composable
private fun CallScreen(onFinish: () -> Unit) {
    val calls by CallRegistry.calls.collectAsStateWithLifecycle()
    val primary = calls.firstOrNull { it.details?.state == Call.STATE_RINGING } ?: calls.lastOrNull()

    if (primary == null) {
        onFinish()
        return
    }

    var state by remember { mutableStateOf(primary.details?.state ?: Call.STATE_NEW) }
    DisposableEffect(primary) {
        val cb = object : Call.Callback() {
            override fun onStateChanged(c: Call, newState: Int) { state = newState }
        }
        primary.registerCallback(cb)
        state = primary.details?.state ?: Call.STATE_NEW
        onDispose { primary.unregisterCallback(cb) }
    }

    val number = primary.details?.handle?.schemeSpecificPart ?: "Unknown"
    val ringing = state == Call.STATE_RINGING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        Text(number, style = DesignType.screenTitle, color = palette.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stateLabel(state), style = DesignType.body, color = palette.TextSecondary)

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (ringing) {
                CallButton("Decline", palette.Negative) {
                    primary.reject(false, null); onFinish()
                }
                CallButton("Answer", palette.Positive) {
                    primary.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            } else {
                CallButton("End", palette.Negative) {
                    primary.disconnect(); onFinish()
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun CallButton(label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.first().toString(), color = Color.White, fontSize = 24.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = DesignType.label, color = palette.TextSecondary)
    }
}

private fun stateLabel(state: Int): String = when (state) {
    Call.STATE_RINGING -> "Incoming call"
    Call.STATE_DIALING -> "Calling…"
    Call.STATE_ACTIVE -> "In call"
    Call.STATE_HOLDING -> "On hold"
    Call.STATE_CONNECTING -> "Connecting…"
    Call.STATE_DISCONNECTED -> "Call ended"
    else -> ""
}
