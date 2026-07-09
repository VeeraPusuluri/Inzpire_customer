package com.inzpire.customer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.inzpire.customer.ui.profile.ProfileScreen
import com.inzpire.customer.ui.shell.CustomerScaffold
import com.inzpire.customer.ui.splash.SplashScreen
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun CustomerApp(customerViewModel: CustomerViewModel) {
    val sessionStatus by customerViewModel.sessionStatus.collectAsState()

    when (sessionStatus) {
        is SessionStatus.Initializing -> SplashScreen()
        is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure ->
            AuthScreen(onAuthenticated = { customerViewModel.onSignedIn() })
        is SessionStatus.Authenticated -> AuthenticatedApp(customerViewModel)
    }
}

@Composable
private fun AuthenticatedApp(customerViewModel: CustomerViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val profile by customerViewModel.profile.collectAsState()
    val cockpit by customerViewModel.cockpit.collectAsState()
    val messages by customerViewModel.messages.collectAsState()
    val live by customerViewModel.live.collectAsState()

    CustomerScaffold(
        currentRoute = currentRoute,
        profile = profile,
        onNavigate = { route -> navigateToTab(navController, route) },
        onSignOut = { customerViewModel.signOut() },
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
                    onOpenApprovals = { navController.navigate(CustomerDestinations.APPROVALS) },
                    onOpenDesigns = { navigateToTab(navController, CustomerDestinations.DESIGNS) },
                    onOpenMaterials = { navigateToTab(navController, CustomerDestinations.MATERIALS) },
                    onOpenPayments = { navigateToTab(navController, CustomerDestinations.PAYMENTS) },
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
                    onRequestChanges = { customerViewModel.requestDesignChanges(it) },
                )
            }
            composable(CustomerDestinations.MATERIALS) { MaterialsScreen(materials = cockpit.materials) }
            composable(CustomerDestinations.PAYMENTS) {
                PaymentsScreen(
                    project = cockpit.project,
                    payments = cockpit.payments,
                    onPay = { customerViewModel.payMilestone(it) },
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
