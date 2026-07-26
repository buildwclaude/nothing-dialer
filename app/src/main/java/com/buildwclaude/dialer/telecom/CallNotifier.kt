package com.buildwclaude.dialer.telecom

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.buildwclaude.dialer.App
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.ui.incall.InCallActivity

/**
 * Posts the incoming/ongoing call notification. The full-screen intent is what
 * actually raises our call screen — apps can't start an activity from the
 * background on Android 10+, so this is the sanctioned path.
 */
object CallNotifier {
    const val NOTIFICATION_ID = 4242

    fun showIncoming(context: Context, call: Call) {
        val caller = callerLabel(call)

        val fullScreen = PendingIntent.getActivity(
            context, 0,
            Intent(context, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val answer = actionIntent(context, CallActionReceiver.ACTION_ANSWER, 1)
        val decline = actionIntent(context, CallActionReceiver.ACTION_DECLINE, 2)

        val person = Person.Builder().setName(caller).setImportant(true).build()
        val notification = NotificationCompat.Builder(context, App.CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_phone_call)
            .setContentTitle(caller)
            .setContentText("Incoming call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, decline, answer))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    fun showOngoing(context: Context, call: Call) {
        val caller = callerLabel(call)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hangUp = actionIntent(context, CallActionReceiver.ACTION_HANGUP, 3)
        val person = Person.Builder().setName(caller).setImportant(true).build()

        val notification = NotificationCompat.Builder(context, App.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_phone_call)
            .setContentTitle(caller)
            .setContentText("Ongoing call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(open)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUp))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    fun clear(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    private fun actionIntent(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun callerLabel(call: Call): String {
        val details = call.details
        val name = details?.callerDisplayName?.takeIf { it.isNotBlank() }
        val number = details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        return name ?: number ?: "Unknown"
    }
}
