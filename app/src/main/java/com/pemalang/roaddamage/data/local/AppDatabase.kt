package com.pemalang.roaddamage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pemalang.roaddamage.model.AnomalyEvent
import com.pemalang.roaddamage.model.Trip

@Database(entities = [Trip::class, AnomalyEvent::class], version = 7, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
