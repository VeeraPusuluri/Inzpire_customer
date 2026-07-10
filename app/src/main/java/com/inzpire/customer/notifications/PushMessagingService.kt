package com.inzpire.customer.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.inzpire.customer.MainActivity
import com.inzpire.customer.R
import com.inzpire.customer.data.DeviceTokenRepository
import com.inzpire.customer.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Receives FCM pushes. When the app is foregrounded, FCM delivers `notification`
 * messages here too, so we always build the system notification ourselves and
 * attach the `link` for deep-linking on tap.
 */
class PushMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        // Rotated token — re-register for whoever is signed in.
        val uid = runCatching { SupabaseClientProvider.client.auth.currentUserOrNull()?.id }.getOrNull() ?: return
        scope.launch { runCatching { DeviceTokenRepository().register(uid, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Inzpire"
        val body = message.notification?.body ?: data["body"] ?: ""
        show(title, body, data["link"])
    }

    private fun show(title: String, body: String, link: String?) {
        NotificationChannels.ensure(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (link != null) putExtra("notification_link", link)
        }
        val pending = PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Colourful brand mark on the right of the notification.
        val largeIcon = runCatching {
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, NotificationChannels.DEFAULT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setColor(NotificationChannels.ACCENT)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pending)
        largeIcon?.let { builder.setLargeIcon(it) }
        val notification = builder.build()

        // On Android 13+ posting requires the runtime POST_NOTIFICATIONS grant; skip silently
        // if the user hasn't granted it. Checked unconditionally so every path to notify() is
        // guarded (pre-13 it's install-time and returns GRANTED); Lint can't see runCatching.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
        }.onFailure { Log.w("PushMessagingService", "notify failed", it) }
    }
}
