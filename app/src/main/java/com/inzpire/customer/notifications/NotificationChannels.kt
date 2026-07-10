package com.inzpire.customer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Notification channels. Created once at app start (Android 8+ requires them). */
object NotificationChannels {
    const val DEFAULT = "inzpire_default"

    /** Brand coral-pink used for the notification accent + LED light. */
    const val ACCENT = 0xFFFF2D6F.toInt()

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(DEFAULT) != null) return
        nm.createNotificationChannel(
            NotificationChannel(DEFAULT, "Project updates", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Messages, approvals, payments and project updates"
                enableLights(true)
                lightColor = ACCENT
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
            },
        )
    }
}
