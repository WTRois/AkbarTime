package com.atf.akbartime.ui

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.atf.akbartime.MainActivity
import com.atf.akbartime.R
import com.atf.akbartime.data.SettingsRepository
import com.atf.akbartime.data.UserLocation
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
    private var currentDegree = 0f
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
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        val location = settingsRepository.getLocation()
        binding.tvLocationName.text = location?.cityLabel ?: "Jakarta"
    }

    private fun calculateQibla() {
        val location = settingsRepository.getLocation() ?: UserLocation(-6.2088, 106.8456, "Asia/Jakarta", "Jakarta")
        
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
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.size)
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
        }

        val rMat = FloatArray(9)
        val iMat = FloatArray(9)
        if (SensorManager.getRotationMatrix(rMat, iMat, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rMat, orientation)
            
            // azimuth is the rotation around the Z axis
            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val azimuthFixed = (azimuth + 360) % 360
            
            // Direction to Kaaba relative to North is qiblaDegree
            // Azimuth is current heading relative to North
            // So arrow rotation should be (qiblaDegree - azimuthFixed)
            val rotation = qiblaDegree - azimuthFixed
            
            updateCompassUI(rotation, azimuthFixed)
        }
    }

    private fun updateCompassUI(rotation: Float, heading: Float) {
        val ra = RotateAnimation(
            currentDegree,
            rotation,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        ra.duration = 210
        ra.fillAfter = true
        
        binding.ivQiblaNeedle.startAnimation(ra)
        currentDegree = rotation
        
        binding.tvDegree.text = String.format(Locale.getDefault(), "%.0f°", heading)
        
        // Status message
        val diff = Math.abs(rotation % 360)
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
