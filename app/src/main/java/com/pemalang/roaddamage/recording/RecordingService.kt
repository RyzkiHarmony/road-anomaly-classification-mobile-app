package com.pemalang.roaddamage.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pemalang.roaddamage.R
import com.pemalang.roaddamage.data.prefs.UserPrefs
import com.pemalang.roaddamage.model.SensorReading
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.sensors.AccelerometerHandler
import com.pemalang.roaddamage.sensors.GPSHandler
import com.pemalang.roaddamage.work.TripUploadWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var userPrefs: UserPrefs
    @Inject lateinit var repository: RecordingRepository

    private var wakeLock: PowerManager.WakeLock? = null
    private var scope: CoroutineScope? = null
    private var accel: AccelerometerHandler? = null
    private var gps: GPSHandler? = null
    private var collectingJob: Job? = null
    private var latestLocation: Location? = null
    private var lastTriggerTime: Long = 0
    private val COOLDOWN_MS = 2000L // 2 seconds cooldown

    companion object {
        const val CHANNEL_ID = "rdd_recording"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.pemalang.roaddamage.START"
        const val ACTION_STOP = "com.pemalang.roaddamage.STOP"


        // Battery Optimization: WakeLock re-acquire interval
        private const val WAKELOCK_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes
        private const val WAKELOCK_TIMEOUT_MS = 12 * 60 * 1000L  // 12 minutes (safety margin)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release WakeLock safely when service is destroyed
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null

        // Stop sensors and jobs to prevent background leakage
        accel?.stop()
        gps?.stop()
        collectingJob?.cancel()
        scope?.cancel()
        scope = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START) {
            startRecording()
            return START_STICKY
        }
        if (action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Merekam data"))

        // Battery Optimization: Use shorter WakeLock with periodic re-acquire
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoadDamageDetector::Recording")
        wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)

        val sManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val samplingHz = runBlocking { userPrefs.getSamplingRateHz() }
        val samplingUs = (1_000_000 / samplingHz).coerceAtLeast(5_000)
        val gpsInterval = runBlocking { userPrefs.getGpsIntervalSec() }
        // Ensure threshold is at least 1.4G to prevent triggering on engine vibration (1.0G +
        // noise)
        val threshold = runBlocking { userPrefs.getSensitivityThreshold() }.coerceAtLeast(1.4f)
        accel = AccelerometerHandler(sManager, samplingUs)
        gps = GPSHandler(application, gpsInterval.toLong())
        scope = CoroutineScope(Dispatchers.Default)
        val sc = scope!!

        collectingJob =
                sc.launch {
                    val userId = userPrefs.getOrCreateUserId()
                    repository.startTrip(userId)
                    accel?.start()
                    launch { gps?.start() }

                    // Observe location and distance to update notification
                    launch {
                        repository.distanceFlow.collect { dist ->
                            val notif = buildNotification("Merekam: %.2f km".format(dist / 1000f))
                            val nm =
                                    getSystemService(Context.NOTIFICATION_SERVICE) as
                                            NotificationManager
                            nm.notify(NOTIF_ID, notif)
                        }
                    }

                    // Battery Optimization: Periodically re-acquire WakeLock
                    // instead of holding a single long WakeLock.
                    launch {
                        while (true) {
                            kotlinx.coroutines.delay(WAKELOCK_INTERVAL_MS)
                            try {
                                if (wakeLock?.isHeld == true) wakeLock?.release()
                                wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    gps?.locations?.collect { loc ->
                        latestLocation = loc
                    }
                }

        scope?.launch {
            accel?.readings?.collect { arr ->
                val x = arr[0]
                val y = arr[1]
                val z = arr[2]
                val m = arr[3]
                val loc = latestLocation
                val lat = loc?.latitude ?: Double.NaN
                val lon = loc?.longitude ?: Double.NaN
                val alt = loc?.altitude ?: Double.NaN
                val spd = loc?.speed ?: Float.NaN
                val acc = loc?.accuracy ?: Float.NaN
                val brg = loc?.bearing ?: Float.NaN
                val reading =
                        SensorReading(
                                timestamp = System.currentTimeMillis(),
                                accelX = x,
                                accelY = y,
                                accelZ = z,
                                magnitude = m,
                                latitude = lat,
                                longitude = lon,
                                altitude = alt,
                                speed = spd,
                                accuracy = acc,
                                bearing = brg
                        )

                // Convert magnitude (m/s^2) to G-Force for threshold comparison
                val gForce = m / 9.80665f
                val now = System.currentTimeMillis()

                if (gForce > threshold && (now - lastTriggerTime > COOLDOWN_MS)) {
                    lastTriggerTime = now
                    repository.incrementEventCount()
                    repository.triggerCamera(gForce)
                }
                repository.appendReading(reading)
            }
        }
    }

    private fun stopRecording() {
        launchFinish()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun launchFinish() {
        CoroutineScope(Dispatchers.Default).launch {
            val trip = repository.finishTrip()
            if (trip != null) {
                checkAutoUpload(trip)
            }
        }
    }

    private suspend fun checkAutoUpload(trip: Trip) {
        val auto = userPrefs.getAutoUpload()
        if (auto) {
            scheduleUpload(trip)
        }
    }

    private fun scheduleUpload(trip: Trip) {
        val input = Data.Builder().putString("tripId", trip.tripId).build()

        // WiFi Only constraint for Auto Upload
        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()

        val request =
                OneTimeWorkRequestBuilder<TripUploadWorker>()
                        .setInputData(input)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                        .addTag("upload_trip_${trip.tripId}")
                        .build()

        WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork("upload_trip_${trip.tripId}", ExistingWorkPolicy.KEEP, request)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch =
                    NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Road Damage Detector")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
    }

}
