package com.inzpire.customer.ui.materials

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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
import com.inzpire.customer.data.CockpitData.MaterialStatus
import com.inzpire.customer.data.FallbackImages
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.components.RemoteImage
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.SkySoft
import com.inzpire.customer.ui.theme.Success

@Composable
fun MaterialsScreen(
    materials: List<CockpitData.Material>,
    onAccept: (materialId: String) -> Unit,
    onRequestChange: (materialId: String, note: String) -> Unit,
) {
    // The material tapped for its detail sheet (null = sheet closed).
    var selected by remember { mutableStateOf<CockpitData.Material?>(null) }
    val rooms = materials.map { it.room }.distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text("Materials", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Finishes, swatches and hardware selected for your home. Tap any item for details.", color = MutedForeground, fontSize = 13.sp)
        }

        if (materials.isEmpty()) {
            EmptyState("No materials yet", "Selections will appear here once your designer shares them.")
        }

        rooms.forEach { room ->
            Column {
                Text(room, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                val items = materials.filter { it.room == room }
                items.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { m -> MaterialCard(m, Modifier.weight(1f), onClick = { selected = m }) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    selected?.let { mat ->
        MaterialDetailDialog(
            material = mat,
            onDismiss = { selected = null },
            onAccept = {
                onAccept(mat.id)
                selected = null
            },
            onSubmit = { note ->
                onRequestChange(mat.id, note)
                selected = null
            },
        )
    }
}

@Composable
private fun MaterialCard(m: CockpitData.Material, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SurfaceCard(modifier, onClick = onClick) {
        Column {
            RemoteImage(
                m.swatchUrl.ifBlank { FallbackImages.forMaterial(m.id) },
                m.name,
                Modifier.fillMaxWidth().aspectRatio(1f),
                ContentScale.Crop,
            )
            Column(Modifier.padding(10.dp)) {
                Text(m.category.uppercase(), color = MutedForeground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(m.name, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(m.make, color = MutedForeground, fontSize = 11.sp, maxLines = 1)
                MaterialStatusRow(m.status, Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun MaterialStatusRow(status: MaterialStatus, modifier: Modifier = Modifier) {
    val (icon, color, label) = when (status) {
        MaterialStatus.LOCKED -> Triple(Icons.Filled.Lock, Navy, "Locked")
        MaterialStatus.SELECTED -> Triple(Icons.Filled.CheckCircle, Success, "Selected")
        MaterialStatus.SUGGESTED -> Triple(Icons.Filled.AutoAwesome, Sky, "Suggested")
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Anything that isn't clearly a document is treated as a viewable image. */
private fun isDocUrl(url: String): Boolean {
    val u = url.substringBefore('?').lowercase()
    return listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".csv", ".txt")
        .any { u.endsWith(it) }
}

@Composable
private fun MaterialDetailDialog(
    material: CockpitData.Material,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var note by remember { mutableStateOf("") }

    val images = material.mediaUrls.filterNot(::isDocUrl)
    val files = material.mediaUrls.filter(::isDocUrl)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MutedForeground) }
        },
        title = { Text(material.name, color = Navy, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Primary image — tap to open full size in the browser. Falls back to the
                // card's swatch (or a curated stock image) when the admin attached no photos.
                val hero = images.firstOrNull()
                    ?: material.swatchUrl.ifBlank { FallbackImages.forMaterial(material.id) }
                RemoteImage(
                    hero,
                    material.name,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { uriHandler.openUri(hero) },
                    ContentScale.Crop,
                )

                // Every image/file the admin attached — tap any to view.
                if (material.mediaUrls.size > 1 || files.isNotEmpty()) {
                    Text("Attachments", color = Foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        images.forEach { url ->
                            RemoteImage(
                                url,
                                null,
                                Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)).clickable { uriHandler.openUri(url) },
                                ContentScale.Crop,
                            )
                        }
                        files.forEach { url ->
                            FileChip(onClick = { uriHandler.openUri(url) })
                        }
                    }
                }

                Text("${material.category.uppercase()} · ${material.room}", color = MutedForeground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (material.make.isNotBlank()) {
                    Text("Make · ${material.make}", color = Foreground, fontSize = 13.sp)
                }
                MaterialStatusRow(material.status)

                // Accept (final sign-off) — available until the material is locked.
                if (material.status != MaterialStatus.LOCKED) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Accept material", fontSize = 14.sp)
                    }
                }

                Text(
                    "Request a change or modification",
                    color = Foreground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. Prefer a darker walnut shade, or a matte finish.") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedButton(
                    onClick = {
                        if (note.trim().length < 5) {
                            Toast.makeText(context, "Add a short note about the change you'd like.", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        onSubmit(note.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send change request", color = Navy, fontSize = 14.sp) }
            }
        },
        containerColor = Color.White,
    )
}

@Composable
private fun FileChip(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SkySoft)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Description, contentDescription = "Open file", tint = Navy, modifier = Modifier.size(22.dp))
        Text("File", color = Navy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}
