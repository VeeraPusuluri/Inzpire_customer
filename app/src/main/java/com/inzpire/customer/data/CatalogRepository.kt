package com.inzpire.customer.data

import com.inzpire.customer.data.model.LeadInsert
import com.inzpire.customer.data.model.OfferDto
import com.inzpire.customer.data.model.ServiceDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/** Global content the customer can browse/act on outside a single project. */
class CatalogRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun offers(): List<OfferDto> =
        client.postgrest["offers"].select {
            filter { eq("is_active", true) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun services(): List<ServiceDto> =
        client.postgrest["services"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()

    suspend fun submitEnquiry(
        customerId: String,
        serviceId: String?,
        requirement: String,
        location: String?,
        address: String?,
        budget: Double?,
    ) {
        client.postgrest["leads"].insert(
            LeadInsert(
                customerId = customerId,
                serviceId = serviceId,
                requirement = requirement,
                location = location?.ifBlank { null },
                address = address?.ifBlank { null },
                budget = budget,
            ),
        )
    }
}
