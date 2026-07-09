package com.inzpire.customer.data

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Outcome of a sign-up attempt. When the Supabase project has email confirmation enabled
 * (`mailer_autoconfirm = false`), `signUpWith` succeeds but does *not* establish a session —
 * the user must click the emailed link first. The UI needs to tell those two cases apart so it
 * can either drop the user straight into the app or ask them to confirm their email.
 */
enum class SignUpOutcome { SIGNED_IN, CONFIRM_EMAIL }

/**
 * Email/phone verification snapshot taken straight off the Supabase auth user
 * (`email_confirmed_at` / `phone_confirmed_at`) — the `profiles` row doesn't track this.
 */
data class AuthIdentity(
    val email: String?,
    val phone: String?,
    val emailVerified: Boolean,
    val phoneVerified: Boolean,
)

/**
 * Wraps Supabase Auth for the customer app. Every sign-up is created with
 * `role = "customer"` in `raw_user_meta_data` — the same trigger the web app relies on
 * (`handle_new_user()`) inserts the matching `profiles` + `user_roles` rows server-side.
 */
class AuthRepository(private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client) {

    val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    suspend fun awaitInitialization() = client.auth.awaitInitialization()

    val currentUserId: String? get() = client.auth.currentUserOrNull()?.id

    /** Verification flags for the signed-in user, or null if there's no session. */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun currentIdentity(): AuthIdentity? {
        val user = client.auth.currentUserOrNull() ?: return null
        return AuthIdentity(
            email = user.email,
            phone = user.phone,
            emailVerified = user.emailConfirmedAt != null,
            phoneVerified = user.phoneConfirmedAt != null,
        )
    }

    /**
     * Sets the auth user's phone, which makes Supabase text a 6-digit OTP to it.
     * Requires an SMS provider + phone confirmations enabled on the Supabase project.
     */
    suspend fun sendPhoneOtp(phone: String) {
        client.auth.updateUser { this.phone = phone }
    }

    /** Confirms the phone with the texted code; sets phone_confirmed_at on success. */
    suspend fun verifyPhoneOtp(phone: String, token: String) {
        client.auth.verifyPhoneOtp(type = OtpType.Phone.PHONE_CHANGE, phone = phone, token = token)
    }

    /** Re-fetch the auth user so the verification flags reflect the latest state. */
    suspend fun reloadUser() {
        client.auth.retrieveUserForCurrentSession(updateSession = true)
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String, name: String, phone: String): SignUpOutcome {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject {
                put("name", name)
                put("phone", phone)
                put("role", "customer")
            }
        }
        // With email confirmation on, no session exists yet — the caller must ask the user to
        // confirm their email instead of pretending they're logged in. After the user taps the
        // link in their inbox, the account is confirmed and they can sign in with these credentials.
        return if (client.auth.currentSessionOrNull() != null) SignUpOutcome.SIGNED_IN
        else SignUpOutcome.CONFIRM_EMAIL
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
