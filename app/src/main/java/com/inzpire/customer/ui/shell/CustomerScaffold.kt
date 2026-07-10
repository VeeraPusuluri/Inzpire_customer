package com.inzpire.customer.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inzpire.customer.ui.components.FloatingPillNav
import com.inzpire.customer.ui.components.PillTab
import com.inzpire.customer.ui.navigation.CustomerDestinations

private val NAV_ITEMS = listOf(
    PillTab(CustomerDestinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    PillTab(CustomerDestinations.DESIGNS, "Designs", Icons.Filled.Image, Icons.Outlined.Image),
    PillTab(CustomerDestinations.MATERIALS, "Materials", Icons.Filled.Palette, Icons.Outlined.Palette),
    PillTab(CustomerDestinations.PAYMENTS, "Pay", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PillTab(CustomerDestinations.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

/** Customer chrome: no top bar (logout lives in Profile), 5-tab floating pill bottom nav. */
@Composable
fun CustomerScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Inzpire top bar removed — keep the space it occupied empty so content
            // doesn't sit under the status bar (status bar inset + a small breathing gap).
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(24.dp))
            }
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
