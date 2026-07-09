package com.inzpire.customer.data

import com.inzpire.customer.data.model.Profile
import com.inzpire.customer.data.model.ProfilePatch
import com.inzpire.customer.data.model.UserRole
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class ProfileRepository(private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client) {

    suspend fun getProfile(userId: String): Profile? =
        client.postgrest["profiles"].select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull()

    suspend fun getRoles(userId: String): List<String> =
        client.postgrest["user_roles"].select(columns = Columns.list("role")) {
            filter { eq("user_id", userId) }
        }.decodeList<UserRole>().map { it.role }

    suspend fun updateProfile(userId: String, patch: ProfilePatch) {
        client.postgrest["profiles"].update(patch) {
            filter { eq("id", userId) }
        }
    }
}
