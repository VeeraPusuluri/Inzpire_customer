package com.inzpire.customer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of public.notifications — the unified in-app notification store. */
@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String = "general",
    val title: String? = null,
    val body: String = "",
    val link: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Upsert payload for public.device_tokens (conflict target: token). */
@Serializable
data class DeviceTokenInsert(
    @SerialName("user_id") val userId: String,
    val token: String,
    val app: String = "customer",
    val platform: String = "android",
)
