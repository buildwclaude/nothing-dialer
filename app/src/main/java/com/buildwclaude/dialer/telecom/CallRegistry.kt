package com.buildwclaude.dialer.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide holder of the framework Call objects the InCallService is driving.
 * The in-call UI observes this. (Framework Call instances are valid for the
 * lifetime of the bound InCallService, which is exactly the call's lifetime.)
 */
object CallRegistry {
    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    val calls: StateFlow<List<Call>> = _calls

    fun add(call: Call) {
        if (_calls.value.none { it === call }) _calls.value = _calls.value + call
    }

    fun remove(call: Call) {
        _calls.value = _calls.value.filterNot { it === call }
    }

    /** The call the UI should foreground: a ringing one if present, else the newest. */
    val primary: Call?
        get() = _calls.value.firstOrNull { it.details?.state == Call.STATE_RINGING }
            ?: _calls.value.lastOrNull()

    // ----- Audio controls, routed through the bound InCallService -----

    @Volatile
    var service: InCallService? = null

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn

    fun toggleMute() {
        val next = !_muted.value
        service?.setMuted(next)
        _muted.value = next
    }

    fun toggleSpeaker() {
        val next = !_speakerOn.value
        service?.setAudioRoute(
            if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
        )
        _speakerOn.value = next
    }

    /** Called by the service when the framework reports the real audio state. */
    fun onAudioStateChanged(state: CallAudioState?) {
        state ?: return
        _muted.value = state.isMuted
        _speakerOn.value = state.route == CallAudioState.ROUTE_SPEAKER
    }

    fun reset() {
        _muted.value = false
        _speakerOn.value = false
    }
}
