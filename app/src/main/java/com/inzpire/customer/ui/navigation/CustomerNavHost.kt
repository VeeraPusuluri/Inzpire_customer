package com.inzpire.customer.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inzpire.customer.ui.CustomerViewModel
import com.inzpire.customer.ui.approvals.ApprovalsScreen
import com.inzpire.customer.ui.auth.AuthScreen
import com.inzpire.customer.ui.chat.ChatScreen
import com.inzpire.customer.ui.designs.DesignsScreen
import com.inzpire.customer.ui.documents.DocumentsScreen
import com.inzpire.customer.ui.enquiry.EnquiryScreen
import com.inzpire.customer.ui.home.HomeScreen
import com.inzpire.customer.ui.materials.MaterialsScreen
import com.inzpire.customer.ui.offers.OffersScreen
import com.inzpire.customer.ui.payments.PaymentsScreen
import com.inzpire.customer.ui.payouts.PayoutsScreen
import com.inzpire.customer.ui.profile.ProfileScreen
import com.inzpire.customer.ui.shell.CustomerScaffold
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun CustomerApp(
    customerViewModel: CustomerViewModel,
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val sessionStatus by customerViewModel.sessionStatus.collectAsState()

    when (sessionStatus) {
        // While the Supabase session resolves (brief), show a plain background — no splash.
        is SessionStatus.Initializing -> Box(Modifier.fillMaxSize())
        is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure ->
            AuthScreen(onAuthenticated = { customerViewModel.onSignedIn() })
        is SessionStatus.Authenticated -> AuthenticatedApp(customerViewModel, deepLink, onDeepLinkHandled)
    }
}

/** Maps a notification link (e.g. "/designs", "/app/chat") to a nav route, or null if unmapped. */
private fun routeForLink(link: String): String? = when {
    link.startsWith("/designs") -> CustomerDestinations.DESIGNS
    link.startsWith("/materials") -> CustomerDestinations.MATERIALS
    link.startsWith("/approvals") -> CustomerDestinations.APPROVALS
    link.startsWith("/payments") -> CustomerDestinations.PAYMENTS
    link.startsWith("/payouts") -> CustomerDestinations.PAYOUTS
    link.startsWith("/wallet") -> CustomerDestinations.PAYOUTS
    link.startsWith("/documents") -> CustomerDestinations.DOCUMENTS
    link.startsWith("/app/chat") -> CustomerDestinations.CHAT
    link.startsWith("/chat") -> CustomerDestinations.CHAT
    link.startsWith("/offers") -> CustomerDestinations.OFFERS
    link.startsWith("/enquiry") -> CustomerDestinations.ENQUIRY
    link.startsWith("/app") -> CustomerDestinations.HOME
    else -> null
}

