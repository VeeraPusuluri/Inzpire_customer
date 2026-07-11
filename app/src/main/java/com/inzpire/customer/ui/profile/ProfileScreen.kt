@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inzpire.customer.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.model.ProfilePatch
import com.inzpire.customer.ui.CustomerViewModel
import com.inzpire.customer.ui.components.RemoteImage
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Destructive
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.SkySoft
import com.inzpire.customer.ui.theme.Success
import com.inzpire.customer.ui.theme.Warning
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(customerViewModel: CustomerViewModel) {
    val profile by customerViewModel.profile.collectAsState()
    val roles by customerViewModel.roles.collectAsState()
    val identity by customerViewModel.identity.collectAsState()

    // Re-seed the form whenever the loaded profile changes.
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var location by remember(profile) { mutableStateOf(profile?.location ?: "") }
    var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var upiId by remember(profile) { mutableStateOf(profile?.upiId ?: "") }
    var bankName by remember(profile) { mutableStateOf(profile?.bankAcctName ?: "") }
    var bankNo by remember(profile) { mutableStateOf(profile?.bankAcctNo ?: "") }
    // Seeded from the saved number so existing users don't have to retype to save.
    var confirmBankNo by remember(profile) { mutableStateOf(profile?.bankAcctNo ?: "") }
    var bankIfsc by remember(profile) { mutableStateOf(profile?.bankIfsc ?: "") }

    var saving by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Identity header — carded so it sits consistently with the sections below.
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        RemoteImage(profile?.photoUrl, profile?.name, Modifier.size(64.dp).clip(CircleShape), ContentScale.Crop)
                    } else {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(SkySoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile?.name?.ifBlank { "Your profile" } ?: "Your profile", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RolePill((roles.firstOrNull() ?: "customer").replace('_', ' '))
                            KycPill(profile?.kycStatus)
                        }
                    }
                }
            }

            // Account & verification
            SectionCard(
                title = "Account",
                subtitle = "Confirm your email and phone so we can reach you about approvals and payments.",
            ) {
                VerificationRow(
                    icon = Icons.Filled.Email,
                    label = "Email",
                    value = identity?.email ?: profile?.email,
                    verified = identity?.emailVerified == true,
                )
                VerificationRow(
                    icon = Icons.Filled.Phone,
                    label = "Phone",
                    value = identity?.phone ?: profile?.phone,
                    verified = identity?.phoneVerified == true,
                    onVerify = if (identity?.phoneVerified == true) null else ({ showPhoneDialog = true }),
                )
            }

            // Personal
            SectionCard("Personal") {
                ProfileField(name, { name = it.take(60) }, "Name", Icons.Filled.Person)
                ProfileField(phone, { phone = sanitizePhone(it) }, "Phone", Icons.Filled.Phone, KeyboardType.Phone)
                ProfileField(location, { location = it.take(60) }, "City / locality", Icons.Filled.LocationOn)
                ProfileField(address, { address = it.take(120) }, "Address", Icons.Outlined.Home, singleLine = false)
            }

            // Payouts (missing from the app until now — matches the web profile page)
            SectionCard(
                title = "Payouts",
                subtitle = "Used for referral & refund payouts. Money is sent only to the linked account.",
            ) {
                ProfileField(upiId, { upiId = sanitizeUpi(it) }, "UPI ID", Icons.Filled.AccountBalanceWallet, KeyboardType.Email, placeholder = "name@upi")
                ProfileField(bankName, { bankName = it.take(60) }, "Account holder", Icons.Filled.Badge)
                ProfileField(bankNo, { bankNo = accountDigits(it) }, "Account number", Icons.Filled.Numbers, KeyboardType.Number)
                ProfileField(confirmBankNo, { confirmBankNo = accountDigits(it) }, "Re-enter account number", Icons.Filled.Numbers, KeyboardType.Number)
                if (confirmBankNo.isNotBlank() && confirmBankNo != bankNo) {
                    Text("Account numbers don't match", color = Destructive, fontSize = 12.sp)
                }
                ProfileField(bankIfsc, { bankIfsc = sanitizeIfsc(it) }, "IFSC", Icons.Filled.Pin, placeholder = "ABCD0123456")
            }

            Button(
                onClick = saveClick@{
                    if (bankNo.isNotBlank() && confirmBankNo != bankNo) {
                        scope.launch { snackbarHostState.showSnackbar("Account numbers don't match") }
                        return@saveClick
                    }
                    saving = true
                    customerViewModel.updateProfile(
                        ProfilePatch(
                            name = name, phone = phone, location = location, address = address,
                            upiId = upiId.ifBlank { null }, bankAcctName = bankName.ifBlank { null },
                            bankAcctNo = bankNo.ifBlank { null }, bankIfsc = bankIfsc.ifBlank { null },
                        ),
                    ) { result ->
                        saving = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (result.isSuccess) "Profile updated" else (result.exceptionOrNull()?.message ?: "Failed to save"),
                            )
                        }
                    }
                },
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save changes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            // Log out — moved here from the (now removed) top bar.
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Destructive.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Destructive),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Log out", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        if (showPhoneDialog) {
            PhoneVerifyDialog(
                initialPhone = identity?.phone ?: profile?.phone ?: "",
                onDismiss = { showPhoneDialog = false },
                onSuccess = {
                    showPhoneDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Phone number verified") }
                },
                onSendOtp = { p, cb -> customerViewModel.sendPhoneOtp(p, cb) },
                onVerify = { p, t, cb -> customerViewModel.verifyPhoneOtp(p, t, cb) },
            )
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutConfirm = false
                        customerViewModel.signOut()
                    }) { Text("Log out", color = Destructive, fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel", color = MutedForeground) }
                },
                title = { Text("Log out?") },
                text = { Text("You'll need to sign in again to access your project.") },
            )
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title.uppercase(), color = MutedForeground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = MutedForeground, fontSize = 12.sp) }
            content()
        }
    }
}

