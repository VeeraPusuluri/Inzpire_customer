package com.inzpire.customer.ui.offers

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CatalogRepository
import com.inzpire.customer.data.model.OfferDto
import com.inzpire.customer.ui.components.BrandGradientBox
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dMMMyyyy: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun OffersScreen(onBack: () -> Unit) {
    val repo = rememberCatalogRepository()
    val state by produceState<List<OfferDto>?>(initialValue = null) {
        value = runCatching { repo.offers() }.getOrDefault(emptyList())
    }

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
            Text("Offers", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Live promos and seasonal pricing.", color = MutedForeground, fontSize = 13.sp)
        }

        val offers = state
        when {
            offers == null -> Text("Loading…", color = MutedForeground, fontSize = 13.sp)
            offers.isEmpty() -> EmptyState("No active offers", "Check back soon for seasonal pricing and promos.")
            else -> offers.forEach { o ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column {
                        BrandGradientBox(Modifier.fillMaxWidth().height(72.dp)) {}
                        Column(Modifier.padding(16.dp)) {
                            Text(o.title, color = Foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            o.body?.let { Text(it, color = MutedForeground, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                            validTill(o.validTill)?.let {
                                Text("Valid till $it", color = Warning, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun validTill(date: String?): String? {
    if (date.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(date).format(dMMMyyyy) }.getOrNull()
}

@Composable
private fun rememberCatalogRepository(): CatalogRepository {
    val holder = androidx.compose.runtime.remember { mutableStateOf(CatalogRepository()) }
    return holder.value
}
