package com.inzpire.customer.ui.designs

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitData.ReviewStatus
import com.inzpire.customer.data.FallbackImages
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
    onRequestChanges: (id: String, note: String) -> Unit,
) {
    // Card tapped for its detail sheet, and the design a change request is being drafted for.
    var detail by remember { mutableStateOf<CockpitData.Design?>(null) }
    var requestFor by remember { mutableStateOf<CockpitData.Design?>(null) }

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
            Text("Moodboards, 2D layouts and 3D renders by room. Tap a card to view it full size.", color = MutedForeground, fontSize = 13.sp)
        }

        if (designs.isEmpty()) {
            EmptyState("No designs yet", "Your designer will publish moodboards and renders here.")
        }

        designs.forEach { d ->
            SurfaceCard(Modifier.fillMaxWidth(), onClick = { detail = d }) {
                Column {
                    Box {
                        RemoteImage(d.imageUrl.ifBlank { FallbackImages.forDesign(d.id) }, d.title, Modifier.fillMaxWidth().height(190.dp), ContentScale.Crop)
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
                                OutlinedButton(onClick = { requestFor = d }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Request change", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { d ->
        DesignDetailDialog(
            design = d,
            onDismiss = { detail = null },
            onApprove = {
                onApprove(d.id)
                detail = null
            },
            onRequestChanges = {
                detail = null
                requestFor = d
            },
        )
    }

    requestFor?.let { design ->
        RequestChangeDialog(
            design = design,
            onDismiss = { requestFor = null },
            onSubmit = { note ->
                onRequestChanges(design.id, note)
                requestFor = null
            },
        )
    }
}

@Composable
private fun DesignDetailDialog(
    design: CockpitData.Design,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onRequestChanges: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MutedForeground) }
        },
        title = { Text(design.title, color = Navy, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val heroUrl = design.imageUrl.ifBlank { FallbackImages.forDesign(design.id) }
                RemoteImage(
                    heroUrl,
                    design.title,
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { uriHandler.openUri(heroUrl) },
                    ContentScale.Crop,
                )
                Text("${design.room.uppercase()} · ${design.type.label} · ${design.version}", color = MutedForeground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                ReviewStatusRow(design.status)

                if (design.status == ReviewStatus.PENDING) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White),
                    ) { Text("Approve", fontSize = 14.sp) }
                    OutlinedButton(onClick = onRequestChanges, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Request a change", fontSize = 14.sp)
                    }
                }
            }
        },
        containerColor = Color.White,
    )
}

@Composable
private fun RequestChangeDialog(
    design: CockpitData.Design,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (note.trim().length < 5) {
                    Toast.makeText(context, "Add a short note about the change you'd like.", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onSubmit(note.trim())
            }) { Text("Send request", color = Navy, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedForeground) }
        },
        title = { Text("Request a change", color = Navy, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${design.room} · ${design.title}", color = MutedForeground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Tell your designer what to change — layout, colours, materials, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        containerColor = Color.White,
    )
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
