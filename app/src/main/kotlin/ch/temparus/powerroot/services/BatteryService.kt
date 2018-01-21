package ch.temparus.powerroot.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import ch.temparus.powerroot.MainActivity
import ch.temparus.powerroot.R
import ch.temparus.powerroot.SharedMethods
import ch.temparus.powerroot.receivers.BatteryReceiver

/**
 * An [Service] subclass for asynchronously monitor battery
 * state while charging in a service on a separate handler thread.
 */
class BatteryService : Service() {

    private val mNotificationBuilder by lazy(LazyThreadSafetyMode.NONE) {Notification.Builder(this)}
    private var mNotificationId = 1
    private var mBatteryReceiver: BatteryReceiver? = null

    override fun onCreate() {
        Log.d("BatteryService", "Service started!")
        running = true

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentApp = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val pendingIntentDisable = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = mNotificationBuilder
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_SYSTEM)
                .addAction(0, "Disable", pendingIntentDisable)
                .addAction(0, "Open App", pendingIntentApp)
                .setOngoing(true)
                .setContentTitle("Please wait!")
                .setContentText("Show Power Root!")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
        startForeground(mNotificationId, notification)

        mBatteryReceiver = BatteryReceiver(this@BatteryService)
        registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Thread.sleep(1000)
        return START_STICKY
    }

    fun setControlState(state: Int): Boolean {
        val hasChanged = BatteryService.state != state
        BatteryService.state = state
        if (hasChanged) {
            Log.d("BatteryService", "State changed to $state")
            stateChange = System.currentTimeMillis()
            when(state) {
                CONTROL_STATE_STOP -> {
                    BatteryService.setChargerState(false)
                    Log.d("BatteryService", "Charger disabled")
                }
                CONTROL_STATE_CHARGING,
                CONTROL_STATE_BOOST -> {
                    BatteryService.setChargerState(true)
                    Log.d("BatteryService", "Charger enabled (1)")
                    stopIfUnplugged()
                }
                else -> {
                    BatteryService.setChargerState(true)
                    Log.d("BatteryService", "Charger enabled (2)")
                }
            }
        }
        return hasChanged
    }

    fun getControlState(): Int {
        return BatteryService.state
    }

    private fun stopIfUnplugged() {
        val triggerState = getControlState()
        Handler().postDelayed({
            // continue only if the state didn't change in the meantime
            if (triggerState == getControlState() && !SharedMethods.isDevicePluggedIn(this)) {
                stopSelf()
            }
        }, POWER_SUPPLY_CHANGE_DELAY_MS)
    }

    override fun onDestroy() {
        // unregister the battery event receiver
        unregisterReceiver(mBatteryReceiver)

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
        const private val CHANGE_PENDING_TIMEOUT_MS: Long = 1000
        const private val CHARGING_CHANGE_DELAY_MS: Long = 500
        const private val POWER_SUPPLY_CHANGE_DELAY_MS: Long = 3000
        const val CONTROL_FILE = "/sys/class/power_supply/battery/charging_enabled"

        @Suppress("MemberVisibilityCanPrivate")
        const val CONTROL_STATE_UNKNOWN = -1
        const val CONTROL_STATE_DISABLED = 0
        const val CONTROL_STATE_STOP = 1
        const val CONTROL_STATE_CHARGING = 2
        const val CONTROL_STATE_BOOST = 3

        private var running = false
        private var state = CONTROL_STATE_UNKNOWN
        private var stateChange: Long = 0

        /**
         * Returns whether the service is running right now
         *
         * @return Whether service is running
         */
        fun isRunning(): Boolean {
            return running
        }

        /**
         * Returns whether a state change is pending
         *
         * @return Whether service is running
         */
        fun isStateChangePending(): Boolean {
            return System.currentTimeMillis() - stateChange < CHANGE_PENDING_TIMEOUT_MS
        }

        fun getBatteryLevel(batteryIntent: Intent): Int {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            return if (level == -1 || scale == -1) {
                100
            } else {
                level * 100 / scale
            }
        }

        fun start(context: Context) {
            Log.d("BatteryService", "Service started! (3)")
            Handler().postDelayed({
                Log.d("BatteryService", "Service started! (4)")
                if (SharedMethods.isDevicePluggedIn(context)) {
                    Log.d("BatteryService", "Service started! (5)")
                    context.startService(Intent(context, BatteryService::class.java))
                    Log.d("BatteryService", "Service started! (2)")
                    Toast.makeText(context, "Service started!", Toast.LENGTH_SHORT).show()
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
    }
}
