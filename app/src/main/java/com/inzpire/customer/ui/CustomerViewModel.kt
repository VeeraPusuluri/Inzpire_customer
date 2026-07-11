package com.inzpire.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inzpire.customer.data.AuthIdentity
import com.inzpire.customer.data.AuthRepository
import com.inzpire.customer.data.ChatRepository
import com.inzpire.customer.data.Cockpit
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitRepository
import com.inzpire.customer.data.PayoutRepository
import com.inzpire.customer.data.ProfileRepository
import com.inzpire.customer.data.SupabaseClientProvider
import com.inzpire.customer.data.model.ChangeRequestInsert
import com.inzpire.customer.data.model.PayoutRow
import com.inzpire.customer.data.model.Profile
import com.inzpire.customer.data.model.ProfilePatch
import com.inzpire.customer.data.model.ReferralBonusRow
import com.inzpire.customer.notifications.PushRegistrar
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime

/** One chat bubble, resolved against the project team for name/role display. */
data class ChatMessage(
    val id: String,
    val body: String,
    val fromMe: Boolean,
    val senderName: String,
    val senderRole: String,
    val at: String,
)

/**
 * Shared, Activity-scoped state for the signed-in customer: session, profile, roles
 * and the live project cockpit. Screens read slices of this. When the customer has
 * no project yet (or a fetch fails) the cockpit falls back to [Cockpit.seed] so the
 * UI is always populated.
 */
class CustomerViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val cockpitRepository = CockpitRepository()
    private val chatRepository = ChatRepository()
    private val payoutRepository = PayoutRepository()

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _roles = MutableStateFlow<List<String>>(emptyList())
    val roles: StateFlow<List<String>> = _roles.asStateFlow()

    /** Email/phone verification flags from the auth user (drives the Settings badges). */
    private val _identity = MutableStateFlow<AuthIdentity?>(null)
    val identity: StateFlow<AuthIdentity?> = _identity.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** The cockpit shown by the screens — the selected project, seed until first load. */
    private val _cockpit = MutableStateFlow(Cockpit.seed)
    val cockpit: StateFlow<Cockpit> = _cockpit.asStateFlow()

    /** All of the customer's projects (for the home pager); empty until loaded. */
    private val _cockpits = MutableStateFlow<List<Cockpit>>(emptyList())
    val cockpits: StateFlow<List<Cockpit>> = _cockpits.asStateFlow()

    /** Index of the active project within [cockpits]. */
    private val _selectedProjectIndex = MutableStateFlow(0)
    val selectedProjectIndex: StateFlow<Int> = _selectedProjectIndex.asStateFlow()

    /** True once a real project has been loaded from Supabase (vs. the seed fallback). */
    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live.asStateFlow()

    private val _cockpitLoading = MutableStateFlow(false)
    val cockpitLoading: StateFlow<Boolean> = _cockpitLoading.asStateFlow()

    /**
     * False until the first cockpit fetch after sign-in completes. Home shows a loading
     * placeholder while false so the seed fallback never flashes before real data arrives.
     */
    private val _cockpitLoaded = MutableStateFlow(false)
    val cockpitLoaded: StateFlow<Boolean> = _cockpitLoaded.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // ---- payouts (money owed to the customer: referral bonuses, commissions, refunds) ----
    private val _payouts = MutableStateFlow<List<PayoutRow>>(emptyList())
    val payouts: StateFlow<List<PayoutRow>> = _payouts.asStateFlow()

    private val _pendingBonuses = MutableStateFlow<List<ReferralBonusRow>>(emptyList())
    val pendingBonuses: StateFlow<List<ReferralBonusRow>> = _pendingBonuses.asStateFlow()

    private val _payoutsLoading = MutableStateFlow(false)
    val payoutsLoading: StateFlow<Boolean> = _payoutsLoading.asStateFlow()

    /** One-shot user-feedback messages (shown as a toast by the UI). */
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    private fun toast(message: String) {
        _toasts.tryEmit(message)
    }

    val currentUserId: String? get() = authRepository.currentUserId

    init {
        viewModelScope.launch {
            authRepository.awaitInitialization()
            authRepository.currentUserId?.let {
                refreshAll()
                PushRegistrar.register(it)
            }
        }
    }

    fun onSignedIn() {
        viewModelScope.launch { refreshAll() }
        currentUserId?.let { PushRegistrar.register(it) }
    }

    fun clearLocalData() {
        _profile.value = null
        _roles.value = emptyList()
        _cockpit.value = Cockpit.seed
        _cockpits.value = emptyList()
        _selectedProjectIndex.value = 0
        _live.value = false
        _cockpitLoaded.value = false
        _messages.value = emptyList()
        _payouts.value = emptyList()
        _pendingBonuses.value = emptyList()
        _identity.value = null
    }

    /** Loads the customer's payouts + not-yet-released referral bonuses (called when the screen opens). */
    fun loadPayouts() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _payoutsLoading.value = true
            runCatching { payoutRepository.listPayouts(userId) }.onSuccess { _payouts.value = it }
            runCatching { payoutRepository.listPendingReferralBonuses(userId) }.onSuccess { _pendingBonuses.value = it }
            _payoutsLoading.value = false
        }
    }

    fun refreshAll() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching {
                _profile.value = profileRepository.getProfile(userId)
                _roles.value = profileRepository.getRoles(userId)
                _identity.value = authRepository.currentIdentity()
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't load your data. Pull to refresh."
            }
            _isLoading.value = false
            refreshCockpit()
        }
    }

    fun refreshCockpit() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _cockpitLoading.value = true
            runCatching {
                cockpitRepository.loadAll(userId, _profile.value?.location)
            }.onSuccess { loaded ->
                if (loaded.isNotEmpty()) {
                    _cockpits.value = loaded
                    val idx = _selectedProjectIndex.value.coerceIn(0, loaded.lastIndex)
                    _selectedProjectIndex.value = idx
                    _cockpit.value = loaded[idx]
                    _live.value = true
                    loadMessages()
                    startRealtimeMessages()
                }
            }
            _cockpitLoading.value = false
            _cockpitLoaded.value = true
        }
    }

    /** Switch the active project — re-drives the home page and every tab. */
    fun selectProject(index: Int) {
        val list = _cockpits.value
        if (index !in list.indices || index == _selectedProjectIndex.value) return
        _selectedProjectIndex.value = index
        _cockpit.value = list[index]
        loadMessages()
    }

    // ---- cockpit write actions (optimistic; persisted when live) ----

    fun approveDesign(id: String) {
        toast("Design approved")
        mutateDesign(id, CockpitData.ReviewStatus.APPROVED) {
            cockpitRepository.approveDesign(id)
        }
    }

    /**
     * Customer asks for changes on a design. Optimistically flags the card, then
     * (when live) flips the moodboard to "revision" and logs a change_request so
     * the request reaches the admin's Requests tab + notification bell.
     */
    fun requestDesignChanges(id: String, comment: String? = null) {
        toast("Change request sent to your designer")
        _cockpit.value = _cockpit.value.copy(
            designs = _cockpit.value.designs.map {
                if (it.id == id) it.copy(status = CockpitData.ReviewStatus.CHANGES) else it
            },
        )
        if (!_live.value) return
        val design = _cockpit.value.designs.firstOrNull { it.id == id }
        val projectId = _cockpit.value.projectId
        val uid = currentUserId ?: return
        val note = comment?.trim()?.ifBlank { null }
        viewModelScope.launch {
            // Independent so a failed moodboard update never drops the logged request.
            runCatching { cockpitRepository.requestDesignChanges(id, note) }
            runCatching {
                cockpitRepository.submitChangeRequest(
                    ChangeRequestInsert(
                        projectId = projectId,
                        customerId = uid,
                        kind = "design",
                        refId = id,
                        refTitle = design?.let { "${it.room} · ${it.title}" },
                        room = design?.room,
                        note = note ?: "Please revise this design.",
                    ),
                )
            }
        }
    }

    /** Customer accepts a material → optimistic "Locked" (final sign-off), persisted when live. */
    fun acceptMaterial(id: String) {
        toast("Material accepted")
        _cockpit.value = _cockpit.value.copy(
            materials = _cockpit.value.materials.map {
                if (it.id == id) it.copy(status = CockpitData.MaterialStatus.LOCKED) else it
            },
        )
        if (_live.value) viewModelScope.launch { runCatching { cockpitRepository.acceptMaterial(id) } }
    }

    /** Customer asks for a change on a material selection → logged to change_requests for admin. */
    fun requestMaterialChange(id: String, note: String) {
        toast("Change request sent to your designer")
        if (!_live.value) return
        val material = _cockpit.value.materials.firstOrNull { it.id == id }
        val projectId = _cockpit.value.projectId
        val uid = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                cockpitRepository.submitChangeRequest(
                    ChangeRequestInsert(
                        projectId = projectId,
                        customerId = uid,
                        kind = "material",
                        refId = id,
                        refTitle = material?.let { "${it.category} · ${it.name}" },
                        room = material?.room,
                        note = note.trim(),
                    ),
                )
            }
        }
    }

    private fun mutateDesign(id: String, status: CockpitData.ReviewStatus, persist: suspend () -> Unit) {
        _cockpit.value = _cockpit.value.copy(
            designs = _cockpit.value.designs.map { if (it.id == id) it.copy(status = status) else it },
        )
        if (_live.value) viewModelScope.launch { runCatching { persist() } }
    }

    fun approveApproval(id: String) {
        toast("Approved")
        val approver = _profile.value?.name ?: "You"
        _cockpit.value = _cockpit.value.copy(
            approvals = _cockpit.value.approvals.map {
                if (it.id == id) it.copy(status = CockpitData.ReviewStatus.APPROVED, approver = approver, signedAt = LocalDate.now()) else it
            },
        )
        val uid = currentUserId
        if (_live.value && uid != null) viewModelScope.launch { runCatching { cockpitRepository.approveApproval(id, uid) } }
    }

    fun rejectApproval(id: String) {
        toast("Changes requested")
        _cockpit.value = _cockpit.value.copy(
            approvals = _cockpit.value.approvals.map {
                if (it.id == id) it.copy(status = CockpitData.ReviewStatus.CHANGES) else it
            },
        )
        if (_live.value) viewModelScope.launch { runCatching { cockpitRepository.rejectApproval(id, null) } }
    }

    fun payMilestone(id: String) {
        toast("Payment recorded")
        _cockpit.value = _cockpit.value.copy(
            payments = _cockpit.value.payments.map {
                if (it.id == id) it.copy(status = CockpitData.PaymentStatus.PAID, paidAt = LocalDate.now()) else it
            },
        )
        if (_live.value) viewModelScope.launch { runCatching { cockpitRepository.markPaymentPaid(id) } }
    }

    // ---- chat ----

    private var realtimeStarted = false

    /**
     * Subscribe once (for the ViewModel's lifetime) to inserts on public.messages so an
     * admin's new message shows up live in the open chat. The event is only used as a
     * "something changed" trigger — we re-fetch the selected project's messages (RLS-scoped)
     * via [loadMessages], so switching the active project needs no re-subscribe.
     */
    private fun startRealtimeMessages() {
        if (realtimeStarted) return
        realtimeStarted = true
        val channel = SupabaseClientProvider.client.channel("customer-messages")
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.onEach { if (_live.value) loadMessages() }.launchIn(viewModelScope)
        viewModelScope.launch { runCatching { channel.subscribe() } }
    }

    fun loadMessages() {
        if (!_live.value) return
        val projectId = _cockpit.value.projectId
        viewModelScope.launch {
            runCatching { chatRepository.list(projectId) }.onSuccess { rows ->
                _messages.value = rows.map { m ->
                    val fromMe = m.senderId == currentUserId
                    val person = _cockpit.value.team.firstOrNull { it.id == m.senderId }
                    ChatMessage(
                        id = m.id,
                        body = m.body ?: "",
                        fromMe = fromMe,
                        senderName = if (fromMe) "You" else person?.name ?: (_profile.value?.name ?: "Inzpire"),
                        senderRole = if (fromMe) "" else person?.role ?: "Team",
                        at = formatTime(m.createdAt),
                    )
                }
            }
        }
    }

    fun sendMessage(body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || !_live.value) return
        val projectId = _cockpit.value.projectId
        val uid = currentUserId ?: return
        viewModelScope.launch {
            runCatching { chatRepository.send(projectId, uid, trimmed) }
            loadMessages()
        }
    }

    private fun formatTime(iso: String?): String {
        if (iso == null) return ""
        return runCatching {
            val dt = OffsetDateTime.parse(iso)
            "%02d:%02d".format(dt.hour, dt.minute)
        }.getOrDefault("")
    }

    fun updateProfile(patch: ProfilePatch, onDone: (Result<Unit>) -> Unit) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = runCatching {
                profileRepository.updateProfile(userId, patch)
                _profile.value = profileRepository.getProfile(userId)
            }
            onDone(result)
        }
    }

    /** Texts a 6-digit OTP to [phone] (sets the auth user's phone; Supabase sends the SMS). */
    fun sendPhoneOtp(phone: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { authRepository.sendPhoneOtp(phone.trim()) })
        }
    }

    /** Confirms the OTP, then refreshes the verification flags. */
    fun verifyPhoneOtp(phone: String, token: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                authRepository.verifyPhoneOtp(phone.trim(), token.trim())
                authRepository.reloadUser()
                _identity.value = authRepository.currentIdentity()
            }
            onDone(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Best-effort: drop this device's token while still authed (RLS), then
            // invalidate it locally so any in-flight push is pruned server-side.
            runCatching { PushRegistrar.unregister() }
            authRepository.signOut()
            clearLocalData()
        }
    }
}
