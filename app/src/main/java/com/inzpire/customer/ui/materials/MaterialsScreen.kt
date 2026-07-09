package com.inzpire.customer.ui.materials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import com.inzpire.customer.data.CockpitData.MaterialStatus
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
fun MaterialsScreen(materials: List<CockpitData.Material>) {
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
            Text("Finishes, swatches and hardware selected for your home.", color = MutedForeground, fontSize = 13.sp)
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
                        pair.forEach { m -> MaterialCard(m, Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(m: CockpitData.Material, modifier: Modifier = Modifier) {
    SurfaceCard(modifier) {
        Column {
            if (m.swatchUrl.isNotBlank()) {
                RemoteImage(m.swatchUrl, m.name, Modifier.fillMaxWidth().aspectRatio(1f), ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(SkySoft), contentAlignment = Alignment.Center) {
                    Text(m.category.uppercase(), color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(m.category.uppercase(), color = MutedForeground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(m.name, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(m.make, color = MutedForeground, fontSize = 11.sp, maxLines = 1)
                val (icon, color, label) = when (m.status) {
                    MaterialStatus.LOCKED -> Triple(Icons.Filled.Lock, Navy, "Locked")
                    MaterialStatus.SELECTED -> Triple(Icons.Filled.CheckCircle, Success, "Selected")
                    MaterialStatus.SUGGESTED -> Triple(Icons.Filled.AutoAwesome, Sky, "Suggested")
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
