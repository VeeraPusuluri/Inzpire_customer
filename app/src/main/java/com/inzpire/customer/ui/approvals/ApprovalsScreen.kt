package com.inzpire.customer.ui.approvals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitData.ReviewStatus
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.components.RemoteImage
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.SkySoft
import com.inzpire.customer.ui.theme.Success
import com.inzpire.customer.ui.theme.Warning
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dMMMyyyy: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun ApprovalsScreen(
    approvals: List<CockpitData.Approval>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable(onClick = onBack)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedForeground, modifier = Modifier.size(18.dp))
            Text("Home", color = MutedForeground, fontSize = 13.sp)
        }
        Column {
            Text("Approvals", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Designs, materials and quotations awaiting your sign-off.", color = MutedForeground, fontSize = 13.sp)
        }

        if (approvals.isEmpty()) {
            EmptyState("Nothing to approve", "You're all caught up — new sign-off requests will appear here.")
        }

        approvals.forEach { a ->
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (a.thumbnailUrl != null) {
                            RemoteImage(a.thumbnailUrl, a.title, Modifier.size(width = 96.dp, height = 80.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Crop)
                        } else {
                            Box(Modifier.size(width = 96.dp, height = 80.dp).clip(RoundedCornerShape(10.dp)).background(SkySoft), contentAlignment = Alignment.Center) {
                                Text(a.kind.label.uppercase(), color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(a.kind.label.uppercase(), color = Sky, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(a.title, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Sent ${a.sentAt.format(dMMMyyyy)}", color = MutedForeground, fontSize = 11.sp)
                            when (a.status) {
                                ReviewStatus.APPROVED -> Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(13.dp))
                                    Text("Approved${a.approver?.let { " by $it" } ?: ""}", color = Success, fontSize = 11.sp)
                                }
                                ReviewStatus.CHANGES -> Text("Changes requested", color = Warning, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                ReviewStatus.PENDING -> Unit
                            }
                        }
                    }
                    if (a.status == ReviewStatus.PENDING) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprove(a.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White),
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Approve", fontSize = 14.sp)
                            }
                            OutlinedButton(onClick = { onReject(a.id) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Changes", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
