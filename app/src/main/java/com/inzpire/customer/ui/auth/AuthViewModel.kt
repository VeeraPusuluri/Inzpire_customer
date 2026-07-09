package com.inzpire.customer.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inzpire.customer.data.AuthRepository
import com.inzpire.customer.data.SignUpOutcome
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

/** Which field an inline error / focus request refers to. */
enum class AuthField { NAME, PHONE, EMAIL, PASSWORD }

/** Password strength buckets used to drive the sign-up strength meter. */
enum class PasswordStrength { NONE, WEAK, FAIR, STRONG }

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    var mode by mutableStateOf(AuthMode.SIGN_IN)
        private set

    // Field values are owned by the VM and mutated only through the on*Change events
    // below (unidirectional data flow) so each edit can clear its own inline error.
    var name by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    /** Server / submit-level error (e.g. bad credentials), shown as a banner. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Positive / neutral banner, e.g. "check your email to confirm your account". */
    var infoMessage by mutableStateOf<String?>(null)
        private set

    // Per-field inline errors, shown directly beneath each input.
    var nameError by mutableStateOf<String?>(null)
        private set
    var phoneError by mutableStateOf<String?>(null)
        private set
    var emailError by mutableStateOf<String?>(null)
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set

    val passwordStrength: PasswordStrength
        get() = strengthOf(password)

    fun switchMode(newMode: AuthMode) {
        if (newMode == mode) return
        mode = newMode
        clearAllErrors()
        infoMessage = null
    }

    fun onNameChange(value: String) {
        name = value
        if (nameError != null) nameError = null
        errorMessage = null
        infoMessage = null
    }

    fun onPhoneChange(value: String) {
        phone = value
        if (phoneError != null) phoneError = null
        errorMessage = null
        infoMessage = null
    }

    fun onEmailChange(value: String) {
        email = value
        if (emailError != null) emailError = null
        errorMessage = null
        infoMessage = null
    }

    fun onPasswordChange(value: String) {
        password = value
        if (passwordError != null) passwordError = null
        errorMessage = null
        infoMessage = null
    }

    // Blur validators — validate on focus-loss (not every keystroke) per inline-validation UX.
    fun validateName() {
        if (mode != AuthMode.SIGN_UP) return
        nameError = if (name.isBlank()) "Enter your name" else null
    }

    fun validatePhone() {
        if (mode != AuthMode.SIGN_UP) return
        phoneError = phoneErrorFor(phone)
    }

    fun validateEmail() {
        emailError = emailErrorFor(email)
    }

    fun validatePassword() {
        passwordError = passwordErrorFor(password, mode)
    }

    /** Validates every visible field, populating inline errors. Returns true when the form is valid. */
    fun validateAll(): Boolean {
        if (mode == AuthMode.SIGN_UP) {
            nameError = if (name.isBlank()) "Enter your name" else null
            phoneError = phoneErrorFor(phone)
        } else {
            nameError = null
            phoneError = null
        }
        emailError = emailErrorFor(email)
        passwordError = passwordErrorFor(password, mode)
        return firstInvalidField() == null
    }

    /** First field (in visual order) that currently has an error, so the UI can focus it. */
    fun firstInvalidField(): AuthField? = when {
        mode == AuthMode.SIGN_UP && nameError != null -> AuthField.NAME
        mode == AuthMode.SIGN_UP && phoneError != null -> AuthField.PHONE
        emailError != null -> AuthField.EMAIL
        passwordError != null -> AuthField.PASSWORD
        else -> null
    }

    fun submit(onSuccess: () -> Unit) {
        if (isLoading) return
        errorMessage = null
        infoMessage = null
        if (!validateAll()) return

        viewModelScope.launch {
            isLoading = true
            val result = runCatching {
                if (mode == AuthMode.SIGN_UP) {
                    authRepository.signUp(email.trim(), password, name.trim(), phone.trim())
                } else {
                    authRepository.signIn(email.trim(), password)
                    // A successful sign-in always yields a session.
                    SignUpOutcome.SIGNED_IN
                }
            }
            isLoading = false
            result
                .onSuccess { outcome ->
                    when (outcome) {
                        // Session established (sign-in, or sign-up with confirmation off) —
                        // let the session-driven navigation take the user into the app.
                        SignUpOutcome.SIGNED_IN -> onSuccess()
                        // Sign-up succeeded but the project requires email confirmation, so there's
                        // no session yet. Flip to Sign in and tell the user to check their inbox —
                        // matching the Connect app's behaviour.
                        SignUpOutcome.CONFIRM_EMAIL -> {
                            mode = AuthMode.SIGN_IN
                            password = ""
                            clearAllErrors()
                            infoMessage =
                                "Account created. We've emailed a confirmation link to ${email.trim()} — " +
                                "tap it, then sign in."
                        }
                    }
                }
                .onFailure { errorMessage = it.message ?: "Something went wrong. Please try again." }
        }
    }

    private fun clearAllErrors() {
        errorMessage = null
        nameError = null
        phoneError = null
        emailError = null
        passwordError = null
    }

    private fun emailErrorFor(value: String): String? = when {
        value.isBlank() -> "Enter your email"
        !EMAIL_REGEX.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    private fun passwordErrorFor(value: String, mode: AuthMode): String? = when {
        value.isBlank() -> "Enter your password"
        mode == AuthMode.SIGN_UP && value.length < 6 -> "Use at least 6 characters"
        else -> null
    }

    private fun phoneErrorFor(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null // phone is optional
        val digits = trimmed.count { it.isDigit() }
        return if (digits < 7) "Enter a valid phone number" else null
    }

    private fun strengthOf(value: String): PasswordStrength {
        if (value.isEmpty()) return PasswordStrength.NONE
        var score = 0
        if (value.length >= 6) score++
        if (value.length >= 10) score++
        if (value.any { it.isDigit() } && value.any { it.isLetter() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 1 -> PasswordStrength.WEAK
            score == 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.STRONG
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
