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

        if (call.details?.state == Call.STATE_RINGING) {
            CallNotifier.showIncoming(this, call)
        } else {
            CallNotifier.showOngoing(this, call)
        }
        // Telecom grants the bound InCallService a background-activity-launch
        // exemption for the duration of a call, so open the call screen directly
        // as well — the full-screen intent alone only fires when the device is
        // locked/idle, which is why incoming calls showed just a notification.
        runCatching {
            startActivity(
                Intent(this, InCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
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
