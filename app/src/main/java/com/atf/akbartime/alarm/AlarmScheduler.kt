package com.atf.akbartime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.PrayerTimes
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.prayer.PrayerTimeRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settings = SettingsRepository(context)

    fun scheduleUpcomingAlarms() {
        val location = settings.getLocation()
        val prayerRepo = PrayerTimeRepository(settings)
        val now = ZonedDateTime.now(ZoneId.of(location.timezoneId))
        val todayTimes = prayerRepo.calculatePrayerTimes(location, Date())
        val tomorrowDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
        val tomorrowTimes = prayerRepo.calculatePrayerTimes(location, tomorrowDate)

        Log.d("AlarmScheduler", "Scheduling upcoming alarms for ${location.cityLabel}")
        schedulePrayerWithRollover(PrayerName.IMSAK, todayTimes.imsak, tomorrowTimes.imsak, now)
        schedulePrayerWithRollover(PrayerName.FAJR, todayTimes.fajr, tomorrowTimes.fajr, now)
        schedulePrayerWithRollover(PrayerName.DHUHA, todayTimes.dhuha, tomorrowTimes.dhuha, now)
        schedulePrayerWithRollover(PrayerName.DHUHR, todayTimes.dhuhr, tomorrowTimes.dhuhr, now)
        schedulePrayerWithRollover(PrayerName.ASR, todayTimes.asr, tomorrowTimes.asr, now)
        schedulePrayerWithRollover(PrayerName.MAGHRIB, todayTimes.maghrib, tomorrowTimes.maghrib, now)
        schedulePrayerWithRollover(PrayerName.ISHA, todayTimes.isha, tomorrowTimes.isha, now)
    }

    fun schedulePrayerAlarms(prayerTimes: PrayerTimes) {
        // Kept for backward compatibility, delegates to 24-hour rolling schedule
        scheduleUpcomingAlarms()
    }

    private fun schedulePrayerWithRollover(
        name: PrayerName,
        todayTime: ZonedDateTime,
        tomorrowTime: ZonedDateTime,
        now: ZonedDateTime
    ) {
        val targetTime = if (todayTime.isAfter(now)) todayTime else tomorrowTime

        // Main Alarm
        scheduleAlarm(name, targetTime, false)

        // Pre-Reminder (10 minutes before)
        if (settings.isPreReminderEnabled()) {
            val preTime = targetTime.minusMinutes(10)
            if (preTime.isAfter(now)) {
                scheduleAlarm(name, preTime, true)
            }
        }
    }

    private fun scheduleAlarm(name: PrayerName, time: ZonedDateTime, isPreReminder: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", name.name)
            putExtra("IS_PRE_REMINDER", isPreReminder)
        }

        val requestCode = if (isPreReminder) name.ordinal + 100 else name.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val millis = time.toInstant().toEpochMilli()

        if (System.currentTimeMillis() < millis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            }
        }
    }
}
