package com.inzpire.customer.ui.designs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
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
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Success
import com.inzpire.customer.ui.theme.Warning

@Composable
fun DesignsScreen(
    designs: List<CockpitData.Design>,
    onApprove: (String) -> Unit,
    onRequestChanges: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Designs", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Moodboards, 2D layouts and 3D renders by room.", color = MutedForeground, fontSize = 13.sp)
        }

        if (designs.isEmpty()) {
            EmptyState("No designs yet", "Your designer will publish moodboards and renders here.")
        }

        designs.forEach { d ->
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    Box {
                        RemoteImage(d.imageUrl, d.title, Modifier.fillMaxWidth().height(190.dp), ContentScale.Crop)
                        Box(
                            Modifier.align(Alignment.TopStart).padding(8.dp)
                                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) { Text(d.type.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp)
                                .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.95f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) { Text(d.version, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text(d.room.uppercase(), color = MutedForeground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(d.title, color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        ReviewStatusRow(d.status, Modifier.padding(top = 4.dp))

                        if (d.status == ReviewStatus.PENDING) {
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApprove(d.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White),
                                ) { Text("Approve", fontSize = 14.sp) }
                                OutlinedButton(onClick = { onRequestChanges(d.id) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Comment", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStatusRow(status: ReviewStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        ReviewStatus.APPROVED -> Success to "Approved"
        ReviewStatus.PENDING -> Warning to "Pending"
        ReviewStatus.CHANGES -> Warning to "Changes requested"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (status == ReviewStatus.APPROVED) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null, tint = color, modifier = Modifier.size(14.dp),
        )
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
