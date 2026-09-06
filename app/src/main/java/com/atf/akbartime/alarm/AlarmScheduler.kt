package com.atf.akbartime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.PrayerTimes
import com.atf.akbartime.data.SettingsRepository
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settings = SettingsRepository(context)

    fun schedulePrayerAlarms(prayerTimes: PrayerTimes) {
        schedulePrayer(PrayerName.IMSAK, prayerTimes.imsak)
        schedulePrayer(PrayerName.FAJR, prayerTimes.fajr)
        schedulePrayer(PrayerName.DHUHA, prayerTimes.dhuha)
        schedulePrayer(PrayerName.DHUHR, prayerTimes.dhuhr)
        schedulePrayer(PrayerName.ASR, prayerTimes.asr)
        schedulePrayer(PrayerName.MAGHRIB, prayerTimes.maghrib)
        schedulePrayer(PrayerName.ISHA, prayerTimes.isha)
    }

    private fun schedulePrayer(name: PrayerName, time: LocalDateTime) {
        // Main Alarm
        scheduleAlarm(name, time, false)

        // Pre-Reminder (10 minutes before)
        if (settings.isPreReminderEnabled()) {
            scheduleAlarm(name, time.minusMinutes(10), true)
        }
    }

    private fun scheduleAlarm(name: PrayerName, time: LocalDateTime, isPreReminder: Boolean) {
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

        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (System.currentTimeMillis() < millis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            }
        }
    }
}
