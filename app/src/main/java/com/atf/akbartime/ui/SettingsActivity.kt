package com.atf.akbartime.ui

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.atf.akbartime.MainActivity
import com.atf.akbartime.R
import com.atf.akbartime.alarm.AlarmReceiver
import com.atf.akbartime.alarm.AlarmScheduler
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.databinding.ActivitySettingsBinding
import com.atf.akbartime.databinding.ItemSettingCorrectionBinding
import com.atf.akbartime.databinding.ItemSettingToggleBinding
import com.atf.akbartime.location.LocationRepository
import com.atf.akbartime.prayer.PrayerTimeRepository
import java.util.Date

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var prayerRepository: PrayerTimeRepository
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)
        locationRepository = LocationRepository(this)
        prayerRepository = PrayerTimeRepository(settingsRepository)
        alarmScheduler = AlarmScheduler(this)

        setupToolbar()
        setupBottomNav()
        setupCitySpinner()
        setupAdzanToggles()
        setupManualCorrections()
        setupExtraFeatures()
        setupTestAlarm()
        setupLocationDetection()
        setupBatteryOptimization()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_kiblat -> {
                    val intent = Intent(this, QiblaActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun setupTestAlarm() {
        binding.btnTestAlarm.setOnClickListener {
            Toast.makeText(this, "Menjalankan Test Adzan...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", PrayerName.MAGHRIB.name)
                putExtra("IS_PRE_REMINDER", false)
            }
            sendBroadcast(intent)
        }
    }

    private fun rescheduleAlarms() {
        alarmScheduler.scheduleUpcomingAlarms()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCitySpinner() {
        val cities = locationRepository.getPresetCities()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities.map { it.cityLabel })
        binding.autoCompleteCities.setAdapter(adapter)

        val currentLocation = settingsRepository.getLocation()
        currentLocation?.let { loc ->
            binding.autoCompleteCities.setText(loc.cityLabel, false)
        }

        binding.autoCompleteCities.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selectedCityLabel = parent.getItemAtPosition(position) as String
            val selectedCity = cities.find { it.cityLabel == selectedCityLabel }
            selectedCity?.let {
                settingsRepository.saveLocation(it)
                rescheduleAlarms()
            }
        }
    }

    private fun setupAdzanToggles() {
        PrayerName.entries.forEach { prayer ->
            val toggleBinding = ItemSettingToggleBinding.inflate(LayoutInflater.from(this), binding.adzanToggleContainer, false)
            toggleBinding.tvSettingLabel.text = getPrayerDisplayName(prayer)
            toggleBinding.switchSetting.isChecked = settingsRepository.isAdzanEnabled(prayer)
            toggleBinding.switchSetting.setOnCheckedChangeListener { _, isChecked ->
                settingsRepository.setAdzanEnabled(prayer, isChecked)
                // No need to reschedule for just audio toggle, Receiver checks it
            }
            binding.adzanToggleContainer.addView(toggleBinding.root)
        }
    }

    private fun setupManualCorrections() {
        PrayerName.entries.forEach { prayer ->
            val correctionBinding = ItemSettingCorrectionBinding.inflate(LayoutInflater.from(this), binding.correctionContainer, false)
            correctionBinding.tvCorrectionLabel.text = getPrayerDisplayName(prayer)
            
            var currentOffset = settingsRepository.getOffset(prayer)
            correctionBinding.tvCorrectionValue.text = if (currentOffset >= 0) "+$currentOffset" else currentOffset.toString()
            
            correctionBinding.btnPlus.setOnClickListener {
                currentOffset++
                settingsRepository.setOffset(prayer, currentOffset)
                correctionBinding.tvCorrectionValue.text = if (currentOffset >= 0) "+$currentOffset" else currentOffset.toString()
                rescheduleAlarms()
            }
            
            correctionBinding.btnMinus.setOnClickListener {
                currentOffset--
                settingsRepository.setOffset(prayer, currentOffset)
                correctionBinding.tvCorrectionValue.text = if (currentOffset >= 0) "+$currentOffset" else currentOffset.toString()
                rescheduleAlarms()
            }
            
            binding.correctionContainer.addView(correctionBinding.root)
        }
    }

    private fun setupExtraFeatures() {
        // Pre-Reminder
        binding.layoutPreReminder.tvSettingLabel.text = getString(R.string.pre_reminder_title)
        binding.layoutPreReminder.switchSetting.isChecked = settingsRepository.isPreReminderEnabled()
        binding.layoutPreReminder.switchSetting.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setPreReminderEnabled(isChecked)
            rescheduleAlarms()
        }

        // Silent Mode
        binding.layoutSilentMode.tvSettingLabel.text = getString(R.string.silent_mode_title)
        binding.layoutSilentMode.switchSetting.isChecked = settingsRepository.isSilentModeEnabled()
        binding.layoutSilentMode.switchSetting.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val nm = getSystemService(NotificationManager::class.java)
                if (!nm.isNotificationPolicyAccessGranted) {
                    // Reset toggle and ask for permission
                    binding.layoutSilentMode.switchSetting.isChecked = false
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(this, "Izin 'Jangan Ganggu' diperlukan untuk fitur ini", Toast.LENGTH_LONG).show()
                    return@setOnCheckedChangeListener
                }
            }
            settingsRepository.setSilentModeEnabled(isChecked)
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

    private fun setupLocationDetection() {
        binding.btnDetectLocation.setOnClickListener {
            val detected = locationRepository.getLastKnownLocation()
            detected?.let {
                settingsRepository.saveLocation(it)
                finish() 
            }
        }
    }

    private fun setupBatteryOptimization() {
        binding.btnBatteryOptimize.setOnClickListener {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                } else {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                }
            } else {
                intent.action = Settings.ACTION_SETTINGS
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}
