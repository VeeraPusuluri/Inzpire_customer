package com.inzpire.customer.data

import android.app.Application
import com.inzpire.customer.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Single [io.github.jan.supabase.SupabaseClient], mirroring the web app's `supabase` singleton
 * in `src/integrations/supabase/client.ts` — same project, same anon key, session persisted
 * on-device (Auth defaults to `persistSession = true`, `autoRefreshToken = true`).
 */
object SupabaseClientProvider {

    private var app: Application? = null

    fun init(application: Application) {
        app = application
    }

    val client by lazy {
        requireNotNull(app) { "SupabaseClientProvider.init() must be called from Application.onCreate()" }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
}
