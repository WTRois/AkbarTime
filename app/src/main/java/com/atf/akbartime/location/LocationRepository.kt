package com.atf.akbartime.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.atf.akbartime.data.UserLocation
import java.util.TimeZone

class LocationRepository(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): UserLocation? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        return bestLocation?.let {
            UserLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                timezoneId = TimeZone.getDefault().id,
                cityLabel = "Detected Location"
            )
        }
    }

    fun getPresetCities(): List<UserLocation> {
        return listOf(
            UserLocation(-6.2088, 106.8456, "Asia/Jakarta", "Jakarta"),
            UserLocation(-7.2575, 112.7521, "Asia/Jakarta", "Surabaya"),
            UserLocation(-6.9175, 107.6191, "Asia/Jakarta", "Bandung"),
            UserLocation(-5.1476, 119.4327, "Asia/Makassar", "Makassar"),
            UserLocation(3.5952, 98.6722, "Asia/Jakarta", "Medan"),
            UserLocation(-7.7956, 110.3695, "Asia/Jakarta", "Yogyakarta")
        )
    }
}
