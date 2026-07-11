package com.inzpire.customer.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Destructive
import com.inzpire.customer.ui.theme.Gold
import com.inzpire.customer.ui.theme.Muted
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.NavyDeep

private val Success = Color(0xFF16A34A)

@Composable
fun AuthScreen(onAuthenticated: () -> Unit, viewModel: AuthViewModel = viewModel()) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val nameFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    fun trySubmit() {
        if (viewModel.validateAll()) {
            focusManager.clearFocus()
            viewModel.submit(onAuthenticated)
        } else when (viewModel.firstInvalidField()) {
            AuthField.NAME -> nameFocus.requestFocus()
            AuthField.PHONE -> phoneFocus.requestFocus()
            AuthField.EMAIL -> emailFocus.requestFocus()
            AuthField.PASSWORD -> passwordFocus.requestFocus()
            null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            // Pad by whichever is larger — the nav bar (keyboard closed) or the IME
            // (keyboard open) — OUTSIDE the scroll, so the viewport shrinks when the
            // keyboard opens and the focused field's bring-into-view lands above it.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .verticalScroll(rememberScrollState()),
    ) {
        AuthHero(mode = viewModel.mode)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-28).dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                ModeToggle(mode = viewModel.mode, onSelect = viewModel::switchMode)

                Column(
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (viewModel.mode == AuthMode.SIGN_UP) {
                        AuthField(
                            value = viewModel.name,
                            onValueChange = viewModel::onNameChange,
                            label = "Full name",
                            required = true,
                            leadingIcon = Icons.Filled.Person,
                            error = viewModel.nameError,
                            imeAction = ImeAction.Next,
                            onImeAction = { phoneFocus.requestFocus() },
                            onFocusLost = viewModel::validateName,
                            focusRequester = nameFocus,
                        )
                        AuthField(
                            value = viewModel.phone,
                            onValueChange = viewModel::onPhoneChange,
                            label = "Phone",
                            placeholder = "+91 98xxxxxxxx",
                            leadingIcon = Icons.Filled.Phone,
                            keyboardType = KeyboardType.Phone,
                            error = viewModel.phoneError,
                            imeAction = ImeAction.Next,
                            onImeAction = { emailFocus.requestFocus() },
                            onFocusLost = viewModel::validatePhone,
                            focusRequester = phoneFocus,
                        )
                    }
                    AuthField(
                        value = viewModel.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        required = true,
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email,
                        error = viewModel.emailError,
                        imeAction = ImeAction.Next,
                        onImeAction = { passwordFocus.requestFocus() },
                        onFocusLost = viewModel::validateEmail,
                        focusRequester = emailFocus,
                    )
                    AuthField(
                        value = viewModel.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        required = true,
                        leadingIcon = Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password,
                        error = viewModel.passwordError,
                        imeAction = ImeAction.Done,
                        onImeAction = { trySubmit() },
                        onFocusLost = viewModel::validatePassword,
                        focusRequester = passwordFocus,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MutedForeground,
                                )
                            }
                        },
                    )

                    if (viewModel.mode == AuthMode.SIGN_UP && viewModel.passwordStrength != PasswordStrength.NONE) {
                        PasswordStrengthMeter(
                            strength = viewModel.passwordStrength,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                        )
                    }

                    viewModel.errorMessage?.let { message ->
                        ErrorBanner(message = message, modifier = Modifier.padding(top = 8.dp))
                    }

                    viewModel.infoMessage?.let { message ->
                        InfoBanner(message = message, modifier = Modifier.padding(top = 8.dp))
                    }

                    Button(
                        onClick = { trySubmit() },
                        enabled = !viewModel.isLoading,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 12.dp),
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (viewModel.mode == AuthMode.SIGN_IN) "Sign in" else "Create account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }

                    if (viewModel.mode == AuthMode.SIGN_UP) {
                        Text(
                            "By creating an account, you agree to our Terms & Privacy Policy.",
                            color = MutedForeground,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AuthHero(mode: AuthMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(listOf(NavyDeep, Navy))),
    ) {
        // Soft brand-tinted glow for depth (decorative, purely visual).
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-70).dp)
                .background(
                    brush = Brush.radialGradient(listOf(Gold.copy(alpha = 0.28f), Color.Transparent)),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 28.dp, bottom = 48.dp, start = 28.dp, end = 28.dp),
        ) {
            LogoMark()
            Text(
                "INZPIRE",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeInSlide() },
                label = "headline",
            ) { m ->
                Text(
                    text = if (m == AuthMode.SIGN_IN) "Welcome back" else "Create your\naccount",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                text = if (mode == AuthMode.SIGN_IN)
                    "Sign in to track your project, designs and payments."
                else
                    "Track your project, designs and payments — all in one place.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun fadeInSlide() =
    (androidx.compose.animation.fadeIn(tween(220)) +
        androidx.compose.animation.slideInVertically(tween(220)) { it / 3 }) togetherWith
        androidx.compose.animation.fadeOut(tween(140))

@Composable
private fun LogoMark() {
    Box(modifier = Modifier.size(width = 60.dp, height = 40.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterStart)
                .border(width = 3.dp, color = Color.White, shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterEnd)
                .border(width = 3.dp, color = Gold, shape = CircleShape),
        )
    }
}

@Composable
private fun ModeToggle(mode: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Muted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(AuthMode.SIGN_IN to "Sign in", AuthMode.SIGN_UP to "Sign up").forEach { (m, label) ->
            val selected = mode == m
            val containerColor by animateColorAsState(
                targetValue = if (selected) Color.White else Color.Transparent,
                animationSpec = tween(220),
                label = "toggleBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Navy else MutedForeground,
                animationSpec = tween(220),
                label = "toggleText",
            )
            val elevation by animateDpAsState(
                targetValue = if (selected) 2.dp else 0.dp,
                animationSpec = tween(220),
                label = "toggleElevation",
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                shadowElevation = elevation,
                onClick = { onSelect(m) },
            ) {
                Text(
                    label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    focusRequester: FocusRequester,
    onImeAction: () -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    error: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isError = error != null
    // Validate on blur: fire onFocusLost once when the field loses focus after being focused.
    var wasFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (required) "$label *" else label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = error?.let {
            {
                Text(
                    it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Navy,
            unfocusedBorderColor = Border,
            focusedLeadingIconColor = Navy,
            unfocusedLeadingIconColor = MutedForeground,
            focusedLabelColor = Navy,
            cursorColor = Navy,
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (wasFocused && !state.isFocused) onFocusLost()
                wasFocused = state.isFocused
            },
    )
}

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength, modifier: Modifier = Modifier) {
    val (activeBars, color, label) = when (strength) {
        PasswordStrength.WEAK -> Triple(1, Destructive, "Weak")
        PasswordStrength.FAIR -> Triple(2, Gold, "Fair")
        PasswordStrength.STRONG -> Triple(3, Success, "Strong")
        PasswordStrength.NONE -> Triple(0, Border, "")
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                val barColor by animateColorAsState(
                    targetValue = if (index < activeBars) color else Muted,
                    animationSpec = tween(200),
                    label = "strengthBar",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor),
                )
            }
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Success.copy(alpha = 0.10f))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MarkEmailRead,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(message, color = Success, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Destructive.copy(alpha = 0.08f))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Destructive,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(message, color = Destructive, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
