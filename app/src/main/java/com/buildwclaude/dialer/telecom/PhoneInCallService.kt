package com.buildwclaude.dialer.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.buildwclaude.dialer.ui.incall.InCallActivity

/**
 * Bound by the system while this app is the default phone app. Every incoming or
 * outgoing call is delivered here; we register it and bring up the call screen.
 */
class PhoneInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        CallRegistry.add(call)
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    override fun onCallRemoved(call: Call) {
        CallRegistry.remove(call)
    }
}
