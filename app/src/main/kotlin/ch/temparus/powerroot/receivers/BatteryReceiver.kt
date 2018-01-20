package ch.temparus.powerroot.receivers

import android.content.*
import android.os.BatteryManager
import android.preference.PreferenceManager
import android.util.Log
import ch.temparus.powerroot.SharedMethods
import ch.temparus.powerroot.fragments.ConfigurationFragment
import ch.temparus.powerroot.services.BatteryService


class BatteryReceiver(private val service: BatteryService) : BroadcastReceiver() {

    private var preferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val configuration: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(service.baseContext)

    private var limitPercentage: Int = 0
    private var rechargePercentage: Int = 0
    private var enabled: Boolean = false

    init {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            Log.d("BatteryReceiver", "SharedPreference '$key' has changed!")
            when (key) {
                ConfigurationFragment.BATTERY_RECHARGE_THRESHOLD -> {
                    service.setControlState(BatteryService.CONTROL_STATE_UNKNOWN)
                    reset(sharedPreferences)
                }
                ConfigurationFragment.BATTERY_CHARGE_LIMIT_ENABLED,
                ConfigurationFragment.BATTERY_CHARGE_LIMIT -> {
                    reset(sharedPreferences)
                }
            }
        }
        configuration.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        reset(configuration)
    }

    private fun reset(configuration: SharedPreferences) {
        limitPercentage = Integer.parseInt(configuration.getString(ConfigurationFragment.BATTERY_CHARGE_LIMIT, "80"))
        rechargePercentage = Integer.parseInt(configuration.getString(ConfigurationFragment.BATTERY_RECHARGE_THRESHOLD, (limitPercentage - 5).toString()))
        enabled = configuration.getBoolean(ConfigurationFragment.BATTERY_CHARGE_LIMIT_ENABLED, false)
        // manually fire onReceive() to update state if service is enabled
        onReceive(service, service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!enabled) {
            service.setControlState(BatteryService.CONTROL_STATE_DISABLED)
            return
        }

        val batteryLevel = BatteryService.getBatteryLevel(intent)
        val currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        when (service.getControlState()) {
            BatteryService.CONTROL_STATE_STOP -> {
                if (batteryLevel <= rechargePercentage) {
                    service.setControlState(BatteryService.CONTROL_STATE_CHARGING)
                }
            }
            BatteryService.CONTROL_STATE_CHARGING -> {
                if (batteryLevel >= limitPercentage) {
                    service.setControlState(BatteryService.CONTROL_STATE_STOP)
                }
            }
            BatteryService.CONTROL_STATE_BOOST -> {
                if (currentStatus != BatteryManager.BATTERY_STATUS_CHARGING) {
                    service.setControlState(BatteryService.CONTROL_STATE_STOP)
                }
            }
            else -> {
                // when the service was started or has been enabled, charge until limit
                if (batteryLevel > rechargePercentage) {
                    service.setControlState(BatteryService.CONTROL_STATE_STOP)
                } else {
                    service.setControlState(BatteryService.CONTROL_STATE_CHARGING)
                }
            }
        }
    }

    fun detach() {
        // unregister the listener that listens for relevant change events
        configuration.unregisterOnSharedPreferenceChangeListener(this.preferenceChangeListener)
        this.preferenceChangeListener = null
    }
}
