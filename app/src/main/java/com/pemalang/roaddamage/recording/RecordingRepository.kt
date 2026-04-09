package com.pemalang.roaddamage.recording

import android.app.Application
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.model.CameraEvent
import com.pemalang.roaddamage.model.SensorReading
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import com.pemalang.roaddamage.domain.usecase.ProcessGpsReadingUseCase
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RecordingRepository
@Inject
constructor(
    private val app: Application,
    private val tripDao: TripDao,
    private val processGps: ProcessGpsReadingUseCase
) {
    private var currentTrip: Trip? = null
    private var lastTripId: String? = null
    private var writer: BufferedWriter? = null
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var totalDistance: Float = 0f
    private val _readings =
            MutableSharedFlow<SensorReading>(
                    replay = 0,
                    extraBufferCapacity = 256,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    private val _points =
            MutableSharedFlow<Pair<Double, Double>>(
                    replay = 0,
                    extraBufferCapacity = 256,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    private val _distance = MutableStateFlow(0f)
    private val _recording = MutableStateFlow(false)
    private val _startTime = MutableStateFlow(0L)
    private val _eventCount = MutableStateFlow(0)
    private val _gpsLastTs = MutableStateFlow(0L)
    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    private val _currentSpeed = MutableStateFlow(0f)
    private val _cameraTrigger =
            MutableSharedFlow<Float>(
                    replay = 0,
                    extraBufferCapacity = 8,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    val readingsFlow: MutableSharedFlow<SensorReading> = _readings
    val pointsFlow: MutableSharedFlow<Pair<Double, Double>> = _points
    val distanceFlow: StateFlow<Float> = _distance
    val recordingFlow: StateFlow<Boolean> = _recording
    val startTimeFlow: StateFlow<Long> = _startTime
    val eventCountFlow: StateFlow<Int> = _eventCount
    val gpsLastTs: StateFlow<Long> = _gpsLastTs
    val gpsAccuracyFlow: StateFlow<Float?> = _gpsAccuracy
    val currentSpeedFlow: StateFlow<Float> = _currentSpeed
    val cameraTrigger: MutableSharedFlow<Float> = _cameraTrigger

    // IO Optimization: Buffer for writing to file
    private val readingBuffer = ArrayList<SensorReading>(60)
    private val BATCH_SIZE = 50
    private val bufferMutex = Mutex()

    suspend fun startTrip(userId: String): Trip {
        val tripId = UUID.randomUUID().toString()
        val file = File(app.getExternalFilesDir(null), "$tripId.csv")
        val now = System.currentTimeMillis()
        val trip =
                Trip(
                        tripId = tripId,
                        userId = userId,
                        startTime = now,
                        endTime = 0,
                        duration = 0,
                        distance = 0f,
                        dataFilePath = file.absolutePath,
                        uploadStatus = UploadStatus.PENDING,
                        createdAt = now
                )
        withContext(Dispatchers.IO) {
            writer = BufferedWriter(FileWriter(file, true))
            writer?.write("timestamp,ax,ay,az,magnitude,lat,lon,alt,speed,accuracy,bearing\n")
            writer?.flush()
        }
        currentTrip = trip
        lastTripId = trip.tripId
        lastLat = null
        lastLon = null
        totalDistance = 0f
        _distance.value = 0f
        _recording.value = true
        _startTime.value = now
        _eventCount.value = 0
        tripDao.upsert(trip)
        return trip
    }

    fun incrementEventCount() {
        _eventCount.value += 1
    }

    suspend fun appendReading(reading: SensorReading) {
        bufferMutex.withLock { readingBuffer.add(reading) }

        if (readingBuffer.size >= BATCH_SIZE) {
            flushBufferSuspend()
        }

        _readings.tryEmit(reading)
        
        // Update real-time speed from GPS (m/s)
        if (!reading.speed.isNaN()) {
            _currentSpeed.value = reading.speed
        }

        // Delegate GPS validation and distance calculation to the domain Use Case
        val gpsResult = processGps(
            latitude = reading.latitude,
            longitude = reading.longitude,
            accuracy = reading.accuracy,
            prevLat = lastLat,
            prevLon = lastLon
        )

        if (gpsResult.isValid) {
            totalDistance += gpsResult.distanceDelta
            lastLat = reading.latitude
            lastLon = reading.longitude
            _points.tryEmit(reading.latitude to reading.longitude)
            _distance.value = totalDistance
            _gpsLastTs.value = System.currentTimeMillis()
            _gpsAccuracy.value = reading.accuracy
        }
    }

    suspend fun finishTrip(): Trip? {
        flushBufferSuspend()
        withContext(Dispatchers.IO) {
            writer?.flush()
            writer?.close()
            writer = null
        }
        val now = System.currentTimeMillis()
        val trip = currentTrip ?: return null
        val duration = ((now - trip.startTime) / 1000)
        val updated = trip.copy(endTime = now, duration = duration, distance = totalDistance)
        tripDao.upsert(updated)
        currentTrip = null
        _recording.value = false
        return updated
    }

    suspend fun triggerCamera(magnitude: Float) {
        _cameraTrigger.tryEmit(magnitude)
    }

    suspend fun saveCameraEvent(path: String, magnitude: Float) {
        // Fallback to lastTripId if currentTrip just ended
        val targetTripId = currentTrip?.tripId ?: lastTripId ?: return
        val eventId = UUID.randomUUID().toString()
        val event =
                CameraEvent(
                        eventId = eventId,
                        tripId = targetTripId,
                        timestamp = System.currentTimeMillis(),
                        latitude = lastLat,
                        longitude = lastLon,
                        imagePath = path,
                        triggerMagnitude = magnitude
                )
        tripDao.insertCameraEvent(event)
    }

    private suspend fun flushBufferSuspend() {
        val chunk =
                bufferMutex.withLock {
                    val c = ArrayList(readingBuffer)
                    readingBuffer.clear()
                    c
                }
        if (chunk.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                writer?.apply {
                    val sb = StringBuilder()
                    for (reading in chunk) {
                        sb.setLength(0)
                        sb.append(reading.timestamp).append(",")
                          .append(reading.accelX).append(",")
                          .append(reading.accelY).append(",")
                          .append(reading.accelZ).append(",")
                          .append(reading.magnitude).append(",")
                          .append(if (reading.latitude.isNaN()) "" else reading.latitude).append(",")
                          .append(if (reading.longitude.isNaN()) "" else reading.longitude).append(",")
                          .append(if (reading.altitude.isNaN()) "" else reading.altitude).append(",")
                          .append(if (reading.speed.isNaN()) "" else reading.speed).append(",")
                          .append(if (reading.accuracy.isNaN()) "" else reading.accuracy).append(",")
                          .append(if (reading.bearing.isNaN()) "" else reading.bearing).append("\n")
                        write(sb.toString())
                    }
                }
            }
        }
    }
}
