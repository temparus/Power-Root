package ch.temparus.powerroot.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.util.Log
import ch.temparus.powerroot.SharedMethods
import ch.temparus.powerroot.receivers.BatteryReceiver
import android.content.SharedPreferences
import android.os.BatteryManager
import android.preference.PreferenceManager
import ch.temparus.powerroot.receivers.BatteryServiceRestartReceiver
import ch.temparus.powerroot.receivers.PowerConnectionReceiver
import ch.temparus.powerroot.services.notifications.BatteryServiceNotification


/**
 * An [Service] subclass for asynchronously monitor battery
 * controlState while charging in a service on a separate handler thread.
 */
class BatteryService : Service() {

    private var mPreferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var mConfiguration: SharedPreferences? = null
    private var mNotification: BatteryServiceNotification? = null
    private var mBatteryReceiver: BatteryReceiver? = null
    private var mPowerConnectionReceiver: PowerConnectionReceiver? = null
    private var mChargeLimitPercentage: Int = 80
    private var mRechargeLevelPercentage: Int = 75
    private var mAutoReset = false

    override fun onCreate() {
        Log.i("BatteryService", "Service started!")

        mConfiguration = PreferenceManager.getDefaultSharedPreferences(this)
        mPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            var validKey = true
            when (key) {
                BatteryService.BATTERY_CHARGE_LIMIT_ENABLED -> {
                    checkServiceEnabledConfiguration()
                }
                BatteryService.BATTERY_RECHARGE_THRESHOLD -> {
                    mRechargeLevelPercentage = Integer.parseInt(
                            sharedPreferences.getString(BatteryService.BATTERY_RECHARGE_THRESHOLD, (mChargeLimitPercentage - 5).toString()))
                }
                BatteryService.BATTERY_CHARGE_LIMIT -> {
                    mChargeLimitPercentage = Integer.parseInt(sharedPreferences.getString(BatteryService.BATTERY_CHARGE_LIMIT, "80"))
                }
                else -> validKey = false
            }
            if (validKey) update()

        }
        mConfiguration?.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)

        // Register Power Connection Receiver
        mPowerConnectionReceiver = PowerConnectionReceiver(this@BatteryService)
        val powerIntentFilter = IntentFilter()
        powerIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        powerIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(mPowerConnectionReceiver, powerIntentFilter)

        mChargeLimitPercentage = Integer.parseInt(mConfiguration?.getString(BatteryService.BATTERY_CHARGE_LIMIT, "80"))
        mRechargeLevelPercentage = Integer.parseInt(
                mConfiguration?.getString(BatteryService.BATTERY_RECHARGE_THRESHOLD, (mChargeLimitPercentage - 5).toString()))

        checkServiceEnabledConfiguration()

        if (SharedMethods.isDevicePluggedIn(this)) {
            setConnectionState(CONNECTION_STATE_CONNECTED)
        } else {
            setConnectionState(CONNECTION_STATE_DISCONNECTED)
        }

        mNotification = BatteryServiceNotification(this)
        mNotification?.update()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun setControlState(state: Int): Boolean {
        Log.d("BatteryService", "setControlState() called ($state)!")
        val hasChanged = BatteryService.controlState != state
        BatteryService.controlState = state
        if (hasChanged) {
            stateChange = System.currentTimeMillis()
            when (state) {
                CONTROL_STATE_STOP,
                CONTROL_STATE_STOP_FORCED -> {
                    setChargerState(false)
                    if (state == CONTROL_STATE_STOP) {
                        mAutoReset = true
                    }

                    registerBatteryReceiver()
                    resetIfUnplugged(true)
                }
                CONTROL_STATE_CHARGING,
                CONTROL_STATE_BOOST -> {
                    setChargerState(true)
                    registerBatteryReceiver()
                    resetIfUnplugged()
                }
                else -> {
                    setChargerState(true)
                    unregisterBatteryReceiver()
                }
            }
            mNotification?.update()
        }
        return hasChanged
    }

    fun getControlState(): Int {
        return BatteryService.controlState
    }

    fun setConnectionState(state: Int): Boolean {
        Log.d("BatteryService", "setConnectionState() called ($state)!")
        val hasChanged = BatteryService.connectionState != state
        BatteryService.connectionState = state
        if (hasChanged) {
            when (state) {
                CONNECTION_STATE_CONNECTED -> {
                    update()
//                    registerBatteryReceiver()
                }
                else -> {
//                    unregisterBatteryReceiver()
                    setControlState(CONTROL_STATE_UNKNOWN)
                }
            }
            mNotification?.update()
        }
        return hasChanged
    }

    fun getConnectionState(): Int {
        return BatteryService.connectionState
    }

    fun isStateChangePending(): Boolean {
        return System.currentTimeMillis() - BatteryService.stateChange < CHANGE_PENDING_TIMEOUT_MS
    }

    fun update() {
        Thread {
            if (getConnectionState() != CONNECTION_STATE_CONNECTED && getControlState() > 0) {
                setControlState(CONTROL_STATE_UNKNOWN)
            } else {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val isCharging = batteryManager.isCharging

                when (getControlState()) {
                    CONTROL_STATE_STOP -> {
                        if (batteryLevel <= mRechargeLevelPercentage) {
                            Log.d("BatteryReceiver", "Charging2")
                            setControlState(BatteryService.CONTROL_STATE_CHARGING)
                        }
                    }
                    CONTROL_STATE_CHARGING -> {
                        if (batteryLevel >= mChargeLimitPercentage) {
                            Log.d("BatteryReceiver", "Stopped2")
                            setControlState(CONTROL_STATE_STOP)
                        }
                    }
                    CONTROL_STATE_BOOST -> {
                        if (!(isCharging || isStateChangePending())) {
                            Log.d("BatteryReceiver", "Stopped3")
                            setControlState(CONTROL_STATE_STOP)
                        }
                    }
                    CONTROL_STATE_STOP_FORCED,
                    CONTROL_STATE_DISABLED -> {
                        // nothing to do here.
                    }
                    else -> {
                        // when the service was started or has been enabled, charge until limit
                        if (batteryLevel > mRechargeLevelPercentage) {
                            Log.d("BatteryReceiver", "Stopped4")
                            setControlState(CONTROL_STATE_STOP)
                        } else {
                            Log.d("BatteryReceiver", "Charging3")
                            setControlState(CONTROL_STATE_CHARGING)
                        }
                    }
                }
            }
        }.start()
    }

    private fun checkServiceEnabledConfiguration() {
        val isEnabled = mConfiguration?.getBoolean(BatteryService.BATTERY_CHARGE_LIMIT_ENABLED, false)
        Log.d("BatteryService", "Service isEnabled: $isEnabled")
        if (isEnabled == null || !isEnabled) {
            setControlState(CONTROL_STATE_DISABLED)
        } else if (getControlState() == CONTROL_STATE_DISABLED) {
            setControlState(CONTROL_STATE_UNKNOWN)
        }
    }

    private fun registerBatteryReceiver() {
        if (mBatteryReceiver != null) return

        // Register Battery Receiver
        mBatteryReceiver = BatteryReceiver(this@BatteryService)
        val batteryIntentFilter = IntentFilter()
        batteryIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        batteryIntentFilter.addAction(ACTION_BATTERY_BOOST)
        batteryIntentFilter.addAction(ACTION_BATTERY_CHARGE)
        batteryIntentFilter.addAction(ACTION_BATTERY_STOP)
        registerReceiver(mBatteryReceiver, batteryIntentFilter)
    }

    private fun unregisterBatteryReceiver() {
        if (mBatteryReceiver != null) {
            unregisterReceiver(mBatteryReceiver)
            mBatteryReceiver = null
        }
    }

    private fun resetIfUnplugged(repeat: Boolean = false) {
        val triggerState = getControlState()
        val context = this
        Thread {
            Thread.sleep(POWER_SUPPLY_CHANGE_DELAY_MS)
            // continue only if the controlState didn't change in the meantime
            if (triggerState == getControlState() && getConnectionState() == CONNECTION_STATE_CONNECTED) {
                if (!SharedMethods.isDevicePluggedIn(context)) {
                    Log.d("BatteryService", "resetIfUnplugged")
                    resetBatteryStatsIfFull()
                    setConnectionState(CONNECTION_STATE_DISCONNECTED)
                } else if (repeat) {
                    resetIfUnplugged(repeat)
                }
            }
        }.start()
    }

    private fun resetBatteryStatsIfFull() {
        if (mAutoReset && mConfiguration?.getBoolean(BATTERY_STATS_AUTO_RESET, true) == true) {
            BatteryService.resetBatteryStats()
        }
    }

    override fun onDestroy() {
        // unregister the battery event receiver
        unregisterBatteryReceiver()
        unregisterReceiver(mPowerConnectionReceiver)
        mConfiguration?.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener)
        stopForeground(true)

        // clear the reference to the battery receiver for GC
        mBatteryReceiver = null
        mPowerConnectionReceiver = null
        mPreferenceChangeListener = null
        val broadcastIntent = Intent(this, BatteryServiceRestartReceiver::class.java)
        sendBroadcast(broadcastIntent)
        Log.i("BatteryService", "Service stopped!")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        // Static configuration
        private const val CHANGE_PENDING_TIMEOUT_MS: Long = 1000
        private const val CHARGING_CHANGE_DELAY_MS: Long = 500
        private const val POWER_SUPPLY_CHANGE_DELAY_MS: Long = 5000
        private const val CONTROL_FILE = "/sys/class/power_supply/battery/charging_enabled"
        const val POWER_SUPPLY_DIRECTORY = "/sys/class/power_supply"

        // SharedPreferences
        const val BATTERY_CHARGE_LIMIT_ENABLED = "batteryChargeLimitEnabled"
        const val BATTERY_CHARGE_LIMIT = "batteryChargeLimit"
        const val BATTERY_RECHARGE_THRESHOLD = "batteryRechargeThreshold"
        const val BATTERY_STATS_AUTO_RESET = "batteryStatsAutoReset"

        // Intent actions
        const val ACTION_BATTERY_BOOST = "ch.temparus.powerroot.intent.ACTION_BATTERY_BOOST"
        const val ACTION_BATTERY_CHARGE = "ch.temparus.powerroot.intent.ACTION_BATTERY_CHARGE"
        const val ACTION_BATTERY_STOP = "ch.temparus.powerroot.intent.ACTION_BATTERY_STOP"

        // BatteryService control states
        const val CONTROL_STATE_UNKNOWN = -1
        const val CONTROL_STATE_DISABLED = 0
        const val CONTROL_STATE_STOP = 1
        const val CONTROL_STATE_STOP_FORCED = 2
        const val CONTROL_STATE_CHARGING = 3
        const val CONTROL_STATE_BOOST = 4

        const val CONNECTION_STATE_UNKNOWN = -1
        const val CONNECTION_STATE_DISCONNECTED = 0
        const val CONNECTION_STATE_CONNECTED = 1

        private var controlState = CONTROL_STATE_UNKNOWN
        private var connectionState = CONNECTION_STATE_UNKNOWN
        private var stateChange: Long = 0

        fun start(context: Context) {
            Handler().postDelayed({
                context.startService(Intent(context, BatteryService::class.java))
            }, CHARGING_CHANGE_DELAY_MS)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryService::class.java))
            setChargerState(true)
            controlState = CONTROL_STATE_UNKNOWN
        }

        private fun setChargerState(enabled: Boolean) {
            SharedMethods.writeControlFile(CONTROL_FILE, if (enabled) { "1" } else { "0" })
            Log.d("BatteryService", "Changed charger controlState ($enabled)!")
        }

        private fun resetBatteryStats() {
            SharedMethods.executeRootCommand("dumpsys batterystats --reset")
        }
    }
}
