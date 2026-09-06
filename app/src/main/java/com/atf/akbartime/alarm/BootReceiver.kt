package com.atf.akbartime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.prayer.PrayerTimeRepository
import java.util.Date

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = SettingsRepository(context)
            val location = settings.getLocation() ?: return

            val prayerRepo = PrayerTimeRepository(settings)
            val prayerTimes = prayerRepo.calculatePrayerTimes(location, Date())

            val scheduler = AlarmScheduler(context)
            scheduler.schedulePrayerAlarms(prayerTimes)
        }
    }
}
