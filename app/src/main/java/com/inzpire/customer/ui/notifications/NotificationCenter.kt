package com.inzpire.customer.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inzpire.customer.data.model.NotificationDto
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Destructive
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import java.time.Duration
import java.time.OffsetDateTime

/**
 * A bell icon with an unread badge that opens a bottom sheet listing the
 * signed-in user's notifications. Self-contained — drop [NotificationBell] into
 * any top bar. Tapping an item marks it read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBell(
    tint: androidx.compose.ui.graphics.Color = Navy,
    vm: NotificationsViewModel = viewModel(),
) {
    val items by vm.items.collectAsState()
    val unread = items.count { !it.isRead }
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { vm.refresh(); open = true }) {
        BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = tint)
        }
    }

    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState, containerColor = Background) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notifications", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unread > 0) TextButton(onClick = { vm.markAllRead() }) { Text("Mark all read") }
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { vm.clearAll() }) { Text("Clear all", color = Destructive) }
                    }
                }
            }
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("You're all caught up", color = MutedForeground, fontSize = 14.sp)
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 480.dp).padding(bottom = 24.dp)) {
                    items(items, key = { it.id }) { n ->
                        NotificationRow(n, onClick = { vm.markRead(n.id) }, onDelete = { vm.delete(n.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationDto, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape)
                .background(if (n.isRead) Border else Sky),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!n.title.isNullOrBlank()) {
                Text(n.title, color = Foreground, fontSize = 14.sp, fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold)
            }
            Text(n.body, color = MutedForeground, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Text(relativeTime(n.createdAt), color = MutedForeground, fontSize = 11.sp)
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Delete", tint = MutedForeground, modifier = Modifier.size(16.dp))
        }
    }
}

private fun relativeTime(iso: String?): String {
    if (iso == null) return ""
    return runCatching {
        val then = OffsetDateTime.parse(iso)
        val d = Duration.between(then.toInstant(), OffsetDateTime.now().toInstant())
        when {
            d.toMinutes() < 1 -> "now"
            d.toMinutes() < 60 -> "${d.toMinutes()}m"
            d.toHours() < 24 -> "${d.toHours()}h"
            d.toDays() < 7 -> "${d.toDays()}d"
            else -> "${d.toDays() / 7}w"
        }
    }.getOrDefault("")
}
