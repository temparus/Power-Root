package ch.temparus.powerroot.receivers

import android.content.*
import android.os.BatteryManager
import android.preference.PreferenceManager
import ch.temparus.powerroot.services.BatteryService

class BatteryReceiver(private val service: BatteryService) : BroadcastReceiver() {

    private var preferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val configuration: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(service.baseContext)

    private var limitPercentage: Int = 0
    private var rechargePercentage: Int = 0
    private var enabled: Boolean = false

    init {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                BatteryService.BATTERY_RECHARGE_THRESHOLD -> {
                    service.setControlState(BatteryService.CONTROL_STATE_UNKNOWN)
                    reset(sharedPreferences)
                }
                BatteryService.BATTERY_CHARGE_LIMIT_ENABLED,
                BatteryService.BATTERY_CHARGE_LIMIT -> {
                    reset(sharedPreferences)
                }
            }
        }
        configuration.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        reset(configuration)
    }

    private fun reset(configuration: SharedPreferences) {
        limitPercentage = Integer.parseInt(configuration.getString(BatteryService.BATTERY_CHARGE_LIMIT, "80"))
        rechargePercentage = Integer.parseInt(
                configuration.getString(BatteryService.BATTERY_RECHARGE_THRESHOLD, (limitPercentage - 5).toString()))
        enabled = configuration.getBoolean(BatteryService.BATTERY_CHARGE_LIMIT_ENABLED, false)
        // manually fire onReceive() to update state if service is enabled
        onReceive(service, service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!enabled) {
            service.setControlState(BatteryService.CONTROL_STATE_DISABLED)
            return
        }

        val state = service.getControlState()

        if (intent.action == BatteryService.ACTION_BATTERY_STOP) {
            if (state == BatteryService.CONTROL_STATE_CHARGING || state == BatteryService.CONTROL_STATE_BOOST) {
                service.setControlState(BatteryService.CONTROL_STATE_STOP_FORCED)
            }
            return
        }

        if (intent.action == BatteryService.ACTION_BATTERY_CHARGE) {
            if (state == BatteryService.CONTROL_STATE_STOP_FORCED) {
                service.setControlState(BatteryService.CONTROL_STATE_CHARGING)
            }
            return
        }

        if (intent.action == BatteryService.ACTION_BATTERY_BOOST) {
            if (state == BatteryService.CONTROL_STATE_STOP || state == BatteryService.CONTROL_STATE_STOP_FORCED ||
                    state == BatteryService.CONTROL_STATE_CHARGING) {
                service.setControlState(BatteryService.CONTROL_STATE_BOOST)
            }
            return
        }

        val batteryLevel = getBatteryLevel(intent)
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
            BatteryService.CONTROL_STATE_STOP_FORCED -> {
                // nothing to do here.
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

    private fun getBatteryLevel(batteryIntent: Intent): Int {
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return if (level == -1 || scale == -1) {
            100
        } else {
            level * 100 / scale
        }
    }

    fun detach() {
        // unregister the listener that listens for relevant change events
        configuration.unregisterOnSharedPreferenceChangeListener(this.preferenceChangeListener)
        this.preferenceChangeListener = null
    }
}
