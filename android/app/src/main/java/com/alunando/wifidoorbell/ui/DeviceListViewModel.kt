package com.alunando.wifidoorbell.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alunando.wifidoorbell.core.db.DatabaseFactory
import com.alunando.wifidoorbell.core.db.SqlDelightDeviceRepository
import com.alunando.wifidoorbell.core.model.Device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging

class DeviceListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseFactory.create(application)
    private val deviceRepository = SqlDelightDeviceRepository(db)

    val allDevices: StateFlow<List<Device>> = deviceRepository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // O monitoramento agora é feito pelo Raspberry Pi,
    // portanto, não temos mais estado de 'isMonitoring' nem start/stop de serviço.

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken

    fun fetchToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _fcmToken.value = task.result
            }
        }
    }

    fun toggleWatched(deviceId: String, currentlyWatched: Boolean) {
        viewModelScope.launch {
            deviceRepository.toggleWatched(deviceId, !currentlyWatched)
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            val device = deviceRepository.getDevice(deviceId)
            if (device != null) {
                val updatedDevice = device.copy(
                    customName = newName.ifBlank { null }
                )
                deviceRepository.saveDevice(updatedDevice)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceListViewModel(application) as T
        }
    }
}
