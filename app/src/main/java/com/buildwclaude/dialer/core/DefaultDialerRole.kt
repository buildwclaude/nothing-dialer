package com.buildwclaude.dialer.core

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps requesting/checking the default-phone (ROLE_DIALER) status. Only the
 * default dialer may place calls silently, read/write the full call log, and
 * drive the in-call UI; when not default the app degrades to read-only.
 */
@Singleton
class DefaultDialerRole @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isDefault: Boolean
        get() {
            val rm = context.getSystemService(RoleManager::class.java) ?: return false
            return rm.isRoleHeld(RoleManager.ROLE_DIALER)
        }

    fun requestIntent(): Intent? {
        val rm = context.getSystemService(RoleManager::class.java) ?: return null
        if (!rm.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
        return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    }
}
