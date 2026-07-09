package com.inzpire.customer.ui.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitData.PaymentStatus
import com.inzpire.customer.data.formatINR
import com.inzpire.customer.data.formatINRCompact
import com.inzpire.customer.ui.components.BrandGradientBox
import com.inzpire.customer.ui.components.ProgressBar
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Gold
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.Success

@Composable
fun PaymentsScreen(
    project: CockpitData.Project,
    payments: List<CockpitData.Payment>,
    onPay: (String) -> Unit,
) {
    val paid = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
    val pct = if (project.value > 0) (paid / project.value).toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Payments", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Milestone schedule for ${project.title}.", color = MutedForeground, fontSize = 13.sp)
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            BrandGradientBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Paid so far", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(formatINRCompact(paid), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("of ${formatINRCompact(project.value)} (${(pct * 100).toInt()}%)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    ProgressBar(fraction = pct, modifier = Modifier.padding(top = 8.dp), fillColor = Gold)
                }
            }
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column {
                payments.forEachIndexed { i, p ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.milestone, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (p.status == PaymentStatus.PAID) "Paid" else "Awaiting your payment",
                                color = MutedForeground, fontSize = 12.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatINR(p.amount), color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (p.status == PaymentStatus.PAID) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = Sky, modifier = Modifier.size(13.dp))
                                    Text(" Invoice", color = Sky, fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onPay(p.id) },
                                    modifier = Modifier.padding(top = 4.dp).height(34.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                                ) { Text("Pay now", fontSize = 13.sp) }
                            }
                        }
                        if (p.status == PaymentStatus.PAID) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.padding(start = 10.dp).size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
