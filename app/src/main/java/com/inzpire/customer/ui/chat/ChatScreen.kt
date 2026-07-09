package com.inzpire.customer.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.ui.ChatMessage
import com.inzpire.customer.ui.components.EmptyState
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    canSend: Boolean,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable(onClick = onBack)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedForeground, modifier = Modifier.size(18.dp))
                Text("Home", color = MutedForeground, fontSize = 13.sp)
            }
            Text("Chat", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("Talk to your designer and project manager.", color = MutedForeground, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                EmptyState("No messages yet", "Say hello — your team replies here.")
            }
            messages.forEach { m -> MessageBubble(m) }
            Box(Modifier.padding(bottom = 8.dp))
        }

        // Composer
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (canSend) "Type a message…" else "Sign in to your project to chat") },
                enabled = canSend,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardActions = KeyboardActions(onSend = {
                    if (draft.isNotBlank()) { onSend(draft); draft = "" }
                }),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend && draft.isNotBlank()) Navy else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canSend && draft.isNotBlank()) { onSend(draft); draft = "" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (canSend && draft.isNotBlank()) Color.White else MutedForeground, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (m.fromMe) Navy else Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!m.fromMe) {
                Text(
                    "${m.senderName}${if (m.senderRole.isNotBlank()) " · ${m.senderRole}" else ""}",
                    color = Sky, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Text(m.body, color = if (m.fromMe) Color.White else Foreground, fontSize = 14.sp)
            if (m.at.isNotBlank()) {
                Text(m.at, color = if (m.fromMe) Color.White.copy(alpha = 0.7f) else MutedForeground, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
