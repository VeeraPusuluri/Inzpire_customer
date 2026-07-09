package com.inzpire.customer

import android.app.Application
import com.inzpire.customer.data.SupabaseClientProvider

class InzpireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClientProvider.init(this)
    }
}
