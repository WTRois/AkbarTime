package com.atf.akbartime.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.atf.akbartime.R

class AdzanService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var afd: AssetFileDescriptor? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra("PRAYER_NAME") ?: "Adzan"
        showForegroundNotification(prayerName)

        Log.d("AdzanService", "Starting Adzan playback for $prayerName")
        playAdzan()
        return START_NOT_STICKY
    }

    private fun showForegroundNotification(prayerName: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setSound(null, null) // Silent channel so MediaPlayer handles audio playback exclusively
            }
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AdzanService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktu $prayerName")
            .setContentText("Sedang mengumandangkan Adzan...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .setDeleteIntent(stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Berhentikan Adzan", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun requestAudioFocus(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopSelf()
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopSelf()
                    }
                },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun playAdzan() {
        try {
            stopAdzan()
            requestAudioFocus()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val localAfd = resources.openRawResourceFd(R.raw.adzan)
            afd = localAfd

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(localAfd.fileDescriptor, localAfd.startOffset, localAfd.length)
                setOnPreparedListener {
                    Log.d("AdzanService", "MediaPlayer prepared, starting playback")
                    it.start()
                }
                setOnCompletionListener {
                    Log.d("AdzanService", "MediaPlayer completed, stopping service")
                    stopSelf()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AdzanService", "MediaPlayer error: what=$what, extra=$extra")
                    stopSelf()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AdzanService", "Error playing adzan", e)
            stopSelf()
        }
    }

    private fun stopAdzan() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        try {
            afd?.close()
        } catch (e: Exception) {
            Log.w("AdzanService", "Error closing AssetFileDescriptor", e)
        }
        afd = null

        abandonAudioFocus()
    }

    override fun onDestroy() {
        Log.d("AdzanService", "Service destroyed, stopping Adzan")
        stopAdzan()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.atf.akbartime.alarm.ACTION_STOP_ADZAN"
        private const val NOTIFICATION_ID = 1001
    }
}
