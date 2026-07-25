package com.buildwclaude.dialer.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Places an outgoing call through the system Telecom stack. As the default phone
 * app this routes back into our own InCallService → in-call screen.
 */
@Singleton
class CallPlacer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun placeCall(number: String) {
        val digits = number.trim()
        if (digits.isEmpty()) return
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val tm = context.getSystemService(TelecomManager::class.java) ?: return
        val uri = Uri.fromParts("tel", digits, null)
        runCatching { tm.placeCall(uri, Bundle()) }
    }
}
