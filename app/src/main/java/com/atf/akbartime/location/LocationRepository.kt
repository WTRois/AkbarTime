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
            UserLocation(-7.7956, 110.3695, "Asia/Jakarta", "Yogyakarta"),
            UserLocation(-6.4025, 106.7942, "Asia/Jakarta", "Depok"),
            UserLocation(-6.2383, 106.9756, "Asia/Jakarta", "Bekasi"),
            UserLocation(-6.5971, 106.7974, "Asia/Jakarta", "Bogor"),
            UserLocation(-6.1764, 106.6329, "Asia/Jakarta", "Tangerang"),
            UserLocation(-6.9667, 110.4167, "Asia/Jakarta", "Semarang"),
            UserLocation(-7.9833, 112.6333, "Asia/Jakarta", "Malang"),
            UserLocation(-7.5667, 110.8167, "Asia/Jakarta", "Solo"),
            UserLocation(3.5952, 98.6722, "Asia/Jakarta", "Medan"),
            UserLocation(0.5071, 101.4478, "Asia/Jakarta", "Pekanbaru"),
            UserLocation(-0.9471, 100.4172, "Asia/Jakarta", "Padang"),
            UserLocation(-2.9761, 104.7754, "Asia/Jakarta", "Palembang"),
            UserLocation(-5.4292, 105.2611, "Asia/Jakarta", "Bandar Lampung"),
            UserLocation(5.5483, 95.3238, "Asia/Jakarta", "Banda Aceh"),
            UserLocation(0.0, 109.3333, "Asia/Pontianak", "Pontianak"),
            UserLocation(-3.3167, 114.5833, "Asia/Makassar", "Banjarmasin"),
            UserLocation(-1.2667, 116.8333, "Asia/Makassar", "Balikpapan"),
            UserLocation(-0.5022, 117.1536, "Asia/Makassar", "Samarinda"),
            UserLocation(-5.1476, 119.4327, "Asia/Makassar", "Makassar"),
            UserLocation(-1.61, 103.61, "Asia/Jakarta", "Jambi"),
            UserLocation(1.4748, 124.8484, "Asia/Makassar", "Manado"),
            UserLocation(-3.9722, 122.5149, "Asia/Makassar", "Kendari"),
            UserLocation(-0.8917, 119.8707, "Asia/Makassar", "Palu"),
            UserLocation(-8.6705, 115.2126, "Asia/Makassar", "Denpasar"),
            UserLocation(-8.5833, 116.1167, "Asia/Makassar", "Mataram"),
            UserLocation(-10.1772, 123.607, "Asia/Makassar", "Kupang"),
            UserLocation(-3.6954, 128.1814, "Asia/Jayapura", "Ambon"),
            UserLocation(-2.5337, 140.7181, "Asia/Jayapura", "Jayapura"),
            UserLocation(-0.8615, 134.062, "Asia/Jayapura", "Manokwari"),
            UserLocation(-6.12, 106.15, "Asia/Jakarta", "Serang"),
            UserLocation(-2.1, 106.1, "Asia/Jakarta", "Pangkal Pinang"),
            UserLocation(1.08, 104.03, "Asia/Jakarta", "Batam"),
            UserLocation(3.94, 116.83, "Asia/Makassar", "Tarakan")
        ).sortedBy { it.cityLabel }
    }
}
