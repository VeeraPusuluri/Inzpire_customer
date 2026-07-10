package com.inzpire.customer.data

import com.inzpire.customer.data.model.DeviceTokenInsert
import io.github.jan.supabase.postgrest.postgrest

/**
 * Registers this device's FCM token against the signed-in user so the
 * `send-push` Edge Function can deliver to it. Upsert keyed on `token` so the
 * same device moving between accounts just re-points to the current user.
 */
class DeviceTokenRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun register(userId: String, token: String) {
        client.postgrest["device_tokens"].upsert(
            DeviceTokenInsert(userId = userId, token = token),
        ) { onConflict = "token" }
    }

    suspend fun unregister(token: String) {
        client.postgrest["device_tokens"].delete { filter { eq("token", token) } }
    }
}
