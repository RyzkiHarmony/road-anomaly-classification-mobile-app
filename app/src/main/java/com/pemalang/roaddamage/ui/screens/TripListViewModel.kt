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
import com.pemalang.roaddamage.data.remote.ApiService
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileWriter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class TripListViewModel
@Inject
constructor(
        private val app: Application,
        private val tripDao: TripDao,
        private val api: ApiService
) : ViewModel() {
    sealed class SaveEvent {
        data class Success(val uri: Uri?, val path: String?) : SaveEvent()
        data class Error(val message: String) : SaveEvent()
    }
    val events =
            MutableSharedFlow<SaveEvent>(
                    replay = 0,
                    extraBufferCapacity = 16,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    val trips: StateFlow<List<Trip>> =
            tripDao.observeAll()
                    .map { it }
                    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun uploadTrip(trip: Trip) {
        if (trip.uploadStatus == UploadStatus.UPLOADED ||
                        trip.uploadStatus == UploadStatus.UPLOADING
        )
                return
        viewModelScope.launch {
            val uploading = trip.copy(uploadStatus = UploadStatus.UPLOADING)
            tripDao.upsert(uploading)
            try {
                val userIdBody = trip.userId.toRequestBody("text/plain".toMediaType())
                val tripIdBody = trip.tripId.toRequestBody("text/plain".toMediaType())
                val metadataJson =
                        """{"duration":${trip.duration},"distance":${trip.distance},"startTime":${trip.startTime},"endTime":${trip.endTime}}"""
                val metadataBody = metadataJson.toRequestBody("application/json".toMediaType())
                val file = File(trip.dataFilePath)
                val fileBody = file.asRequestBody("text/csv".toMediaType())
                val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
                
                val resp = api.uploadTrip(userIdBody, tripIdBody, metadataBody, filePart)
                val ok = resp.isSuccessful && (resp.body()?.success == true)
                if (ok) {
                    tripDao.upsert(trip.copy(uploadStatus = UploadStatus.UPLOADED))
                } else {
                    tripDao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
                }
            } catch (t: Throwable) {
                tripDao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
            }
        }
    }

    fun saveLocal(trip: Trip) {
        viewModelScope.launch {
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
                events.tryEmit(SaveEvent.Success(null, destCsv.absolutePath))
            } catch (t: Throwable) {
                events.tryEmit(SaveEvent.Error("Gagal menyimpan: ${t.message ?: ""}"))
            }
        }
    }
    fun saveToDownloads(trip: Trip) {
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
                    val csvName = "RoadDamage_${dateStr}_${trip.tripId}.csv"
                    val jsonName = "RoadDamage_${dateStr}_${trip.tripId}.json"

                    val csvUri = insertDownloads(csvName, "text/csv")
                    if (csvUri != null) {
                        app.contentResolver.openOutputStream(csvUri)?.use { out ->
                            csvSrc.inputStream().use { inp -> inp.copyTo(out) }
                        }
                    }
                    val metaJson =
                            """{"tripId":"${trip.tripId}","userId":"${trip.userId}","startTime":${trip.startTime},"startTimeReadable":"$dateStr","endTime":${trip.endTime},"duration":${trip.duration},"distance":${trip.distance}}"""
                    val jsonUri = insertDownloads(jsonName, "application/json")
                    if (jsonUri != null) {
                        app.contentResolver.openOutputStream(jsonUri)?.use { out ->
                            out.write(metaJson.toByteArray())
                        }
                    }
                    events.tryEmit(SaveEvent.Success(csvUri, null))
                } catch (t: Throwable) {
                    events.tryEmit(
                            SaveEvent.Error("Gagal menyimpan ke Downloads: ${t.message ?: ""}")
                    )
                }
            } else {
                saveLocal(trip)
            }
        }
    }

    private fun insertDownloads(name: String, mime: String): Uri? {
        val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/RoadDamageDetector")
                    }
                }
        return if (Build.VERSION.SDK_INT >= 29) {
            app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            null
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            // 1. Delete CSV data file
            try { java.io.File(trip.dataFilePath).delete() } catch (_: Throwable) {}
            // 2. Delete trip record
            tripDao.delete(trip)
        }
    }

    fun enqueueUpload(trip: Trip) {
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
}
