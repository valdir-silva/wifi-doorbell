package com.alunando.wifidoorbell.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alunando.wifidoorbell.core.db.DatabaseFactory
import com.alunando.wifidoorbell.core.db.SqlDelightDeviceRepository
import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.service.NetworkMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseFactory.create(application)
    private val deviceRepository = SqlDelightDeviceRepository(db)

    val allDevices: StateFlow<List<Device>> = deviceRepository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    fun toggleMonitoring() {
        val context = getApplication<Application>()
        if (_isMonitoring.value) {
            NetworkMonitorService.stop(context)
            _isMonitoring.value = false
        } else {
            NetworkMonitorService.start(context)
            _isMonitoring.value = true
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
