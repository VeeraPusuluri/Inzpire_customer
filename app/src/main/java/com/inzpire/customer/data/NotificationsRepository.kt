package com.inzpire.customer.data

import com.inzpire.customer.data.model.NotificationDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/** Reads/writes the signed-in user's in-app notifications (public.notifications). */
class NotificationsRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun list(userId: String, limit: Long = 50): List<NotificationDto> =
        client.postgrest["notifications"].select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(limit)
        }.decodeList()

    suspend fun markRead(id: String) {
        client.postgrest["notifications"].update({ set("is_read", true) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun markAllRead(userId: String) {
        client.postgrest["notifications"].update({ set("is_read", true) }) {
            filter {
                eq("user_id", userId)
                eq("is_read", false)
            }
        }
    }

    /** Permanently removes a single notification from the DB. */
    suspend fun delete(id: String) {
        client.postgrest["notifications"].delete {
            filter { eq("id", id) }
        }
    }

    /** Permanently removes all of the user's notifications from the DB. */
    suspend fun deleteAll(userId: String) {
        client.postgrest["notifications"].delete {
            filter { eq("user_id", userId) }
        }
    }
}
