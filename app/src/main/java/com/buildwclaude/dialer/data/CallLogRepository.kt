package com.buildwclaude.dialer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import com.buildwclaude.dialer.domain.CallDirection
import com.buildwclaude.dialer.domain.RecentCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun canRead() =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    suspend fun recentCalls(limit: Int = 500): List<RecentCall> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val out = ArrayList<RecentCall>()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.CACHED_PHOTO_URI,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    out += RecentCall(
                        id = c.getLong(0),
                        number = c.getString(1) ?: "",
                        name = c.getString(2),
                        photoUri = c.getString(3),
                        direction = when (c.getInt(4)) {
                            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                            CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
                            CallLog.Calls.VOICEMAIL_TYPE -> CallDirection.VOICEMAIL
                            else -> CallDirection.INCOMING
                        },
                        date = c.getLong(5),
                        durationSec = c.getInt(6),
                    )
                }
            }
        }
        out
    }
}
