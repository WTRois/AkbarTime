package com.atf.akbartime.ui

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
import androidx.appcompat.app.AppCompatActivity
import com.atf.akbartime.R
import com.atf.akbartime.data.PrayerName
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.databinding.ActivitySettingsBinding
import com.atf.akbartime.databinding.ItemSettingCorrectionBinding
import com.atf.akbartime.databinding.ItemSettingToggleBinding
import com.atf.akbartime.location.LocationRepository

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationRepository: LocationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)
        locationRepository = LocationRepository(this)

        setupToolbar()
        setupCitySpinner()
        setupAdzanToggles()
        setupManualCorrections()
        setupExtraFeatures()
        setupLocationDetection()
        setupBatteryOptimization()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCitySpinner() {
        val cities = locationRepository.getPresetCities()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities.map { it.cityLabel })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCities.adapter = adapter

        val currentLocation = settingsRepository.getLocation()
        currentLocation?.let { loc ->
            val index = cities.indexOfFirst { it.cityLabel == loc.cityLabel }
            if (index != -1) {
                binding.spinnerCities.setSelection(index)
            }
        }

        binding.spinnerCities.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCity = cities[position]
                settingsRepository.saveLocation(selectedCity)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAdzanToggles() {
        PrayerName.entries.forEach { prayer ->
            val toggleBinding = ItemSettingToggleBinding.inflate(LayoutInflater.from(this), binding.adzanToggleContainer, false)
            toggleBinding.tvSettingLabel.text = getPrayerDisplayName(prayer)
            toggleBinding.switchSetting.isChecked = settingsRepository.isAdzanEnabled(prayer)
            toggleBinding.switchSetting.setOnCheckedChangeListener { _, isChecked ->
                settingsRepository.setAdzanEnabled(prayer, isChecked)
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
            }
            
            correctionBinding.btnMinus.setOnClickListener {
                currentOffset--
                settingsRepository.setOffset(prayer, currentOffset)
                correctionBinding.tvCorrectionValue.text = if (currentOffset >= 0) "+$currentOffset" else currentOffset.toString()
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
        }

        // Silent Mode
        binding.layoutSilentMode.tvSettingLabel.text = getString(R.string.silent_mode_title)
        binding.layoutSilentMode.switchSetting.isChecked = settingsRepository.isSilentModeEnabled()
        binding.layoutSilentMode.switchSetting.setOnCheckedChangeListener { _, isChecked ->
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
