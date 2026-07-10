package com.inzpire.customer.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.inzpire.customer.R

/** Shown while the Supabase session is initializing — just the brand logo on white. */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.inzpire_decor_logo),
            contentDescription = "Inzpire Decor",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.66f)
                .padding(24.dp),
        )
    }
}
