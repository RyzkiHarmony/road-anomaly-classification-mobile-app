package com.pemalang.roaddamage.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pemalang.roaddamage.R
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.data.remote.ApiService
import com.pemalang.roaddamage.model.UploadStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class TripUploadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun tripDao(): TripDao
        fun apiService(): ApiService
    }

    override suspend fun doWork(): Result {
        val tripId = inputData.getString("tripId") ?: return Result.failure()

        // Use Hilt EntryPoint to get dependencies
        val entryPoint =
            EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val dao = entryPoint.tripDao()
        val api = entryPoint.apiService()

        val trip = dao.getById(tripId) ?: return Result.failure()

        return try {
            val userIdBody = "user-id-placeholder".toRequestBody("text/plain".toMediaTypeOrNull())
            val tripIdBody = tripId.toRequestBody("text/plain".toMediaTypeOrNull())
            val metadataJson = """{"version":"1.0","app":"RoadDamageDetector","timestamp":${System.currentTimeMillis()}}"""
            // Security: explicitly define payload type for parsing protection
            val metadataBody = metadataJson.toRequestBody("application/json".toMediaTypeOrNull())
            val file = File(trip.dataFilePath)
            
            if (!file.exists() || file.length() == 0L) {
                 dao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
                 return Result.failure()
            }
            
            // Security: explicitly set text/csv to avoid unknown binary execution vulnerabilities at server
            val fileBody = file.asRequestBody("text/csv".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
            
            val resp = api.uploadTrip(userIdBody, tripIdBody, metadataBody, filePart)
            val ok = resp.isSuccessful && (resp.body()?.success == true)
            
            if (ok) {
                dao.upsert(trip.copy(uploadStatus = UploadStatus.UPLOADED))
                notify("Unggah Perjalanan", "Berhasil mengunggah perjalanan")
                Result.success()
            } else {
                // Don't mark as FAILED immediately if it's a server error, allow retry
                if (resp.code() in 500..599) {
                    Result.retry()
                } else {
                    dao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
                    notify("Unggah Perjalanan", "Gagal: Server menolak data")
                    Result.failure()
                }
            }
        } catch (t: Throwable) {
            // Network error, retry
            notify("Unggah Perjalanan", "Gagal: Masalah jaringan, akan dicoba lagi")
            Result.retry()
        }
    }

    private fun notify(title: String, message: String) {
        val nm =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch =
                    NotificationChannel(
                            "upload_status",
                            "Status Unggahan",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            nm.createNotificationChannel(ch)
        }
        val notif =
                NotificationCompat.Builder(applicationContext, "upload_status")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
