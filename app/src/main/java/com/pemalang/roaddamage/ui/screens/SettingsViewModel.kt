package com.pemalang.roaddamage.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.data.prefs.UserPrefs
import com.pemalang.roaddamage.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val prefs: UserPrefs,
    private val tripDao: TripDao
) : ViewModel() {
    data class Ui(
        val samplingHz: Int = 50,
        val gpsIntervalSec: Int = 1,
        val autoUpload: Boolean = false,
        val userId: String = "",
        val userName: String = "",
        val userEmail: String = "",
        val vehicleType: String = ""
    )
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    init {
        viewModelScope.launch {
            val id = prefs.getOrCreateUserId()
            val hz = prefs.getSamplingRateHz()
            val gps = prefs.getGpsIntervalSec()
            val auto = prefs.getAutoUpload()
            val name = prefs.getUserName() ?: ""
            val email = prefs.getUserEmail() ?: ""
            val vehicle = prefs.getVehicleType() ?: ""
            _ui.value = Ui(
                samplingHz = hz,
                gpsIntervalSec = gps,
                autoUpload = auto,
                userId = id,
                userName = name,
                userEmail = email,
                vehicleType = vehicle
            )
        }
    }


    fun setSampling(hz: Int) {
        viewModelScope.launch {
            prefs.setSamplingRateHz(hz)
            _ui.value = _ui.value.copy(samplingHz = hz)
        }
    }
    fun setGpsInterval(sec: Int) {
        viewModelScope.launch {
            prefs.setGpsIntervalSec(sec)
            _ui.value = _ui.value.copy(gpsIntervalSec = sec)
        }
    }
    fun setAutoUpload(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoUpload(enabled)
            _ui.value = _ui.value.copy(autoUpload = enabled)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            prefs.setUserName(name)
            _ui.value = _ui.value.copy(userName = name)
        }
    }
    fun setUserEmail(email: String) {
        viewModelScope.launch {
            prefs.setUserEmail(email)
            _ui.value = _ui.value.copy(userEmail = email)
        }
    }
    fun setVehicleType(vehicle: String) {
        viewModelScope.launch {
            prefs.setVehicleType(vehicle)
            _ui.value = _ui.value.copy(vehicleType = vehicle)
        }
    }

    fun deleteUploadedTrips() {
        viewModelScope.launch {
            try {
                val list = tripDao.getAll()
                list.filter { it.uploadStatus == UploadStatus.UPLOADED }.forEach { t ->
                    // 1. Delete CSV data file
                    try { File(t.dataFilePath).delete() } catch (_: Throwable) {}
                    // 2. Delete trip record
                    tripDao.deleteById(t.tripId)
                }
            } catch (_: Throwable) {
            }
        }
    }
}
