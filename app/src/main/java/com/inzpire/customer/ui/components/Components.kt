package com.inzpire.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.NavyDeep
import com.inzpire.customer.ui.theme.SkySoft

/** Remote image (Unsplash seed URLs) with a neutral placeholder background while loading. */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // While loading — or when the URL is missing/broken — show a branded image
    // placeholder instead of an empty box, so material & design cards always read
    // as image tiles.
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.background(SkySoft),
        loading = { ImagePlaceholder() },
        error = { ImagePlaceholder() },
    )
}

@Composable
private fun ImagePlaceholder() {
    Box(Modifier.fillMaxSize().background(SkySoft), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Outlined.Image,
            contentDescription = null,
            tint = Navy.copy(alpha = 0.30f),
            modifier = Modifier.size(30.dp),
        )
    }
}

/**
 * Material 3 elevated card — the web's `.surface-card`. Pass [onClick] for a genuinely
 * clickable card (MD3 ripple + a small pressed-elevation lift); omit it for a static card.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    val elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp)
    if (onClick != null) {
        ElevatedCard(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    } else {
        ElevatedCard(modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    }
}

/** Navy brand gradient block (matches `.brand-gradient`). */
@Composable
fun BrandGradientBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.background(Brush.linearGradient(listOf(NavyDeep, Navy)))) { content() }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        trailing?.invoke()
    }
}

/** Small status pill. */
@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Rounded progress track + fill. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = Color.White.copy(alpha = 0.2f),
    fillColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(fillColor),
        )
    }
}

/** Centered empty-state card used when a live list has no rows yet. */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                color = MutedForeground,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Row of muted label + value used in detail lists. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueContent: @Composable (RowScope.() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Foreground.copy(alpha = 0.6f), fontSize = 13.sp)
        if (valueContent != null) valueContent() else Text(value, color = Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
