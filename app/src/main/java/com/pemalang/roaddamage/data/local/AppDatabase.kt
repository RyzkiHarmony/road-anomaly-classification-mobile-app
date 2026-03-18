package com.pemalang.roaddamage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pemalang.roaddamage.model.CameraEvent
import com.pemalang.roaddamage.model.Trip

@Database(entities = [Trip::class, CameraEvent::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
