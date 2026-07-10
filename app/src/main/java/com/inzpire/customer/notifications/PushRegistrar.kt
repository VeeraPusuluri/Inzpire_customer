package com.inzpire.customer.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.inzpire.customer.data.DeviceTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bridges Firebase's current FCM token to Supabase `device_tokens`. Call
 * [register] right after sign-in and [unregister] while still signed in (RLS
 * scopes the delete to the current user) just before signing out.
 */
object PushRegistrar {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun register(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            scope.launch { runCatching { DeviceTokenRepository().register(userId, token) } }
        }
    }

    /** Deletes this device's token row, then invalidates the token locally. */
    fun unregister(onDone: () -> Unit = {}) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = task.result
            scope.launch {
                if (token != null) runCatching { DeviceTokenRepository().unregister(token) }
                runCatching { FirebaseMessaging.getInstance().deleteToken() }
                onDone()
            }
        }
    }
}
