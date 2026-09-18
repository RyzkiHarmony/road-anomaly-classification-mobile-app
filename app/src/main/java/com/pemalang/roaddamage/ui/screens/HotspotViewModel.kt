package com.pemalang.roaddamage.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.remote.AnomalyContextResponse
import com.pemalang.roaddamage.data.remote.AnomalyPointDto
import com.pemalang.roaddamage.data.remote.ApiService
import com.pemalang.roaddamage.data.repository.RagRepository
import com.pemalang.roaddamage.model.AnomalyEvent
import com.pemalang.roaddamage.util.GeocoderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HotspotFilter {
    ALL,
    POTHOLE,
    SPEED_BUMP
}

sealed class RagState {
    object Idle : RagState()
    object Loading : RagState()
    data class Success(val response: AnomalyContextResponse) : RagState()
    data class Error(val message: String) : RagState()
}

data class HotspotUiState(
    val isLoading: Boolean = false,
    val anomalies: List<AnomalyPointDto> = emptyList(),
    val filter: HotspotFilter = HotspotFilter.ALL,
    val isHeatmapMode: Boolean = false,
    val selectedAnomaly: AnomalyPointDto? = null,
    val selectedAddress: String? = null,
    val ragState: RagState = RagState.Idle,
    val errorMessage: String? = null
) {
    val filteredAnomalies: List<AnomalyPointDto>
        get() = when (filter) {
            HotspotFilter.ALL -> anomalies
            HotspotFilter.POTHOLE -> anomalies.filter { it.isPothole }
            HotspotFilter.SPEED_BUMP -> anomalies.filter { !it.isPothole }
        }

    val potholeCount: Int
        get() = anomalies.count { it.isPothole }

    val speedBumpCount: Int
        get() = anomalies.count { !it.isPothole }

    val totalCount: Int
        get() = anomalies.size
}

@HiltViewModel
class HotspotViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val ragRepository: RagRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HotspotUiState())
    val ui: StateFlow<HotspotUiState> = _ui.asStateFlow()

    init {
        loadAnomalies()
    }

    fun loadAnomalies() {
        if (_ui.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _ui.value = _ui.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = apiService.getAllAnomalies()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        anomalies = list,
                        errorMessage = if (list.isEmpty()) "Belum ada data titik anomali di PostgreSQL." else null
                    )
                } else {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal mengambil data dari server (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    errorMessage = "Koneksi ke server gagal: ${e.localizedMessage ?: "Cek jaringan / IP server"}"
                )
            }
        }
    }

    fun setFilter(filter: HotspotFilter) {
        _ui.value = _ui.value.copy(filter = filter)
    }

    fun toggleHeatmap() {
        _ui.value = _ui.value.copy(isHeatmapMode = !_ui.value.isHeatmapMode)
    }

    fun selectAnomaly(anomaly: AnomalyPointDto?) {
        if (anomaly == null) {
            _ui.value = _ui.value.copy(
                selectedAnomaly = null,
                selectedAddress = null,
                ragState = RagState.Idle
            )
            return
        }

        _ui.value = _ui.value.copy(
            selectedAnomaly = anomaly,
            selectedAddress = "Mendeteksi nama jalan...",
            ragState = RagState.Idle
        )

        viewModelScope.launch {
            val address = GeocoderHelper.getStreetName(
                context = context,
                latitude = anomaly.latitude,
                longitude = anomaly.longitude
            )
            if (_ui.value.selectedAnomaly?.id == anomaly.id) {
                _ui.value = _ui.value.copy(selectedAddress = address)
            }
        }
    }

    fun requestRagContext(anomaly: AnomalyPointDto) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(ragState = RagState.Loading)
            try {
                val event = AnomalyEvent(
                    eventId = anomaly.id,
                    tripId = anomaly.tripId,
                    timestamp = System.currentTimeMillis(),
                    latitude = anomaly.latitude,
                    longitude = anomaly.longitude,
                    anomalyType = if (anomaly.isPothole) "Pothole" else "Speed Bump",
                    confidence = anomaly.confidence
                )
                val response = ragRepository.getAnomalyContext(event, _ui.value.selectedAddress)
                _ui.value = _ui.value.copy(ragState = RagState.Success(response))
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    ragState = RagState.Error("Gagal memanggil AI: ${e.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }
}
