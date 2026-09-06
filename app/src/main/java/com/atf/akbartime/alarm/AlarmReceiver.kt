package com.atf.akbartime.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.atf.akbartime.R
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.SettingsRepository

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "prayer_reminder_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)
        
        Log.d("AlarmReceiver", "Alarm triggered for: $prayerName (Pre-reminder: $isPreReminder)")

        if (isPreReminder) {
            showPreReminderNotification(context, prayerName)
        } else {
            showNotification(context, prayerName)
            if (isMainPrayer(prayerName)) {
                playAdzan(context)
                handleSilentMode(context)
            }
        }
    }

    private fun handleSilentMode(context: Context) {
        val settings = SettingsRepository(context)
        if (settings.isSilentModeEnabled()) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalMode = audioManager.ringerMode
            
            // Set to silent/vibrate
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            
            // Restore after 15 minutes
            Handler(Looper.getMainLooper()).postDelayed({
                audioManager.ringerMode = originalMode
            }, 15 * 60 * 1000)
        }
    }

    private fun showPreReminderNotification(context: Context, prayerName: String) {
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

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

    private fun playAdzan(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.adzan)
            mediaPlayer?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error playing adzan", e)
        }
    }
}
