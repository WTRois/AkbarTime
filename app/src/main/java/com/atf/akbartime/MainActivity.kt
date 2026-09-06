package com.atf.akbartime

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.PrayerTimes
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.data.UserLocation
import com.atf.akbartime.databinding.ActivityMainBinding
import com.atf.akbartime.prayer.PrayerTimeRepository
import com.atf.akbartime.ui.SettingsActivity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prayerRepository: PrayerTimeRepository
    private val handler = Handler(Looper.getMainLooper())
    private var nextPrayerTime: LocalDateTime? = null
    private var nextPrayerName: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Location permission granted
        }
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateCountdownUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)
        prayerRepository = PrayerTimeRepository(settingsRepository)
        checkPermissions()
        setupListeners()

        handler.post(countdownRunnable)
    }

    override fun onResume() {
        super.onResume()
        loadCurrentData()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCurrentData() {
        val location = settingsRepository.getLocation() ?: UserLocation(-6.2088, 106.8456, "Asia/Jakarta", "Jakarta")
        updatePrayerTimes(location)
        updateDateUI()
    }

    private fun updateDateUI() {
        val now = LocalDate.now()
        val hijriDate = HijrahDate.from(now)
        
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
        val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
        
        val dateString = now.format(dateFormatter)
        var hijriString = hijriDate.format(hijriFormatter)
        
        // Remove "Islamic Hijrah" prefix if present
        hijriString = hijriString.replace("Islamic Hijrah", "").trim()
        
        binding.tvDate.text = "$dateString • $hijriString H"
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Check Exact Alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun updatePrayerTimes(location: UserLocation) {
        val prayerTimes = prayerRepository.calculatePrayerTimes(location, Date())
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        binding.tvLocation.text = location.cityLabel

        val now = LocalDateTime.now()
        val next = findNextPrayer(prayerTimes, now)
        nextPrayerName = getPrayerDisplayName(next.first)
        nextPrayerTime = next.second

        binding.tvNextPrayerLabel.text = nextPrayerName
        binding.tvNextPrayerTime.text = nextPrayerTime?.format(timeFormatter)

        binding.itemImsak.apply {
            tvPrayerName.text = getString(R.string.prayer_imsak)
            tvPrayerTime.text = prayerTimes.imsak.format(timeFormatter)
        }
        binding.itemSubuh.apply {
            tvPrayerName.text = getString(R.string.prayer_subuh)
            tvPrayerTime.text = prayerTimes.fajr.format(timeFormatter)
        }
        binding.itemDhuha.apply {
            tvPrayerName.text = getString(R.string.prayer_dhuha)
            tvPrayerTime.text = prayerTimes.dhuha.format(timeFormatter)
        }
        binding.itemDzuhur.apply {
            tvPrayerName.text = getString(R.string.prayer_dzuhur)
            tvPrayerTime.text = prayerTimes.dhuhr.format(timeFormatter)
        }
        binding.itemAshar.apply {
            tvPrayerName.text = getString(R.string.prayer_ashar)
            tvPrayerTime.text = prayerTimes.asr.format(timeFormatter)
        }
        binding.itemMaghrib.apply {
            tvPrayerName.text = getString(R.string.prayer_maghrib)
            tvPrayerTime.text = prayerTimes.maghrib.format(timeFormatter)
        }
        binding.itemIsya.apply {
            tvPrayerName.text = getString(R.string.prayer_isya)
            tvPrayerTime.text = prayerTimes.isha.format(timeFormatter)
        }
    }

    private fun getPrayerDisplayName(name: PrayerName): String {
        return when (name) {
            PrayerName.IMSAK -> getString(R.string.prayer_imsak)
            PrayerName.FAJR -> getString(R.string.prayer_subuh)
            PrayerName.DHUHA -> getString(R.string.prayer_dhuha)
            PrayerName.DHUHR -> getString(R.string.prayer_dzuhur)
            PrayerName.ASR -> getString(R.string.prayer_ashar)
            PrayerName.MAGHRIB -> getString(R.string.prayer_maghrib)
            PrayerName.ISHA -> getString(R.string.prayer_isya)
        }
    }

    private fun findNextPrayer(prayerTimes: PrayerTimes, now: LocalDateTime): Pair<PrayerName, LocalDateTime> {
        val times = listOf(
            PrayerName.IMSAK to prayerTimes.imsak,
            PrayerName.FAJR to prayerTimes.fajr,
            PrayerName.DHUHA to prayerTimes.dhuha,
            PrayerName.DHUHR to prayerTimes.dhuhr,
            PrayerName.ASR to prayerTimes.asr,
            PrayerName.MAGHRIB to prayerTimes.maghrib,
            PrayerName.ISHA to prayerTimes.isha
        )

        for ((name, time) in times) {
            if (time.isAfter(now)) return name to time
        }
        return PrayerName.IMSAK to prayerTimes.imsak.plusDays(1)
    }

    private fun updateCountdownUI() {
        val target = nextPrayerTime ?: return
        val now = LocalDateTime.now()
        val diff = Duration.between(now, target)

        if (diff.isNegative || diff.isZero) {
            // Should refresh for the next one
            // In a real app, re-fetch prayer times
            binding.tvCountdown.text = "00:00:00"
            return
        }

        val hours = diff.toHours()
        val minutes = diff.toMinutes() % 60
        val seconds = diff.seconds % 60
        binding.tvCountdown.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
