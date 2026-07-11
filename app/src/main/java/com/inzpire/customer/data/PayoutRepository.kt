package com.inzpire.customer.data

import com.inzpire.customer.data.model.PayoutRow
import com.inzpire.customer.data.model.ReferralBonusRow
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** Reads the money owed / paid to the signed-in customer (referral bonuses, commissions, refunds). */
class PayoutRepository(private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client) {

    /** Every payout credited to this user, newest first. RLS scopes it to `payee_id = auth.uid()`. */
    suspend fun listPayouts(userId: String): List<PayoutRow> =
        client.postgrest["payouts"].select(
            columns = Columns.list("id", "amount", "type", "status", "paid_at", "created_at", "ref"),
        ) {
            filter { eq("payee_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<PayoutRow>()

    /** This user's referral bonuses that haven't been released yet — money still to come. */
    suspend fun listPendingReferralBonuses(userId: String): List<ReferralBonusRow> =
        client.postgrest["referrals"].select(
            columns = Columns.list("id", "bonus_amount", "is_paid", "status", "milestone", "referred_name", "created_at"),
        ) {
            filter {
                eq("influencer_id", userId)
                eq("is_paid", false)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<ReferralBonusRow>()
}
