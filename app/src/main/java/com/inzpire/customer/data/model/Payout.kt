package com.inzpire.customer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A row from `public.payouts` — money the firm pays *out* to this user. `type` is one of
 * `referral` / `commission` / `vendor`; customers only ever see their own (`payee_id = me`).
 * `status` moves pending → approved → paid.
 */
@Serializable
data class PayoutRow(
    val id: String,
    val amount: Double = 0.0,
    val type: String = "referral",
    val status: String = "pending",
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val ref: String? = null,
) {
    val isPaid: Boolean get() = status == "paid"
}

/**
 * A row from `public.referrals` for referrals this user submitted (`influencer_id = me`).
 * Used to show bonuses that are earned but not yet released as a payout.
 */
@Serializable
data class ReferralBonusRow(
    val id: String,
    @SerialName("bonus_amount") val bonusAmount: Double = 0.0,
    @SerialName("is_paid") val isPaid: Boolean = false,
    val status: String? = null,
    val milestone: String? = null,
    @SerialName("referred_name") val referredName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