@Composable
private fun AuthenticatedApp(
    customerViewModel: CustomerViewModel,
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val profile by customerViewModel.profile.collectAsState()
    val cockpit by customerViewModel.cockpit.collectAsState()
    val cockpits by customerViewModel.cockpits.collectAsState()
    val cockpitLoaded by customerViewModel.cockpitLoaded.collectAsState()
    val selectedProjectIndex by customerViewModel.selectedProjectIndex.collectAsState()
    val messages by customerViewModel.messages.collectAsState()
    val live by customerViewModel.live.collectAsState()
    val payouts by customerViewModel.payouts.collectAsState()
    val pendingBonuses by customerViewModel.pendingBonuses.collectAsState()
    val payoutsLoading by customerViewModel.payoutsLoading.collectAsState()

    // Surface one-shot feedback (approve / accept / request change / pay …) as a toast.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        customerViewModel.toasts.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Route a tapped push notification to its screen once the nav host is composed.
    LaunchedEffect(deepLink) {
        deepLink?.let { link ->
            routeForLink(link)?.let { route -> navController.navigate(route) }
            onDeepLinkHandled()
        }
    }

    CustomerScaffold(
        currentRoute = currentRoute,
        onNavigate = { route -> navigateToTab(navController, route) },
    ) { paddingModifier ->
        NavHost(
            navController = navController,
            startDestination = CustomerDestinations.HOME,
            modifier = Modifier.then(paddingModifier),
        ) {
            composable(CustomerDestinations.HOME) {
                HomeScreen(
                    cockpit = cockpit,
                    customerName = profile?.name,
                    loaded = cockpitLoaded,
                    onOpenLink = { link -> routeForLink(link)?.let { navController.navigate(it) } },
                    cockpits = cockpits,
                    selectedProjectIndex = selectedProjectIndex,
                    onSelectProject = { customerViewModel.selectProject(it) },
                    onOpenApprovals = { navController.navigate(CustomerDestinations.APPROVALS) },
                    onOpenDesigns = { navigateToTab(navController, CustomerDestinations.DESIGNS) },
                    onOpenMaterials = { navigateToTab(navController, CustomerDestinations.MATERIALS) },
                    onOpenPayments = { navigateToTab(navController, CustomerDestinations.PAYMENTS) },
                    onOpenPayouts = { navController.navigate(CustomerDestinations.PAYOUTS) },
                    onOpenDocuments = { navController.navigate(CustomerDestinations.DOCUMENTS) },
                    onOpenChat = { navController.navigate(CustomerDestinations.CHAT) },
                    onOpenOffers = { navController.navigate(CustomerDestinations.OFFERS) },
                    onOpenEnquiry = { navController.navigate(CustomerDestinations.ENQUIRY) },
                )
            }
            composable(CustomerDestinations.DESIGNS) {
                DesignsScreen(
                    designs = cockpit.designs,
                    onApprove = { customerViewModel.approveDesign(it) },
                    onRequestChanges = { id, note -> customerViewModel.requestDesignChanges(id, note) },
                )
            }
            composable(CustomerDestinations.MATERIALS) {
                MaterialsScreen(
                    materials = cockpit.materials,
                    onAccept = { customerViewModel.acceptMaterial(it) },
                    onRequestChange = { id, note -> customerViewModel.requestMaterialChange(id, note) },
                )
            }
            composable(CustomerDestinations.PAYMENTS) {
                PaymentsScreen(
                    project = cockpit.project,
                    payments = cockpit.payments,
                    onPay = { customerViewModel.payMilestone(it) },
                )
            }
            composable(CustomerDestinations.PAYOUTS) {
                LaunchedEffect(Unit) { customerViewModel.loadPayouts() }
                PayoutsScreen(
                    payouts = payouts,
                    pendingBonuses = pendingBonuses,
                    hasPayoutDetails = profile?.hasPayoutDetails == true,
                    loading = payoutsLoading,
                    onBack = { navController.popBackStack() },
                    onEditPayoutMethod = { navigateToTab(navController, CustomerDestinations.PROFILE) },
                )
            }
            composable(CustomerDestinations.PROFILE) { ProfileScreen(customerViewModel = customerViewModel) }
            composable(CustomerDestinations.APPROVALS) {
                ApprovalsScreen(
                    approvals = cockpit.approvals,
                    onApprove = { customerViewModel.approveApproval(it) },
                    onReject = { customerViewModel.rejectApproval(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(CustomerDestinations.DOCUMENTS) {
                DocumentsScreen(documents = cockpit.documents, onBack = { navController.popBackStack() })
            }
            composable(CustomerDestinations.CHAT) {
                LaunchedEffect(Unit) { customerViewModel.loadMessages() }
                ChatScreen(
                    messages = messages,
                    canSend = live,
                    onSend = { customerViewModel.sendMessage(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(CustomerDestinations.OFFERS) {
                OffersScreen(onBack = { navController.popBackStack() })
            }
            composable(CustomerDestinations.ENQUIRY) {
                EnquiryScreen(
                    customerId = customerViewModel.currentUserId,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
