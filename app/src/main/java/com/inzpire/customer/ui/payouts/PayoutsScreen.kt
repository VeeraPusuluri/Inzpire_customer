package com.inzpire.customer.ui.payouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.formatINR
import com.inzpire.customer.data.formatLongDate
import com.inzpire.customer.data.model.PayoutRow
import com.inzpire.customer.data.model.ReferralBonusRow
import com.inzpire.customer.ui.components.BrandGradientBox
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.components.SectionHeader
import com.inzpire.customer.ui.components.StatusChip
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.SkySoft
import com.inzpire.customer.ui.theme.Success
import com.inzpire.customer.ui.theme.Warning

@Composable
fun PayoutsScreen(
    payouts: List<PayoutRow>,
    pendingBonuses: List<ReferralBonusRow>,
    hasPayoutDetails: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onEditPayoutMethod: () -> Unit,
) {
    val received = payouts.filter { it.isPaid }.sumOf { it.amount }
    val processing = payouts.filter { it.status == "pending" || it.status == "approved" }.sumOf { it.amount }
    val upcoming = pendingBonuses.sumOf { it.bonusAmount }
    val nothingYet = payouts.isEmpty() && pendingBonuses.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with back affordance.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(SkySoft).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Navy, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Payouts", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Referral bonuses & refunds paid to you", color = MutedForeground, fontSize = 13.sp)
            }
        }

        // Summary — total received, with pending / upcoming underneath.
        SurfaceCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            BrandGradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Total received", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatINR(received), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryStat("Pending payouts", processing)
                        SummaryStat("Upcoming bonuses", upcoming, alignEnd = true)
                    }
                }
            }
        }

        // Where the money goes.
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (hasPayoutDetails) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = if (hasPayoutDetails) Success else Warning,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasPayoutDetails) "Payout method linked" else "Add your payout details",
                        color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (hasPayoutDetails) "Payouts are sent to your saved UPI / bank account."
                        else "Add a UPI ID or bank account so we can send your payouts.",
                        color = MutedForeground, fontSize = 12.sp,
                    )
                }
                TextButton(onClick = onEditPayoutMethod) {
                    Text(if (hasPayoutDetails) "Change" else "Add", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        if (loading && nothingYet) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Navy, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }

        // Upcoming referral bonuses — earned but not released yet.
        if (pendingBonuses.isNotEmpty()) {
            Column {
                SectionHeader("Upcoming bonuses")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    pendingBonuses.forEach { b ->
                        PayoutLine(
                            title = b.referredName?.ifBlank { null } ?: "Referral bonus",
                            subtitle = milestoneLabel(b.milestone, b.status) + (formatLongDate(b.createdAt)?.let { " · $it" } ?: ""),
                            amount = b.bonusAmount,
                            statusLabel = "Upcoming",
                            statusColor = Warning,
                        )
                    }
                }
            }
        }

        // Payout history.
        Column {
            SectionHeader("Payout history")
            if (payouts.isEmpty()) {
                if (!loading) {
                    EmptyState("No payouts yet", "Referral bonuses and refunds will appear here once they're released.")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    payouts.forEach { p ->
                        PayoutLine(
                            title = typeLabel(p.type),
                            subtitle = formatLongDate(p.paidAt ?: p.createdAt) ?: "",
                            amount = p.amount,
                            statusLabel = statusLabel(p.status),
                            statusColor = statusColor(p.status),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, amount: Double, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        Text(formatINR(amount), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PayoutLine(title: String, subtitle: String, amount: Double, statusLabel: String, statusColor: Color) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(SkySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Savings, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = MutedForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatINR(amount), color = Navy, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                StatusChip(statusLabel, statusColor, Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "referral" -> "Referral bonus"
    "commission" -> "Commission"
    "vendor" -> "Vendor payout"
    else -> type.replaceFirstChar { it.uppercase() }
}

private fun statusLabel(status: String): String = status.replaceFirstChar { it.uppercase() }

private fun statusColor(status: String): Color = when (status) {
    "paid" -> Success
    "approved" -> Sky
    "pending" -> Warning
    else -> MutedForeground
}

private fun milestoneLabel(milestone: String?, status: String?): String {
    val raw = milestone?.takeIf { it.isNotBlank() } ?: status?.takeIf { it.isNotBlank() } ?: "Referral"
    return raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
