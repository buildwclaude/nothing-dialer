package com.buildwclaude.dialer.telecom

import android.telecom.Call
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
}
