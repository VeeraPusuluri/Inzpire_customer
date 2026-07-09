@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inzpire.customer.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.model.Profile
import com.inzpire.customer.ui.components.FloatingPillNav
import com.inzpire.customer.ui.components.PillTab
import com.inzpire.customer.ui.navigation.CustomerDestinations
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy

private val NAV_ITEMS = listOf(
    PillTab(CustomerDestinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    PillTab(CustomerDestinations.DESIGNS, "Designs", Icons.Filled.Image, Icons.Outlined.Image),
    PillTab(CustomerDestinations.MATERIALS, "Materials", Icons.Filled.Palette, Icons.Outlined.Palette),
    PillTab(CustomerDestinations.PAYMENTS, "Pay", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PillTab(CustomerDestinations.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

/** Mirrors the web `AppShell` customer chrome: brand + signed-in user top bar, 5-tab floating pill bottom nav. */
@Composable
fun CustomerScaffold(
    currentRoute: String?,
    profile: Profile?,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Navy,
                    actionIconContentColor = Navy,
                ),
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text("Inzpire", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                        Text(
                            profile?.name ?: "Customer",
                            fontSize = 11.sp,
                            color = MutedForeground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        bottomBar = {
            FloatingPillNav(
                tabs = NAV_ITEMS,
                selectedRoute = currentRoute,
                onSelect = { tab -> onNavigate(tab.route) },
            )
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}
