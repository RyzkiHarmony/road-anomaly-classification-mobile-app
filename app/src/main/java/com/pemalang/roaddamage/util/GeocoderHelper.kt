package com.pemalang.roaddamage.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Utility for converting GPS coordinates to human-readable street names in Indonesia (Pemalang).
 * Includes memory caching and backward compatibility for all Android versions.
 */
object GeocoderHelper {
    private val addressCache = ConcurrentHashMap<String, String>()

    suspend fun getStreetName(context: Context, latitude: Double, longitude: Double): String {
        if (latitude == 0.0 && longitude == 0.0) return "Koordinat tidak valid"

        val cacheKey = "%.4f,%.4f".format(Locale.US, latitude, longitude)
        addressCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                if (!Geocoder.isPresent()) {
                    return@withContext formatCoordinates(latitude, longitude)
                }

                val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }

                val formatted = formatAddress(address, latitude, longitude)
                addressCache[cacheKey] = formatted
                formatted
            } catch (e: Exception) {
                formatCoordinates(latitude, longitude)
            }
        }
    }

    private fun formatAddress(address: Address?, latitude: Double, longitude: Double): String {
        if (address == null) return formatCoordinates(latitude, longitude)

        val street = address.thoroughfare
        val subLocality = address.subLocality // e.g., Desa/Kelurahan
        val locality = address.locality ?: address.subAdminArea // e.g., Pemalang

        return when {
            !street.isNullOrBlank() && !locality.isNullOrBlank() -> "$street, $locality"
            !street.isNullOrBlank() -> street
            !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
            !locality.isNullOrBlank() -> locality
            address.maxAddressLineIndex >= 0 -> {
                val line = address.getAddressLine(0)
                if (line.length > 45) line.substring(0, 42) + "..." else line
            }
            else -> formatCoordinates(latitude, longitude)
        }
    }

    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "%s (%.4f, %.4f)".format(Locale.US, AppConstants.REGION_NAME, latitude, longitude)
    }
}
