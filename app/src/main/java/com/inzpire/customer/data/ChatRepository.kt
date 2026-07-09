package com.inzpire.customer.data

import com.inzpire.customer.data.model.MessageDto
import com.inzpire.customer.data.model.MessageInsert
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ChatRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun list(projectId: String): List<MessageDto> =
        client.postgrest["messages"].select {
            filter { eq("project_id", projectId) }
            order("created_at", Order.ASCENDING)
        }.decodeList()

    suspend fun send(projectId: String, senderId: String, body: String) {
        client.postgrest["messages"].insert(
            MessageInsert(projectId = projectId, senderId = senderId, body = body),
        )
    }
}
