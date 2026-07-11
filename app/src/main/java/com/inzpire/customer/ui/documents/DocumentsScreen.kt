package com.inzpire.customer.ui.documents

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.SkySoft
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dMMMyyyy: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun DocumentsScreen(documents: List<CockpitData.Document>, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
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
            Text("Documents", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Contract, invoices, BOQs and drawings.", color = MutedForeground, fontSize = 13.sp)
        }

        if (documents.isEmpty()) {
            EmptyState("No documents yet", "Your contract, invoices and drawings will show up here.")
        } else {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    documents.forEachIndexed { i, d ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(if (d.url.isNotBlank()) Modifier.clickable { uriHandler.openUri(d.url) } else Modifier)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SkySoft), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(d.name, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${d.type} · ${d.date.format(dMMMyyyy)}", color = MutedForeground, fontSize = 11.sp)
                            }
                            Icon(Icons.Filled.Download, contentDescription = "Download", tint = Sky, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
