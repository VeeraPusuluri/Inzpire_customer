@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inzpire.customer.ui.enquiry

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.CatalogRepository
import com.inzpire.customer.data.model.ServiceDto
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import kotlinx.coroutines.launch

@Composable
fun EnquiryScreen(
    customerId: String?,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val repo = remember { CatalogRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var services by remember { mutableStateOf<List<ServiceDto>>(emptyList()) }
    var service by remember { mutableStateOf<ServiceDto?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var requirement by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        services = runCatching { repo.services() }.getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable(onClick = onBack)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MutedForeground, modifier = Modifier.size(18.dp))
            Text("Home", color = MutedForeground, fontSize = 13.sp)
        }
        Column {
            Text("New enquiry", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Tell us what you want to build. We'll match a project manager and survey your site.", color = MutedForeground, fontSize = 13.sp)
        }

        // Service dropdown
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = service?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Service") },
                placeholder = { Text("Choose a service") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                services.forEach { s ->
                    DropdownMenuItem(text = { Text(s.name) }, onClick = { service = s; expanded = false })
                }
            }
        }

        OutlinedTextField(
            value = requirement,
            onValueChange = { requirement = it },
            label = { Text("What do you want done?") },
            placeholder = { Text("e.g. 3BHK interior — modular kitchen, wardrobes, false ceiling.") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("City / locality") },
            placeholder = { Text("Hyderabad — Gachibowli") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it.filter { c -> c.isDigit() } },
            label = { Text("Budget (₹)") },
            placeholder = { Text("500000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Site address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        Button(
            onClick = {
                if (customerId == null) {
                    Toast.makeText(context, "Please sign in first.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (requirement.trim().length < 10) {
                    Toast.makeText(context, "Please describe your requirement (min 10 chars).", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                submitting = true
                scope.launch {
                    val result = runCatching {
                        repo.submitEnquiry(
                            customerId = customerId,
                            serviceId = service?.id,
                            requirement = requirement.trim(),
                            location = location,
                            address = address,
                            budget = budget.toDoubleOrNull(),
                        )
                    }
                    submitting = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Enquiry submitted. Our team will reach out shortly.", Toast.LENGTH_LONG).show()
                        onSubmitted()
                    } else {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Could not submit", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
        ) {
            if (submitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Submit enquiry", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
