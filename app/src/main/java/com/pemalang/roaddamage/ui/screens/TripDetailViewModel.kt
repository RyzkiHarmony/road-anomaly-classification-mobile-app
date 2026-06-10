package com.pemalang.roaddamage.ui.screens

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.google.firebase.crashlytics.FirebaseCrashlytics

@HiltViewModel
class TripDetailViewModel
@Inject
constructor(private val app: Application, private val tripDao: TripDao) : ViewModel() {
    data class UiState(
            val trip: Trip? = null,
            val points: List<Pair<Double, Double>> = emptyList(),
            val magnitudes: List<Float> = emptyList(),
            val verticalG: List<Float> = emptyList(),
            val anomalyEvents: List<com.pemalang.roaddamage.model.AnomalyEvent> = emptyList()
    )
    sealed class Event {
        object Deleted : Event()
        data class Error(val message: String) : Event()
        data class Saved(val path: String) : Event()
        data class Share(val uri: Uri) : Event()
    }
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    val events =
            MutableSharedFlow<Event>(
                    replay = 0,
                    extraBufferCapacity = 8,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

    suspend fun load(tripId: String) {
        val trip = tripDao.getById(tripId)
        if (trip == null) {
            kotlinx.coroutines.delay(500) // Allow UI collector to start
            events.tryEmit(Event.Error("Trip tidak ditemukan"))
            events.tryEmit(Event.Deleted) // Trigger onBack() to close the endless buffering screen
            return
        }
        val pts = mutableListOf<Pair<Double, Double>>()
        val mags = mutableListOf<Float>()
        val vertG = mutableListOf<Float>()
        val anomalyEventsList = tripDao.getAnomalyEventsForTrip(tripId)
        _ui.value = UiState(trip = trip, points = pts, magnitudes = mags, verticalG = vertG, anomalyEvents = anomalyEventsList)
        
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(trip.dataFilePath)
                if (file.exists()) {
                // Downsampling strategy for large files to prevent OOM and UI lag
                // Target approx 3000 points for optimal graph/map performance
                val fileLen = file.length()
                val estLines = fileLen / 60 // Estimate 60 bytes per line
                val targetPoints = 3000
                val step = (estLines / targetPoints).toInt().coerceAtLeast(1)
                
                BufferedReader(FileReader(file)).use { br ->
                    var line = br.readLine()
                    if (line != null) {
                        val headers = line.lowercase().split(",").map { it.trim() }
                        val tsIdx = headers.indexOf("timestamp")
                        val axIdx = headers.indexOf("ax")
                        val ayIdx = headers.indexOf("ay")
                        val azIdx = headers.indexOf("az")
                        val magIdx = headers.indexOf("magnitude")
                        val latIdx = headers.indexOf("lat")
                        val lonIdx = headers.indexOf("lon")

                        var index = 0
                        line = br.readLine()
                        while (line != null) {
                            if (index % step == 0) {
                                val parts = line.split(",")
                                if (latIdx != -1 && lonIdx != -1 && parts.size > latIdx && parts.size > lonIdx) {
                                    val ax = if (axIdx != -1 && parts.size > axIdx) parts[axIdx].toFloatOrNull() ?: 0f else 0f
                                    val ay = if (ayIdx != -1 && parts.size > ayIdx) parts[ayIdx].toFloatOrNull() ?: 0f else 0f
                                    val az = if (azIdx != -1 && parts.size > azIdx) parts[azIdx].toFloatOrNull() ?: 0f else 0f
                                    
                                    val mag = if (magIdx != -1 && parts.size > magIdx) {
                                        parts[magIdx].toFloatOrNull()
                                    } else {
                                        kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                                    }
                                    
                                    val lat = parts[latIdx].toDoubleOrNull()
                                    val lon = parts[lonIdx].toDoubleOrNull()
                                    
                                    if (mag != null) mags.add(mag)
                                    vertG.add(ay)
                                    if (lat != null && lon != null && !lat.isNaN() && !lon.isNaN()) {
                                        pts.add(lat to lon)
                                    }
                                }
                            }
                            index++
                            line = br.readLine()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(t)
            events.tryEmit(Event.Error("Gagal membaca file: ${t.message ?: ""}"))
        }
        }
        _ui.value = _ui.value.copy(points = pts, magnitudes = mags, verticalG = vertG)
    }

    fun enqueueUpload() {
        val trip = _ui.value.trip ?: return
        if (trip.uploadStatus == UploadStatus.UPLOADED ||
                        trip.uploadStatus == UploadStatus.UPLOADING
        )
                return
        viewModelScope.launch { tripDao.upsert(trip.copy(uploadStatus = UploadStatus.UPLOADING)) }
        val input = Data.Builder().putString("tripId", trip.tripId).build()
        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request =
                OneTimeWorkRequestBuilder<com.pemalang.roaddamage.work.TripUploadWorker>()
                        .setInputData(input)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                        .addTag("upload_trip_${trip.tripId}")
                        .build()
        WorkManager.getInstance(app)
                .enqueueUniqueWork(
                        "upload_trip_${trip.tripId}",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        request
                )
    }

    fun deleteTrip() {
        val trip = _ui.value.trip ?: return
        viewModelScope.launch {
            try {
                // 1. Delete CSV data file
                try { File(trip.dataFilePath).delete() } catch (_: Throwable) {}
                // 2. Delete trip record
                tripDao.deleteById(trip.tripId)
                events.tryEmit(Event.Deleted)
            } catch (t: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(t)
                events.tryEmit(Event.Error("Gagal menghapus: ${t.message ?: ""}"))
            }
        }
    }

    fun shareTrip() {
        val trip = _ui.value.trip ?: return
        viewModelScope.launch {
            try {
                val file = File(trip.dataFilePath)
                if (file.exists()) {
                    val uri =
                            androidx.core.content.FileProvider.getUriForFile(
                                    app,
                                    "${app.packageName}.provider",
                                    file
                            )
                    events.tryEmit(Event.Share(uri))
                } else {
                    events.tryEmit(Event.Error("File data tidak ditemukan"))
                }
            } catch (t: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(t)
                events.tryEmit(Event.Error("Gagal membagikan: ${t.message}"))
            }
        }
    }

    fun saveToDownloads() {
        val trip = _ui.value.trip ?: return
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    val csvSrc = File(trip.dataFilePath)
                    val dateFormat =
                            java.text.SimpleDateFormat(
                                    "yyyy-MM-dd_HH-mm-ss",
                                    java.util.Locale.getDefault()
                            )
                    val dateStr = dateFormat.format(java.util.Date(trip.startTime))
                    val csvName = "RoadDamage_$dateStr.csv"
                    val jsonName = "RoadDamage_$dateStr.json"
                    val folderName = "Trip_$dateStr"

                    val csvUri = insertDownloads(csvName, "text/csv", folderName)
                    if (csvUri != null) {
                        app.contentResolver.openOutputStream(csvUri)?.use { out ->
                            csvSrc.inputStream().use { inp -> inp.copyTo(out) }
                        }
                    }

                    val metaJson =
                            """{"tripId":"${trip.tripId}","userId":"${trip.userId}","startTime":${trip.startTime},"startTimeReadable":"$dateStr","endTime":${trip.endTime},"duration":${trip.duration},"distance":${trip.distance}}"""
                    val jsonUri = insertDownloads(jsonName, "application/json", folderName)
                    if (jsonUri != null) {
                        app.contentResolver.openOutputStream(jsonUri)?.use { out ->
                            out.write(metaJson.toByteArray())
                        }
                    }

                    events.tryEmit(Event.Saved("Tersimpan di Download/RoadDamageDetector/$folderName"))
                } catch (t: Throwable) {
                    FirebaseCrashlytics.getInstance().recordException(t)
                    events.tryEmit(Event.Error("Gagal menyimpan ke Downloads: ${t.message ?: ""}"))
                }
            } else {
                saveLocal(trip)
            }
        }
    }

    private fun saveLocal(trip: Trip) {
        try {
            val src = File(trip.dataFilePath)
            val exportDir = File(app.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val destCsv = File(exportDir, src.name)
            src.copyTo(destCsv, overwrite = true)

            val meta = File(exportDir, "${trip.tripId}.json")
            val metaJson =
                    """{"tripId":"${trip.tripId}","userId":"${trip.userId}","startTime":${trip.startTime},"endTime":${trip.endTime},"duration":${trip.duration},"distance":${trip.distance},"csv":"${destCsv.absolutePath}"}"""
            FileWriter(meta, false).use { it.write(metaJson) }

            events.tryEmit(Event.Saved("Tersimpan di ${destCsv.parent}"))
        } catch (t: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(t)
            events.tryEmit(Event.Error("Gagal menyimpan: ${t.message ?: ""}"))
        }
    }

    private fun insertDownloads(name: String, mime: String, subfolder: String = ""): Uri? {
        val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (Build.VERSION.SDK_INT >= 29) {
                        val path = if (subfolder.isEmpty()) "Download/RoadDamageDetector" else "Download/RoadDamageDetector/$subfolder"
                        put(MediaStore.Downloads.RELATIVE_PATH, path)
                    }
                }
        return if (Build.VERSION.SDK_INT >= 29) {
            app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            null
        }
    }
}
