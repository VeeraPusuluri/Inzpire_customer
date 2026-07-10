package com.inzpire.customer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row DTOs for the live cockpit tables (mapped into the UI models in
 * [com.inzpire.customer.data.CockpitRepository]). Column names are snake_case
 * via [SerialName] to match the Supabase schema.
 */

@Serializable
data class ProjectDto(
    val id: String,
    val name: String? = null,
    val code: String? = null,
    val segment: String? = null,
    val status: String? = null,
    val stage: Int = 1,
    @SerialName("start_date") val startDate: String? = null,
    val value: Double = 0.0,
    @SerialName("target_days") val targetDays: Int = 60,
    @SerialName("target_handover_date") val targetHandoverDate: String? = null,
    @SerialName("manager_id") val managerId: String? = null,
    @SerialName("designer_id") val designerId: String? = null,
)

@Serializable
data class ProfileLiteDto(
    val id: String,
    val name: String? = null,
    val phone: String? = null,
    val location: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class MoodboardDto(
    val id: String,
    val room: String? = null,
    val title: String,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    val type: String = "moodboard",
    val version: Int = 1,
    val status: String = "draft",
    val comment: String? = null,
)

@Serializable
data class RoomDto(
    val id: String,
    val name: String,
)

@Serializable
data class SelectionDto(
    val id: String,
    val category: String? = null,
    val make: String? = null,
    val status: String = "suggested",
    @SerialName("room_id") val roomId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
)

@Serializable
data class ProductDto(
    val id: String,
    val category: String? = null,
    val description: String? = null,
    @SerialName("make_notes") val makeNotes: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class PaymentDto(
    val id: String,
    val milestone: String,
    val amount: Double = 0.0,
    val status: String = "pending",
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
data class ApprovalDto(
    val id: String,
    val type: String,
    val title: String? = null,
    val status: String = "pending",
    @SerialName("approver_id") val approverId: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SiteUpdateDto(
    val id: String,
    val note: String? = null,
    val geotag: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class DocumentDto(
    val id: String,
    val name: String,
    @SerialName("doc_type") val docType: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val body: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("valid_till") val validTill: String? = null,
)

@Serializable
data class ServiceDto(
    val id: String,
    val name: String,
)

// ---- write payloads ----

@Serializable
data class ApprovalPatch(
    val status: String,
    @SerialName("approver_id") val approverId: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    val comment: String? = null,
)

@Serializable
data class MoodboardPatch(
    val status: String,
    val comment: String? = null,
)

@Serializable
data class PaymentPatch(
    val status: String,
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
data class SelectionPatch(
    val status: String,
)

@Serializable
data class MessageInsert(
    @SerialName("project_id") val projectId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
)

@Serializable
data class LeadInsert(
    @SerialName("customer_id") val customerId: String,
    @SerialName("service_id") val serviceId: String? = null,
    val requirement: String,
    val location: String? = null,
    val address: String? = null,
    val budget: Double? = null,
    val source: String = "app",
)

/** A customer-raised change request on a material or design (see change_requests). */
@Serializable
data class ChangeRequestInsert(
    @SerialName("project_id") val projectId: String,
    @SerialName("customer_id") val customerId: String,
    val kind: String, // "material" | "design"
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("ref_title") val refTitle: String? = null,
    val room: String? = null,
    val note: String,
)
