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
import android.preference.PreferenceManager
import ch.temparus.powerroot.services.notifications.BatteryNotification

/**
 * An [Service] subclass for asynchronously monitor battery
 * state while charging in a service on a separate handler thread.
 */
class BatteryService : Service() {

    private var mNotification: BatteryNotification? = null
    private var mConfiguration: SharedPreferences? = null
    private var mBatteryReceiver: BatteryReceiver? = null
    private var mAutoReset = false

    override fun onCreate() {
        Log.d("BatteryService", "Service started!")
        running = true

        mConfiguration = PreferenceManager.getDefaultSharedPreferences(this)
        mBatteryReceiver = BatteryReceiver(this@BatteryService)
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.addAction(ACTION_BATTERY_BOOST)
        intentFilter.addAction(ACTION_BATTERY_CHARGE)
        intentFilter.addAction(ACTION_BATTERY_STOP)
        registerReceiver(mBatteryReceiver, intentFilter)

        mNotification = BatteryNotification(this)
        mNotification?.update()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun setControlState(state: Int): Boolean {
        val hasChanged = BatteryService.state != state
        BatteryService.state = state
        if (hasChanged) {
            stateChange = System.currentTimeMillis()
            when (state) {
                CONTROL_STATE_STOP,
                CONTROL_STATE_STOP_FORCED -> {
                    BatteryService.setChargerState(false)
                    if (state == CONTROL_STATE_STOP) {
                        mAutoReset = true
                    }
                    stopIfUnplugged(true)
                }
                CONTROL_STATE_CHARGING,
                CONTROL_STATE_BOOST -> {
                    BatteryService.setChargerState(true)
                    stopIfUnplugged()
                }
                else -> {
                    BatteryService.setChargerState(true)
                }
            }
            mNotification?.update()
        }
        return hasChanged
    }

    fun getControlState(): Int {
        return BatteryService.state
    }

    private fun stopIfUnplugged(repeat: Boolean = false) {
        val triggerState = getControlState()
        Handler().postDelayed({
            // continue only if the state didn't change in the meantime
            if (triggerState == getControlState()) {
                if (!SharedMethods.isDevicePluggedIn(this)) {
                    resetBatteryStatsIfFull()
                    setControlState(CONTROL_STATE_UNKNOWN)
                    stopSelf()
                } else if (repeat) {
                    stopIfUnplugged(repeat)
                }
            }
        }, POWER_SUPPLY_CHANGE_DELAY_MS)
    }

    private fun resetBatteryStatsIfFull() {
        if (mAutoReset && mConfiguration?.getBoolean(BATTERY_STATS_AUTO_RESET, true) == true) {
            BatteryService.resetBatteryStats()
        }
    }

    override fun onDestroy() {
        // unregister the battery event receiver
        unregisterReceiver(mBatteryReceiver)
        stopForeground(true)

        // make the BatteryReceiver and dependencies ready for garbage-collection
        mBatteryReceiver!!.detach()
        // clear the reference to the battery receiver for GC
        mBatteryReceiver = null
        running = false
        Log.d("BatteryService", "Service stopped!")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        // Static configuration
        private const val CHANGE_PENDING_TIMEOUT_MS: Long = 1000
        private const val CHARGING_CHANGE_DELAY_MS: Long = 500
        private const val POWER_SUPPLY_CHANGE_DELAY_MS: Long = 3000
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

        private var running = false
        private var state = CONTROL_STATE_UNKNOWN
        private var stateChange: Long = 0

        fun isRunning(): Boolean {
            return running
        }

        fun isStateChangePending(): Boolean {
            return System.currentTimeMillis() - stateChange < CHANGE_PENDING_TIMEOUT_MS
        }

        fun start(context: Context) {
            Handler().postDelayed({
                if (SharedMethods.isDevicePluggedIn(context)) {
                    context.startService(Intent(context, BatteryService::class.java))
                }
            }, CHARGING_CHANGE_DELAY_MS)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryService::class.java))
            setChargerState(true)
            state = CONTROL_STATE_UNKNOWN
        }

        private fun setChargerState(enabled: Boolean) {
            SharedMethods.writeControlFile(CONTROL_FILE, if (enabled) { "1" } else { "0" })
        }

        private fun resetBatteryStats() {
            SharedMethods.executeRootCommand("dumpsys batterystats --reset")
        }
    }
}
