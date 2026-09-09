package com.atf.akbartime.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.atf.akbartime.R
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.SettingsRepository

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "prayer_reminder_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_RESTORE_RINGER_MODE = "com.atf.akbartime.alarm.ACTION_RESTORE_RINGER_MODE"
        const val EXTRA_ORIGINAL_RINGER_MODE = "EXTRA_ORIGINAL_RINGER_MODE"
        private const val RINGER_RESTORE_REQUEST_CODE = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RESTORE_RINGER_MODE) {
            val originalMode = intent.getIntExtra(EXTRA_ORIGINAL_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                audioManager.ringerMode = originalMode
                Log.d("AlarmReceiver", "Restored ringer mode to: $originalMode")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error restoring ringer mode", e)
            }
            return
        }

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)
        
        Log.d("AlarmReceiver", "Alarm triggered for: $prayerName (Pre-reminder: $isPreReminder)")

        if (isPreReminder) {
            showPreReminderNotification(context, prayerName)
        } else {
            val settings = SettingsRepository(context)
            val isAdzanEnabled = try {
                settings.isAdzanEnabled(PrayerName.valueOf(prayerName))
            } catch (e: Exception) {
                false
            }

            if (isAdzanEnabled && isMainPrayer(prayerName)) {
                Log.d("AlarmReceiver", "Starting AdzanService for $prayerName")
                val serviceIntent = Intent(context, AdzanService::class.java).apply {
                    putExtra("PRAYER_NAME", getPrayerDisplayName(context, prayerName))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                handleSilentMode(context)
            } else {
                showNotification(context, prayerName)
            }

            // Immediately schedule upcoming 24h rolling cycle
            AlarmScheduler(context).scheduleUpcomingAlarms()
        }
    }

    private fun handleSilentMode(context: Context) {
        val settings = SettingsRepository(context)
        if (settings.isSilentModeEnabled()) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if we have permission to change Do Not Disturb state (required for Android 6.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.isNotificationPolicyAccessGranted) {
                Log.w("AlarmReceiver", "Cannot change silent mode: Notification Policy Access not granted")
                return
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                val originalMode = audioManager.ringerMode
                
                // Set to vibrate
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                
                // Restore after 15 minutes reliably via AlarmManager
                val restoreIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_RESTORE_RINGER_MODE
                    putExtra(EXTRA_ORIGINAL_RINGER_MODE, originalMode)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    RINGER_RESTORE_REQUEST_CODE,
                    restoreIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerAtMillis = System.currentTimeMillis() + (15 * 60 * 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                Log.e("AlarmReceiver", "SecurityException when changing ringer mode", e)
            }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showPreReminderNotification(context: Context, prayerName: String) {
        ensureNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prayerDisplayName = getPrayerDisplayName(context, prayerName)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Persiapan $prayerDisplayName")
            .setContentText("10 menit lagi waktu $prayerDisplayName tiba.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showNotification(context: Context, prayerName: String) {
        ensureNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prayerDisplayName = getPrayerDisplayName(context, prayerName)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktu $prayerDisplayName")
            .setContentText(context.getString(R.string.notification_content_text, prayerDisplayName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getPrayerDisplayName(context: Context, name: String): String {
        return when (name) {
            PrayerName.IMSAK.name -> context.getString(R.string.prayer_imsak)
            PrayerName.FAJR.name -> context.getString(R.string.prayer_subuh)
            PrayerName.DHUHA.name -> context.getString(R.string.prayer_dhuha)
            PrayerName.DHUHR.name -> context.getString(R.string.prayer_dzuhur)
            PrayerName.ASR.name -> context.getString(R.string.prayer_ashar)
            PrayerName.MAGHRIB.name -> context.getString(R.string.prayer_maghrib)
            PrayerName.ISHA.name -> context.getString(R.string.prayer_isya)
            else -> name
        }
    }

    private fun isMainPrayer(name: String): Boolean {
        return name != PrayerName.IMSAK.name && name != PrayerName.DHUHA.name
    }
}
