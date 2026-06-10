package com.pemalang.roaddamage.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.data.prefs.UserPrefs
import com.pemalang.roaddamage.recording.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingViewModel
@Inject
constructor(
        private val app: Application,
        private val repo: RecordingRepository,
        private val tripDao: TripDao,
        private val prefs: UserPrefs
) : ViewModel() {
    private val _magnitudes = MutableStateFlow(FloatArray(0))
    private val _ax = MutableStateFlow(FloatArray(0))
    private val _ay = MutableStateFlow(FloatArray(0))
    private val _az = MutableStateFlow(FloatArray(0))
    private val _points = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val magnitudes: StateFlow<FloatArray> = _magnitudes
    val ax: StateFlow<FloatArray> = _ax
    val ay: StateFlow<FloatArray> = _ay
    val az: StateFlow<FloatArray> = _az
    val points: StateFlow<List<Pair<Double, Double>>> = _points
    val distance: StateFlow<Float> = repo.distanceFlow
    val recording: StateFlow<Boolean> = repo.recordingFlow
    val startTime: StateFlow<Long> = repo.startTimeFlow
    val eventCount: StateFlow<Int> = repo.eventCountFlow
    val anomalyProbabilities: StateFlow<FloatArray> = repo.anomalyProbabilities
    
    private val _gpsActive = MutableStateFlow(false)
    val gpsActive: StateFlow<Boolean> = _gpsActive
    private val _isGpsEnabled = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled
    val gpsAccuracy: StateFlow<Float?> = repo.gpsAccuracyFlow
    val pendingUploads: StateFlow<Int> =
            tripDao.observePendingUploadCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalTrips: StateFlow<Int> =
            tripDao.observeCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalDistance: StateFlow<Float?> =
            tripDao.observeTotalDistance().stateIn(viewModelScope, SharingStarted.Lazily, 0f)
    val samplingRate: StateFlow<Int> =
            prefs.samplingRateFlow.stateIn(viewModelScope, SharingStarted.Lazily, 50)
    val sensitivityThreshold: StateFlow<Float> =
            prefs.sensitivityFlow.stateIn(viewModelScope, SharingStarted.Lazily, 2.0f)
    val userName: StateFlow<String> =
            kotlinx.coroutines.flow.flow {
                emit(prefs.getUserName() ?: "Pengguna")
            }.stateIn(viewModelScope, SharingStarted.Lazily, "Pengguna")
    val currentSpeedKmh: StateFlow<Float> =
            repo.currentSpeedFlow
                    .map { (it * 3.6f).coerceAtLeast(0f) }
                    .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    // Helper for fast FloatArray concatenation and truncation
    private fun appendWithLimit(current: FloatArray, newItems: FloatArray, limit: Int = 200): FloatArray {
        val totalSize = current.size + newItems.size
        if (totalSize <= limit) {
            val result = FloatArray(totalSize)
            System.arraycopy(current, 0, result, 0, current.size)
            System.arraycopy(newItems, 0, result, current.size, newItems.size)
            return result
        } else {
            val result = FloatArray(limit)
            val dropCount = totalSize - limit
            if (dropCount >= current.size) {
                // We drop all of current, just take the last 'limit' items of newItems
                System.arraycopy(newItems, newItems.size - limit, result, 0, limit)
            } else {
                // Keep some of current, and all of newItems (since newItems.size < limit)
                val keepFromCurrent = current.size - dropCount
                System.arraycopy(current, dropCount, result, 0, keepFromCurrent)
                System.arraycopy(newItems, 0, result, keepFromCurrent, newItems.size)
            }
            return result
        }
    }

    init {
        viewModelScope.launch {
            // UI Optimization: Batch updates to reduce recomposition frequency.
            // Using FloatArray minimizes memory churning (boxing) and fast native arraycopy
            val buffer = mutableListOf<com.pemalang.roaddamage.model.SensorReading>()
            var lastUpdate = System.currentTimeMillis()

            repo.readingsFlow.collect { r ->
                buffer.add(r)
                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 33) { // 33ms throttle (approx 30 FPS)
                    if (buffer.isNotEmpty()) {
                        val size = buffer.size
                        val newMags = FloatArray(size)
                        val newAx = FloatArray(size)
                        val newAy = FloatArray(size)
                        val newAz = FloatArray(size)
                        
                        for (i in 0 until size) {
                            val reading = buffer[i]
                            newMags[i] = reading.magnitude
                            newAx[i] = reading.accelX
                            newAy[i] = reading.accelY
                            newAz[i] = reading.accelZ
                        }

                        _magnitudes.value = appendWithLimit(_magnitudes.value, newMags, 200)
                        _ax.value = appendWithLimit(_ax.value, newAx, 200)
                        _ay.value = appendWithLimit(_ay.value, newAy, 200)
                        _az.value = appendWithLimit(_az.value, newAz, 200)

                        buffer.clear()
                    }
                    lastUpdate = now
                }
            }
        }
        viewModelScope.launch {
            repo.pointsFlow.collect { p ->
                val src = _points.value
                val dst = if (src.size >= 300) src.drop(src.size - 299) + p else src + p
                _points.value = dst
            }
        }
        viewModelScope.launch {
            val locationManager = app.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            while (true) {
                val last = repo.gpsLastTs.value
                val active = (System.currentTimeMillis() - last) <= 5000
                _gpsActive.value = active
                
                _isGpsEnabled.value = try {
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                } catch (e: Exception) {
                    false
                }
                
                delay(1000)
            }
        }
    }
}
