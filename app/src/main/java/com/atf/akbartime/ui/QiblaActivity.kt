package com.atf.akbartime.ui

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.atf.akbartime.MainActivity
import com.atf.akbartime.R
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.databinding.ActivityQiblaBinding
import java.util.Locale

class QiblaActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityQiblaBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sensorManager: SensorManager
    
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rMat = FloatArray(9)
    private val iMat = FloatArray(9)
    private val orientation = FloatArray(3)
    private var qiblaDegree = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQiblaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        setupUI()
        calculateQibla()
    }

    private fun setupUI() {
        binding.bottomNavigation.selectedItemId = R.id.nav_kiblat
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
                R.id.nav_kiblat -> true
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

        val location = settingsRepository.getLocation()
        binding.tvLocationName.text = location.cityLabel
    }

    private fun calculateQibla() {
        val location = settingsRepository.getLocation()
        
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLng = Math.toRadians(39.8262)
        val myLat = Math.toRadians(location.latitude)
        val myLng = Math.toRadians(location.longitude)
        
        val deltaLng = kaabaLng - myLng
        
        val y = Math.sin(deltaLng) * Math.cos(kaabaLat)
        val x = Math.cos(myLat) * Math.sin(kaabaLat) - Math.sin(myLat) * Math.cos(kaabaLat) * Math.cos(deltaLng)
        
        qiblaDegree = ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()
        Log.d("QiblaActivity", "Qibla Angle for ${location.cityLabel}: $qiblaDegree")
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_kiblat
        calculateQibla()
        val location = settingsRepository.getLocation()
        binding.tvLocationName.text = location.cityLabel

        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float = 0.25f) {
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            lowPass(event.values, gravity)
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            lowPass(event.values, geomagnetic)
        }

        if (SensorManager.getRotationMatrix(rMat, iMat, gravity, geomagnetic)) {
            SensorManager.getOrientation(rMat, orientation)
            
            // azimuth is the rotation around the Z axis
            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val azimuthFixed = (azimuth + 360) % 360
            
            // Direction to Kaaba relative to North is qiblaDegree
            // Azimuth is current heading relative to North
            // So arrow rotation should be (qiblaDegree - azimuthFixed)
            val rotation = (qiblaDegree - azimuthFixed + 360) % 360
            
            updateCompassUI(rotation, azimuthFixed)
        }
    }

    private fun updateCompassUI(rotation: Float, heading: Float) {
        binding.ivQiblaNeedle.rotation = rotation
        binding.tvDegree.text = String.format(Locale.getDefault(), "%.0f°", heading)
        
        // Status message
        val diff = Math.abs(rotation)
        if (diff < 5 || diff > 355) {
            binding.tvStatus.text = "Arah Kiblat Tepat!"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neob_primary))
        } else {
            binding.tvStatus.text = "Putar HP untuk mencari kiblat"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neob_text))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