@Composable
private fun RolePill(role: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(SkySoft).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(role.replaceFirstChar { it.uppercase() }, color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun KycPill(status: String?) {
    val (color, label, icon) = when (status) {
        "verified" -> Triple(Success, "KYC verified", Icons.Filled.Verified)
        "rejected" -> Triple(Destructive, "KYC rejected", Icons.Filled.Cancel)
        else -> Triple(Warning, "KYC pending", Icons.Filled.HourglassBottom)
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Green "Verified" / amber "Unverified" chip used beside email & phone. */
@Composable
private fun VerifiedPill(verified: Boolean) {
    val (color, label, icon) = if (verified) {
        Triple(Success, "Verified", Icons.Filled.Verified)
    } else {
        Triple(Warning, "Unverified", Icons.Filled.ErrorOutline)
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VerificationRow(
    icon: ImageVector,
    label: String,
    value: String?,
    verified: Boolean,
    onVerify: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = MutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value?.ifBlank { "—" } ?: "—", color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        VerifiedPill(verified)
        if (onVerify != null) {
            TextButton(onClick = onVerify) {
                Text("Verify", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

/** Two-step dialog: enter/confirm the phone → receive an SMS OTP → enter the code to verify. */
@Composable
private fun PhoneVerifyDialog(
    initialPhone: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onSendOtp: (String, (Result<Unit>) -> Unit) -> Unit,
    onVerify: (String, String, (Result<Unit>) -> Unit) -> Unit,
) {
    var phone by remember { mutableStateOf(initialPhone) }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            if (!codeSent) {
                TextButton(
                    enabled = !busy && phone.count { it.isDigit() } >= 7,
                    onClick = {
                        busy = true
                        error = null
                        onSendOtp(phone) { r ->
                            busy = false
                            if (r.isSuccess) {
                                codeSent = true
                                info = "Code sent to $phone"
                            } else {
                                error = r.exceptionOrNull()?.message ?: "Couldn't send the code"
                            }
                        }
                    },
                ) { Text(if (busy) "Sending…" else "Send code") }
            } else {
                TextButton(
                    enabled = !busy && code.trim().length >= 4,
                    onClick = {
                        busy = true
                        error = null
                        onVerify(phone, code) { r ->
                            busy = false
                            if (r.isSuccess) onSuccess() else error = r.exceptionOrNull()?.message ?: "Invalid code"
                        }
                    },
                ) { Text(if (busy) "Verifying…" else "Verify") }
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Verify phone number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("We'll text a 6-digit code to confirm this number.", color = MutedForeground, fontSize = 13.sp)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    enabled = !codeSent && !busy,
                    label = { Text("Phone (with country code)") },
                    placeholder = { Text("+9198XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (codeSent) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { new -> code = new.filter { it.isDigit() }.take(6) },
                        enabled = !busy,
                        label = { Text("6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                info?.let { Text(it, color = Success, fontSize = 12.sp) }
                error?.let { Text(it, color = Destructive, fontSize = 12.sp) }
            }
        },
    )
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp)) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Navy,
            unfocusedBorderColor = Border,
            focusedLabelColor = Navy,
            cursorColor = Navy,
            focusedTextColor = Foreground,
            unfocusedTextColor = Foreground,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---- input sanitizers: keep each field to a valid character set + sensible max length ----
/** Digits only, capped at 18 (longest Indian bank account number). */
private fun accountDigits(input: String): String = input.filter { it.isDigit() }.take(18)

/** Allows a leading + and digits, e.g. +9198XXXXXXXX. */
private fun sanitizePhone(input: String): String = input.filter { it.isDigit() || it == '+' }.take(15)

/** UPI IDs have no spaces (name@bank); cap length defensively. */
private fun sanitizeUpi(input: String): String = input.filter { !it.isWhitespace() }.take(50)

/** IFSC is 11 uppercase alphanumerics. */
private fun sanitizeIfsc(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }.take(11)
