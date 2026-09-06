package com.atf.akbartime.prayer

import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.PrayerTimes
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.data.UserLocation
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class PrayerTimeRepository(private val settingsRepository: SettingsRepository) {
    fun calculatePrayerTimes(location: UserLocation, date: Date): PrayerTimes {
        val coordinates = Coordinates(location.latitude, location.longitude)
        val dateComponents = DateComponents.from(date)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters

        val adhanTimes = AdhanPrayerTimes(coordinates, dateComponents, params)
        val zoneId = ZoneId.of(location.timezoneId)

        val fajr = toLocalDateTime(adhanTimes.fajr, zoneId).plusMinutes(settingsRepository.getOffset(PrayerName.FAJR).toLong())
        val sunrise = toLocalDateTime(adhanTimes.sunrise, zoneId)
        val dhuhr = toLocalDateTime(adhanTimes.dhuhr, zoneId).plusMinutes(settingsRepository.getOffset(PrayerName.DHUHR).toLong())
        val asr = toLocalDateTime(adhanTimes.asr, zoneId).plusMinutes(settingsRepository.getOffset(PrayerName.ASR).toLong())
        val maghrib = toLocalDateTime(adhanTimes.maghrib, zoneId).plusMinutes(settingsRepository.getOffset(PrayerName.MAGHRIB).toLong())
        val isha = toLocalDateTime(adhanTimes.isha, zoneId).plusMinutes(settingsRepository.getOffset(PrayerName.ISHA).toLong())

        val baseImsak = fajr.minusMinutes(10)
        val imsakWithOffset = baseImsak.plusMinutes(settingsRepository.getOffset(PrayerName.IMSAK).toLong())
        
        val baseDhuha = sunrise.plusMinutes(15)
        val dhuhaWithOffset = baseDhuha.plusMinutes(settingsRepository.getOffset(PrayerName.DHUHA).toLong())

        return PrayerTimes(
            imsak = imsakWithOffset,
            fajr = fajr,
            sunrise = sunrise,
            dhuha = dhuhaWithOffset,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha
        )
    }

    private fun toLocalDateTime(date: Date, zoneId: ZoneId): LocalDateTime {
        return LocalDateTime.ofInstant(date.toInstant(), zoneId)
    }
}
