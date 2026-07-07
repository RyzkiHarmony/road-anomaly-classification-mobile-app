package com.pemalang.roaddamage.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pemalang.roaddamage.R
import com.pemalang.roaddamage.data.prefs.UserPrefs
import com.pemalang.roaddamage.model.SensorReading
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.sensors.AccelerometerHandler
import com.pemalang.roaddamage.sensors.GPSHandler
import com.pemalang.roaddamage.sensors.GyroscopeHandler
import com.pemalang.roaddamage.sensors.LinearAccelerationHandler
import com.pemalang.roaddamage.sensors.GravityHandler
import com.pemalang.roaddamage.work.TripUploadWorker
import com.pemalang.roaddamage.domain.OnnxModelRunner
import com.pemalang.roaddamage.domain.SensorFusionProcessor
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
import kotlinx.coroutines.withContext
import android.util.Log

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var userPrefs: UserPrefs
    @Inject lateinit var repository: RecordingRepository

    private var wakeLock: PowerManager.WakeLock? = null
    private var scope: CoroutineScope? = null
    private var accel: AccelerometerHandler? = null
    private var gyro: GyroscopeHandler? = null
    private var linearAccel: LinearAccelerationHandler? = null
    private var gravity: GravityHandler? = null
    private var gps: GPSHandler? = null
    private var collectingJob: Job? = null
    private var latestLocation: Location? = null
    private var lastTriggerTime: Long = 0
    private val COOLDOWN_MS = 2000L // 2 seconds cooldown
    
    private var onnxRunner: OnnxModelRunner? = null
    private var fusionProcessor: SensorFusionProcessor? = null

    // Snapshot of the latest gyroscope reading (rad/s).
    // Updated asynchronously; read on each accelerometer tick.
    @Volatile
    private var latestGyro: FloatArray = floatArrayOf(Float.NaN, Float.NaN, Float.NaN)

    @Volatile
    private var latestLinearAccel: FloatArray = floatArrayOf(Float.NaN, Float.NaN, Float.NaN)

    @Volatile
    private var latestGravity: FloatArray = floatArrayOf(Float.NaN, Float.NaN, Float.NaN)

    companion object {
        private const val TAG = "RecordingService"
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
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        wakeLock = null

        // Stop sensors and jobs to prevent background leakage
        try {
            onnxRunner?.close()
            onnxRunner = null
            fusionProcessor = null
            
            accel?.stop()
            gyro?.stop()
            linearAccel?.stop()
            gravity?.stop()
            gps?.stop()
            collectingJob?.cancel()
            scope?.cancel()
            scope = null
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification("Merekam data"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, buildNotification("Merekam data"))
        }

        // Battery Optimization: Use shorter WakeLock with periodic re-acquire
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoadDamageDetector::Recording")
            wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        val sManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val samplingHz = runBlocking { userPrefs.getSamplingRateHz() }
        val samplingUs = 5000
        val gpsInterval = runBlocking { userPrefs.getGpsIntervalSec() }
        try {
            accel = AccelerometerHandler(sManager, samplingUs)
            gyro = GyroscopeHandler(sManager, samplingUs)
            linearAccel = LinearAccelerationHandler(sManager, samplingUs)
            gravity = GravityHandler(sManager, samplingUs)
            gps = GPSHandler(application, gpsInterval.toLong())
            fusionProcessor = SensorFusionProcessor()
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Failed to initialize Sensors", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            stopSelf()
            return
        }

        scope = CoroutineScope(Dispatchers.Default)
        val sc = scope!!

        collectingJob =
                sc.launch {
                    try {
                        // Load ONNX Model on Background Thread to prevent Main Thread blocking (ANR)
                        onnxRunner = OnnxModelRunner(this@RecordingService)
                        onnxRunner?.initialize()
                        Log.d(TAG, "ONNX + Fusion initialized. onnxRunner=$onnxRunner, fusionProcessor=$fusionProcessor")
                    } catch (e: Exception) {
                        Log.e(TAG, "FATAL: Failed to initialize ONNX Model", e)
                        FirebaseCrashlytics.getInstance().recordException(e)
                        
                        // Notify user and gracefully shut down service
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                this@RecordingService, 
                                "Gagal memuat AI Model: ${e.message}", 
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        stopSelf()
                        return@launch
                    }
                    try {
                        val userId = userPrefs.getOrCreateUserId()
                        repository.startTrip(userId)
                        accel?.start()
                        gyro?.start()
                        linearAccel?.start()
                        gravity?.start()
                        launch { gps?.start() }
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }

                    // Pipe raw sensors directly to SensorFusionProcessor for independent interpolation
                    launch {
                        gyro?.readings?.collect { data ->
                            fusionProcessor?.addGyro(data)
                        }
                    }

                    launch {
                        linearAccel?.readings?.collect { data ->
                            fusionProcessor?.addLinearAccel(data)
                        }
                    }

                    launch {
                        gravity?.readings?.collect { data ->
                            fusionProcessor?.addGravity(data)
                        }
                    }

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
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                        }
                    }

                    launch {
                        gps?.locations?.collect { loc ->
                            latestLocation = loc
                            fusionProcessor?.updateSpeed(loc.speed)
                        }
                    }
                }
                
        // Listen to ONNX Inference Triggers
        scope?.launch {
            Log.d(TAG, "Starting inferenceTrigger collector...")
            fusionProcessor?.inferenceTrigger?.collect { tensorData ->
                // Log.d(TAG, "<<< Received inference trigger (${tensorData.size} floats)")
                try {
                    val probs = onnxRunner?.predict(tensorData)
                    val endTime = System.nanoTime()
                    
                    if (probs != null) {
                        val startTime = fusionProcessor?.lastPipelineStartTime ?: endTime
                        val latencyMs = (endTime - startTime) / 1_000_000.0
                        
                        Log.d(TAG, "===============================================")
                        Log.d(TAG, "END-TO-END LATENCY (Preproc + ONNX): $latencyMs ms")
                        Log.d(TAG, "===============================================")
                        
                        // Log.d(TAG, "<<< ONNX Result: None=${probs[0]}, Pothole=${probs[1]}, SpeedBump=${probs[2]}")
                        repository.updateAnomalyProbabilities(probs)
                        
                        // probs: [0]=Non-Event, [1]=Pothole, [2]=SpeedBump
                        val noneProb = probs[0]
                        val potholeProb = probs[1]
                        val speedBumpProb = probs[2]
                        
                        // Argmax with Confidence Threshold
                        // Mencegah False Positives (Spam Alert) di permukaan jalan kerikil
                        val CONFIDENCE_THRESHOLD = 0.60f
                        val maxProb = maxOf(noneProb, maxOf(potholeProb, speedBumpProb))
                        val isPotholeDetected = (maxProb == potholeProb) && (potholeProb >= CONFIDENCE_THRESHOLD)
                        val isSpeedBumpDetected = (maxProb == speedBumpProb) && (speedBumpProb >= CONFIDENCE_THRESHOLD)
                        
                        if (isPotholeDetected || isSpeedBumpDetected) {
                            val now = System.currentTimeMillis()
                            if (now - lastTriggerTime > COOLDOWN_MS) {
                                lastTriggerTime = now
                                repository.incrementEventCount()
                                val type = if (isPotholeDetected) "Pothole" else "Speed Bump"
                                val conf = maxProb
                                val loc = latestLocation
                                if (loc != null) {
                                    repository.saveAnomalyEvent(now, loc.latitude, loc.longitude, type, conf)
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    // Log to local file for debugging
                    try {
                        val logFile = java.io.File(getExternalFilesDir(null), "onnx_error.txt")
                        logFile.appendText("Error: ${e.message}\n${android.util.Log.getStackTraceString(e)}\n\n")
                    } catch (ioe: Exception) {
                        // ignore
                    }
                }
            }
        }

        // Collect fully fused and synchronized data from SensorFusionProcessor
        scope?.launch {
            fusionProcessor?.fusedSensorStream?.collect { reading ->
                val loc = latestLocation
                val finalReading = reading.copy(
                    latitude = loc?.latitude ?: Double.NaN,
                    longitude = loc?.longitude ?: Double.NaN,
                    altitude = loc?.altitude ?: Double.NaN,
                    speed = loc?.speed ?: Float.NaN,
                    accuracy = loc?.accuracy ?: Float.NaN,
                    bearing = loc?.bearing ?: Float.NaN,
                    probNone = repository.anomalyProbabilities.value[0],
                    probPothole = repository.anomalyProbabilities.value[1],
                    probSpeedbump = repository.anomalyProbabilities.value[2]
                )
                repository.appendReading(finalReading)
            }
        }
    }

    private fun stopRecording() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val trip = repository.finishTrip()
                if (trip != null) {
                    checkAutoUpload(trip)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private suspend fun checkAutoUpload(trip: Trip) {
        try {
            val auto = userPrefs.getAutoUpload()
            if (auto) {
                scheduleUpload(trip)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun scheduleUpload(trip: Trip) {
        try {
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
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
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
