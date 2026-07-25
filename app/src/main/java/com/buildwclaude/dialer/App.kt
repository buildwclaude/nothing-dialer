package com.buildwclaude.dialer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                getString(R.string.call_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING,
                getString(R.string.incoming_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { setBypassDnd(true) },
        )
    }

    companion object {
        const val CHANNEL_ONGOING = "ongoing_calls"
        const val CHANNEL_INCOMING = "incoming_calls"
    }
}
