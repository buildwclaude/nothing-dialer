package com.buildwclaude.dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.VideoProfile

/** Handles answer / decline actions from the incoming-call notification. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = CallRegistry.primary ?: return
        when (intent.action) {
            ACTION_ANSWER -> call.answer(VideoProfile.STATE_AUDIO_ONLY)
            ACTION_DECLINE -> call.reject(false, null)
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.buildwclaude.dialer.ANSWER"
        const val ACTION_DECLINE = "com.buildwclaude.dialer.DECLINE"

        fun Call.answerAudio() = answer(VideoProfile.STATE_AUDIO_ONLY)
    }
}
