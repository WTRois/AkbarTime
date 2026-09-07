package com.atf.akbartime.data

import java.time.ZonedDateTime

data class PrayerTimes(
    val imsak: ZonedDateTime,
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val dhuha: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime
)

enum class PrayerName { IMSAK, FAJR, DHUHA, DHUHR, ASR, MAGHRIB, ISHA }
