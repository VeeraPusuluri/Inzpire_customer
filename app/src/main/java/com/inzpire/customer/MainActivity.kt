package com.inzpire.customer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inzpire.customer.ui.CustomerViewModel
import com.inzpire.customer.ui.navigation.CustomerApp
import com.inzpire.customer.ui.theme.InzpireCustomerTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    // Deep-link carried by a tapped push notification (PushMessagingService puts it in the
    // launch intent as "notification_link"). Observed by Compose to route to the right screen.
    private var pendingLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLink = intent?.getStringExtra(EXTRA_NOTIFICATION_LINK)
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            InzpireCustomerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val customerViewModel: CustomerViewModel = viewModel()
                    CustomerApp(
                        customerViewModel = customerViewModel,
                        deepLink = pendingLink,
                        onDeepLinkHandled = { pendingLink = null },
                    )
                }
            }
        }
    }

    // App already running when the notification is tapped — Android delivers the new intent here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_NOTIFICATION_LINK)?.let { pendingLink = it }
    }

    /** Android 13+ requires an explicit runtime grant to post notifications. */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_NOTIFICATION_LINK = "notification_link"
    }
}
