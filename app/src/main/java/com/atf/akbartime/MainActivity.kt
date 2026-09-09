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
import com.atf.akbartime.alarm.AlarmScheduler
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.PrayerTimes
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.data.UserLocation
import com.atf.akbartime.databinding.ActivityMainBinding
import com.atf.akbartime.prayer.PrayerTimeRepository
import com.atf.akbartime.ui.QiblaActivity
import com.atf.akbartime.ui.SettingsActivity
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prayerRepository: PrayerTimeRepository
    private val handler = Handler(Looper.getMainLooper())
    private var nextPrayerTime: ZonedDateTime? = null
    private var nextPrayerName: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

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
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        loadCurrentData()
        AlarmScheduler(this).scheduleUpcomingAlarms()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun setupListeners() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_kiblat -> {
                    val intent = Intent(this, QiblaActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCurrentData() {
        val location = settingsRepository.getLocation()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun updatePrayerTimes(location: UserLocation) {
        val prayerTimes = prayerRepository.calculatePrayerTimes(location, Date())
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val tzAbbr = getTimezoneAbbreviation(location.timezoneId)
        binding.tvLocation.text = if (tzAbbr.isNotEmpty()) "${location.cityLabel} ($tzAbbr)" else location.cityLabel

        val now = ZonedDateTime.now(ZoneId.of(location.timezoneId))
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

    private fun getTimezoneAbbreviation(timezoneId: String): String {
        return when (timezoneId) {
            "Asia/Jakarta", "Asia/Pontianak" -> "WIB"
            "Asia/Makassar", "Asia/Banjarmasin", "Asia/Samarinda", "Asia/Balikpapan", "Asia/Denpasar", "Asia/Mataram", "Asia/Kupang" -> "WITA"
            "Asia/Jayapura", "Asia/Ambon", "Asia/Manokwari" -> "WIT"
            else -> ""
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

    private fun findNextPrayer(prayerTimes: PrayerTimes, now: ZonedDateTime): Pair<PrayerName, ZonedDateTime> {
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
        val now = ZonedDateTime.now(target.zone)
        val diff = Duration.between(now, target)

        if (diff.isNegative || diff.isZero) {
            binding.tvCountdown.text = "-00:00:00"
            loadCurrentData() 
            return
        }

        val hours = diff.toHours()
        val minutes = diff.toMinutes() % 60
        val seconds = diff.seconds % 60
        binding.tvCountdown.text = String.format(Locale.getDefault(), "-%02d:%02d:%02d", hours, minutes, seconds)
    }
}
