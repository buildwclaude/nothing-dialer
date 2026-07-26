package com.buildwclaude.dialer.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.buildwclaude.dialer.ui.incall.InCallActivity

/**
 * Bound by the system while this app is the default phone app. Every incoming or
 * outgoing call is delivered here. Incoming calls are surfaced via a full-screen
 * intent notification (background activity starts are blocked on Android 10+);
 * outgoing calls can open the screen directly since the app is in the foreground.
 */
class PhoneInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            when (state) {
                Call.STATE_ACTIVE -> CallNotifier.showOngoing(this@PhoneInCallService, call)
                Call.STATE_DISCONNECTED -> CallNotifier.clear(this@PhoneInCallService)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        CallRegistry.service = this
        CallRegistry.add(call)
        call.registerCallback(callback)

        val ringing = call.details?.state == Call.STATE_RINGING

        // Telecom grants the bound InCallService a background-activity-launch
        // exemption for the duration of a call, so we can open the call screen
        // directly — including over the lock screen.
        val launched = runCatching {
            startActivity(
                Intent(this, InCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }.isSuccess

        when {
            // While ringing we deliberately post NO notification: a CallStyle
            // incoming notification always shows a heads-up banner, which would
            // float on top of the full-screen call UI. Only if the screen could
            // not be launched do we fall back to the full-screen-intent one.
            ringing && !launched -> CallNotifier.showIncoming(this, call)
            !ringing -> CallNotifier.showOngoing(this, call)
        }
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        CallRegistry.remove(call)
        if (CallRegistry.calls.value.isEmpty()) {
            CallNotifier.clear(this)
            CallRegistry.reset()
            CallRegistry.service = null
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallRegistry.onAudioStateChanged(audioState)
    }
}
