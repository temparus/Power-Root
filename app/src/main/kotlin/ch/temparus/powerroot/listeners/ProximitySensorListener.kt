package ch.temparus.powerroot.listeners

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.preference.PreferenceManager
import android.util.Log
import ch.temparus.powerroot.services.ProximityService
import ch.temparus.powerroot.services.ScreenHandler

class ProximitySensorListener(private val service: ProximityService) : SensorEventListener {

    private var preferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val configuration: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(service.baseContext)

    private var lockScreenCoverTime: Int = 1000
    private var lockScreenEnabled: Boolean = false
    private var lockScreenLandscape: Boolean = false
    private var waveModeEnabled: Boolean = false
    private var pocketModeEnabled: Boolean = false

    private var lastDistance = Distance.FAR
    private var lastTime: Long = 0
    private var waveCount = 0
    private var lastWaveTime: Long = 0

    init {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                ProximityService.PREF_ENABLED -> {
                    if (!sharedPreferences.getBoolean(key, false)) {
                        ProximityService.stop(service)
                    } else {
                        reset(sharedPreferences)
                    }
                }
                ProximityService.PREF_LOCK_SCREEN,
                ProximityService.PREF_LOCK_SCREEN_COVER_TIME,
                ProximityService.PREF_LOCK_SCREEN_LANDSCAPE,
                ProximityService.PREF_WAVE_MODE,
                ProximityService.PREF_POCKET_MODE -> {
                    reset(sharedPreferences)
                }
            }
        }
        configuration.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun reset(configuration: SharedPreferences) {
        lockScreenCoverTime = Integer.parseInt(configuration.getString(ProximityService.PREF_LOCK_SCREEN_COVER_TIME, "1000"))
        lockScreenEnabled = configuration.getBoolean(ProximityService.PREF_LOCK_SCREEN, false)
        lockScreenLandscape = configuration.getBoolean(ProximityService.PREF_LOCK_SCREEN_LANDSCAPE, false)
        waveModeEnabled = configuration.getBoolean(ProximityService.PREF_WAVE_MODE, false)
        pocketModeEnabled = configuration.getBoolean(ProximityService.PREF_POCKET_MODE, false)
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {
        // No implementation needed
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        val currentDistance = if (event.values[0] >= event.sensor.maximumRange) Distance.FAR else Distance.NEAR

        val uncovered = lastDistance === Distance.NEAR && currentDistance === Distance.FAR
        val covered = lastDistance === Distance.FAR && currentDistance === Distance.NEAR

        val timeBetweenFarAndNear = currentTime - lastTime

        val waved = timeBetweenFarAndNear <= MAX_WAVE_PERIOD
        val tookOutOfPocket = timeBetweenFarAndNear > MIN_POCKET_PERIOD

        if (uncovered) {
            val timeSinceLastScreenChange = currentTime - service.getLastTimeScreenChange()
            Log.d("ProximitySensorListener", "sensor uncovered!")
            Log.d("ProximitySensorListener", "timeBetweenFarAndNear: $timeBetweenFarAndNear")
            Log.d("ProximitySensorListener", "timeSinceLastScreenChange: $timeSinceLastScreenChange")
            if (timeSinceLastScreenChange > WAITING_PERIOD_BETWEEN_SCREEN_CHANGE) { // Don't do anything if it turned on or off 1.5 seconds ago
                if (waved && waveModeEnabled) {
                    if (currentTime - lastWaveTime > MAX_WAVE_PERIOD) {
                        // the last wave was a long time ago -> reset wave count
                        waveCount = 0
                    }

                    waveCount++
                    lastWaveTime = System.currentTimeMillis()
                    // TODO: Add option to support multiple wave gestures
                    if (waveCount > 0) {
                        Log.d("ProximitySensorListener", "wave detected!")
                        service.turnOnScreen()
                        waveCount = 0
                    }
                } else if (tookOutOfPocket && pocketModeEnabled) {
                    Log.d("ProximitySensorListener", "Took out of pocket!")
                    service.turnOnScreen()
                }
            }
        } else if (covered) {
            Log.d("ProximitySensorListener", "sensor covered!")
            service.turnOffScreen()
        }

        lastDistance = currentDistance
        lastTime = currentTime
    }

    companion object {
        private enum class Distance {
            NEAR, FAR
        }

        // Static configuration
        private const val MAX_WAVE_PERIOD: Long = 2000
        private const val MIN_POCKET_PERIOD: Long = 5000
        private const val WAITING_PERIOD_BETWEEN_SCREEN_CHANGE: Long = 1500
    }
}