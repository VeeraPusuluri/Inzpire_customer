package com.inzpire.customer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inzpire.customer.data.AuthRepository
import com.inzpire.customer.data.NotificationsRepository
import com.inzpire.customer.data.model.NotificationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs the in-app notification bell — loads the signed-in user's notifications. */
class NotificationsViewModel : ViewModel() {
    private val repo = NotificationsRepository()
    private val auth = AuthRepository()

    private val _items = MutableStateFlow<List<NotificationDto>>(emptyList())
    val items: StateFlow<List<NotificationDto>> = _items.asStateFlow()

    init { refresh() }

    fun refresh() {
        val uid = auth.currentUserId ?: return
        viewModelScope.launch {
            runCatching { repo.list(uid) }.onSuccess { _items.value = it }
        }
    }

    fun markRead(id: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(isRead = true) else it }
        viewModelScope.launch { runCatching { repo.markRead(id) } }
    }

    fun markAllRead() {
        val uid = auth.currentUserId ?: return
        _items.value = _items.value.map { it.copy(isRead = true) }
        viewModelScope.launch { runCatching { repo.markAllRead(uid) } }
    }

    fun delete(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        viewModelScope.launch {
            runCatching { repo.delete(id) }.onFailure { refresh() }
        }
    }

    fun clearAll() {
        val uid = auth.currentUserId ?: return
        _items.value = emptyList()
        viewModelScope.launch {
            runCatching { repo.deleteAll(uid) }.onFailure { refresh() }
        }
    }
}
