package com.atf.akbartime.data

import android.content.Context
import android.content.SharedPreferences
import java.lang.Double

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_LOCATION = UserLocation(-6.2088, 106.8456, "Asia/Jakarta", "Jakarta")
    }

    fun saveLocation(location: UserLocation) {
        prefs.edit().apply {
            putLong("lat", Double.doubleToRawLongBits(location.latitude))
            putLong("lng", Double.doubleToRawLongBits(location.longitude))
            putString("timezone", location.timezoneId)
            putString("city", location.cityLabel)
            apply()
        }
    }

    fun getLocation(): UserLocation {
        if (!prefs.contains("lat")) return DEFAULT_LOCATION
        return UserLocation(
            latitude = Double.longBitsToDouble(prefs.getLong("lat", 0)),
            longitude = Double.longBitsToDouble(prefs.getLong("lng", 0)),
            timezoneId = prefs.getString("timezone", DEFAULT_LOCATION.timezoneId) ?: DEFAULT_LOCATION.timezoneId,
            cityLabel = prefs.getString("city", DEFAULT_LOCATION.cityLabel) ?: DEFAULT_LOCATION.cityLabel
        )
    }

    fun setAdzanEnabled(prayerName: PrayerName, enabled: Boolean) {
        prefs.edit().putBoolean("adzan_${prayerName.name}", enabled).apply()
    }

    fun isAdzanEnabled(prayerName: PrayerName): Boolean {
        return prefs.getBoolean("adzan_${prayerName.name}", true)
    }

    fun setOffset(prayerName: PrayerName, offset: Int) {
        prefs.edit().putInt("offset_${prayerName.name}", offset).apply()
    }

    fun getOffset(prayerName: PrayerName): Int {
        return prefs.getInt("offset_${prayerName.name}", 0)
    }

    fun isPreReminderEnabled(): Boolean {
        return prefs.getBoolean("pre_reminder_enabled", false)
    }

    fun setPreReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pre_reminder_enabled", enabled).apply()
    }

    fun isSilentModeEnabled(): Boolean {
        return prefs.getBoolean("silent_mode_enabled", false)
    }

    fun setSilentModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("silent_mode_enabled", enabled).apply()
    }
}
