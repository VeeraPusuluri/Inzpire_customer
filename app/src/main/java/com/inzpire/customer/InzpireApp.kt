package com.inzpire.customer

import android.app.Application
import com.inzpire.customer.data.SupabaseClientProvider
import com.inzpire.customer.notifications.NotificationChannels

class InzpireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClientProvider.init(this)
        NotificationChannels.ensure(this)
    }
}
