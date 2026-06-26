package com.pemalang.roaddamage.data.prefs

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class UserPrefs
@Inject
constructor(private val app: Application, private val store: DataStore<Preferences>) {
    private val userIdKey = stringPreferencesKey("user_id")
    private val samplingHzKey = intPreferencesKey("sampling_hz")
    private val gpsIntervalKey = intPreferencesKey("gps_interval_sec")
    private val autoUploadKey = booleanPreferencesKey("auto_upload")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val vehicleTypeKey = stringPreferencesKey("vehicle_type")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    val samplingRateFlow: Flow<Int> = store.data.map { prefs -> prefs[samplingHzKey] ?: 100 }

    suspend fun isOnboardingCompleted(): Boolean {
        val prefs = store.data.first()
        return prefs[onboardingCompletedKey] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun getOrCreateUserId(): String {
        val prefs = store.data.first()
        val existing = prefs[userIdKey]
        if (existing != null) return existing
        val id = UUID.randomUUID().toString()
        store.edit { it[userIdKey] = id }
        return id
    }

    suspend fun getSamplingRateHz(): Int {
        val prefs = store.data.first()
        return prefs[samplingHzKey] ?: 100
    }
    suspend fun setSamplingRateHz(hz: Int) {
        store.edit { it[samplingHzKey] = hz }
    }

    suspend fun getGpsIntervalSec(): Int {
        val prefs = store.data.first()
        return prefs[gpsIntervalKey] ?: 1
    }
    suspend fun setGpsIntervalSec(sec: Int) {
        store.edit { it[gpsIntervalKey] = sec }
    }

    suspend fun getAutoUpload(): Boolean {
        val prefs = store.data.first()
        return prefs[autoUploadKey] ?: false
    }
    suspend fun setAutoUpload(enabled: Boolean) {
        store.edit { it[autoUploadKey] = enabled }
    }

    suspend fun getUserName(): String? {
        val prefs = store.data.first()
        return prefs[userNameKey]
    }
    suspend fun setUserName(name: String?) {
        store.edit { if (name.isNullOrBlank()) it.remove(userNameKey) else it[userNameKey] = name }
    }

    suspend fun getUserEmail(): String? {
        val prefs = store.data.first()
        return prefs[userEmailKey]
    }
    suspend fun setUserEmail(email: String?) {
        store.edit {
            if (email.isNullOrBlank()) it.remove(userEmailKey) else it[userEmailKey] = email
        }
    }

    suspend fun getVehicleType(): String? {
        val prefs = store.data.first()
        return prefs[vehicleTypeKey]
    }
    suspend fun setVehicleType(vehicle: String?) {
        store.edit {
            if (vehicle.isNullOrBlank()) it.remove(vehicleTypeKey) else it[vehicleTypeKey] = vehicle
        }
    }


}
