package com.inzpire.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inzpire.customer.data.AuthIdentity
import com.inzpire.customer.data.AuthRepository
import com.inzpire.customer.data.ChatRepository
import com.inzpire.customer.data.Cockpit
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitRepository
import com.inzpire.customer.data.ProfileRepository
import com.inzpire.customer.data.model.Profile
import com.inzpire.customer.data.model.ProfilePatch
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** The cockpit shown by the screens — seed until the first live load resolves. */
    private val _cockpit = MutableStateFlow(Cockpit.seed)
    val cockpit: StateFlow<Cockpit> = _cockpit.asStateFlow()

    /** True once a real project has been loaded from Supabase (vs. the seed fallback). */
    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live.asStateFlow()

    private val _cockpitLoading = MutableStateFlow(false)
    val cockpitLoading: StateFlow<Boolean> = _cockpitLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUserId

    init {
        viewModelScope.launch {
            authRepository.awaitInitialization()
            if (authRepository.currentUserId != null) refreshAll()
        }
    }

    fun onSignedIn() {
        viewModelScope.launch { refreshAll() }
    }

    fun clearLocalData() {
        _profile.value = null
        _roles.value = emptyList()
        _cockpit.value = Cockpit.seed
        _live.value = false
        _messages.value = emptyList()
        _identity.value = null
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
                cockpitRepository.load(userId, _profile.value?.location)
            }.onSuccess { loaded ->
                if (loaded != null) {
                    _cockpit.value = loaded
                    _live.value = true
                    loadMessages()
                }
            }
            _cockpitLoading.value = false
        }
    }

    // ---- cockpit write actions (optimistic; persisted when live) ----

    fun approveDesign(id: String) = mutateDesign(id, CockpitData.ReviewStatus.APPROVED) {
        cockpitRepository.approveDesign(id)
    }

    fun requestDesignChanges(id: String, comment: String? = null) =
        mutateDesign(id, CockpitData.ReviewStatus.CHANGES) {
            cockpitRepository.requestDesignChanges(id, comment)
        }

    private fun mutateDesign(id: String, status: CockpitData.ReviewStatus, persist: suspend () -> Unit) {
        _cockpit.value = _cockpit.value.copy(
            designs = _cockpit.value.designs.map { if (it.id == id) it.copy(status = status) else it },
        )
        if (_live.value) viewModelScope.launch { runCatching { persist() } }
    }

    fun approveApproval(id: String) {
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
        _cockpit.value = _cockpit.value.copy(
            approvals = _cockpit.value.approvals.map {
                if (it.id == id) it.copy(status = CockpitData.ReviewStatus.CHANGES) else it
            },
        )
        if (_live.value) viewModelScope.launch { runCatching { cockpitRepository.rejectApproval(id, null) } }
    }

    fun payMilestone(id: String) {
        _cockpit.value = _cockpit.value.copy(
            payments = _cockpit.value.payments.map {
                if (it.id == id) it.copy(status = CockpitData.PaymentStatus.PAID, paidAt = LocalDate.now()) else it
            },
        )
        if (_live.value) viewModelScope.launch { runCatching { cockpitRepository.markPaymentPaid(id) } }
    }

    // ---- chat ----

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
            authRepository.signOut()
            clearLocalData()
        }
    }
}
