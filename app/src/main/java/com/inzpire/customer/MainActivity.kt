package com.inzpire.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inzpire.customer.ui.CustomerViewModel
import com.inzpire.customer.ui.navigation.CustomerApp
import com.inzpire.customer.ui.theme.InzpireCustomerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash on screen so the ~1.4s petal-reveal animation plays fully
        // instead of being cut off the moment the first app frame is drawn.
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        window.decorView.postDelayed({ keepSplash = false }, 1400)
        enableEdgeToEdge()
        setContent {
            InzpireCustomerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val customerViewModel: CustomerViewModel = viewModel()
                    CustomerApp(customerViewModel)
                }
            }
        }
    }
}
