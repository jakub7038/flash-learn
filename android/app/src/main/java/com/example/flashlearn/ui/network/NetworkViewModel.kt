package com.example.flashlearn.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.network.NetworkMonitor
import com.example.flashlearn.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : ViewModel() {

    /**
     * Emits true when the device has internet access, false otherwise.
     * Automatically schedules a sync whenever connectivity is (re)established.
     */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .onEach { online -> if (online) syncManager.scheduleSync() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )
}
