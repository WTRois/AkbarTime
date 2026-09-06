package com.atf.akbartime.data

import java.time.LocalDateTime

data class PrayerTimes(
    val imsak: LocalDateTime,
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuha: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime
)

enum class PrayerName { IMSAK, FAJR, DHUHA, DHUHR, ASR, MAGHRIB, ISHA }
