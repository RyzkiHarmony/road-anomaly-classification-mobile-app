package com.pemalang.roaddamage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pemalang.roaddamage.model.CameraEvent
import com.pemalang.roaddamage.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(trip: Trip)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCameraEvent(event: CameraEvent)

    @Query("SELECT * FROM trips ORDER BY createdAt DESC") fun observeAll(): Flow<List<Trip>>
    @Query("SELECT * FROM camera_events WHERE tripId = :tripId") fun observeCameraEvents(tripId: String): Flow<List<CameraEvent>>

    @Query("SELECT * FROM camera_events WHERE tripId = :tripId")
    suspend fun getCameraEvents(tripId: String): List<CameraEvent>

    @Query("SELECT * FROM trips WHERE tripId = :tripId LIMIT 1")
    suspend fun getById(tripId: String): Trip?

    @androidx.room.Delete suspend fun delete(trip: Trip)

    @Query("DELETE FROM trips WHERE tripId = :tripId") suspend fun deleteById(tripId: String)

    @Query("DELETE FROM camera_events WHERE tripId = :tripId") suspend fun deleteCameraEventsByTripId(tripId: String)

    @Query("SELECT * FROM trips") suspend fun getAll(): List<Trip>

    @Query("SELECT COUNT(*) FROM trips") fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trips WHERE uploadStatus != 'UPLOADED'") fun observePendingUploadCount(): Flow<Int>

    @Query("SELECT SUM(distance) FROM trips") fun observeTotalDistance(): Flow<Float?>
}
