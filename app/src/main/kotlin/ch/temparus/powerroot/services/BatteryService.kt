package ch.temparus.powerroot.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import ch.temparus.powerroot.MainActivity
import ch.temparus.powerroot.R
import ch.temparus.powerroot.SharedMethods
import ch.temparus.powerroot.receivers.BatteryReceiver
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.app.NotificationChannel

/**
 * An [Service] subclass for asynchronously monitor battery
 * state while charging in a service on a separate handler thread.
 */
class BatteryService : Service() {

    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private var mNotificationId = 1
    private var mBatteryReceiver: BatteryReceiver? = null

    override fun onCreate() {
        Log.d("BatteryService", "Service started!")
        running = true

        mBatteryReceiver = BatteryReceiver(this@BatteryService)
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.addAction(ACTION_BATTERY_BOOST)
        intentFilter.addAction(ACTION_BATTERY_CHARGE)
        intentFilter.addAction(ACTION_BATTERY_STOP)
        registerReceiver(mBatteryReceiver, intentFilter)

        createNotification()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelOreo(NOTIFICATION_CHANNEL_ID)
        }
        mNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        val showActivityIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                this,
                0,
                showActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        mNotificationBuilder!!
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setContentTitle("Battery Charge Control")
                .setSmallIcon(R.drawable.ic_battery_charging_full_black_24dp)
                .color = ContextCompat.getColor(this, R.color.colorPrimary)
        @Suppress("DEPRECATION")
        mNotificationBuilder!!.priority = Notification.PRIORITY_LOW

        updateNotification()
    }

    private fun updateNotification() {
        if (mNotificationBuilder == null) {
            createNotification()
        }

        mNotificationBuilder!!.mActions.clear()

        val state = getControlState()

        if (state == CONTROL_STATE_DISABLED || state == CONTROL_STATE_UNKNOWN) {
            stopForeground(true)
            return
        }

        if (state == CONTROL_STATE_STOP_FORCED) {
            val chargePendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(ACTION_BATTERY_CHARGE),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder!!.addAction(NotificationCompat.Action(
                    android.R.drawable.ic_dialog_alert,
                    "Charge Now",
                    chargePendingIntent))
        }

        if (state == CONTROL_STATE_CHARGING || state == CONTROL_STATE_BOOST) {
            val chargePendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(ACTION_BATTERY_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder!!.addAction(NotificationCompat.Action(
                    android.R.drawable.ic_dialog_alert,
                    "Stop Charging",
                    chargePendingIntent))
        }

        if (state == CONTROL_STATE_STOP || state == CONTROL_STATE_STOP_FORCED || state == CONTROL_STATE_CHARGING) {
            val boostPendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(ACTION_BATTERY_BOOST),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder!!.addAction(NotificationCompat.Action(
                    android.R.drawable.ic_dialog_alert,
                    "Boost Charge",
                    boostPendingIntent))
        }

        mNotificationBuilder!!.mContentText =
                when(state) {
                    CONTROL_STATE_CHARGING -> "Charging"
                    CONTROL_STATE_BOOST -> "Boost Charging"
                    CONTROL_STATE_STOP -> "Charging limit reached"
                    CONTROL_STATE_STOP_FORCED -> "Charging interrupted by user"
                    else -> ""
                }

        startForeground(mNotificationId, mNotificationBuilder!!.build())
    }

    @TargetApi(26)
    private fun createNotificationChannelOreo(channelId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
                channelId,
                "Battery Charging Notification",
                NotificationManager.IMPORTANCE_LOW)

        // Configure the notification channel.
        notificationChannel.description = "Shows Battery Charging Information"
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
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
                CONTROL_STATE_STOP,
                CONTROL_STATE_STOP_FORCED -> {
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
            updateNotification()
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
        private const val NOTIFICATION_CHANNEL_ID: String = "PowerRootBatteryNotification"
        private const val CHANGE_PENDING_TIMEOUT_MS: Long = 1000
        private const val CHARGING_CHANGE_DELAY_MS: Long = 500
        private const val POWER_SUPPLY_CHANGE_DELAY_MS: Long = 3000
        const val CONTROL_FILE = "/sys/class/power_supply/battery/charging_enabled"

        const val ACTION_BATTERY_BOOST = "ch.temparus.powerroot.intent.ACTION_BATTERY_BOOST"
        const val ACTION_BATTERY_CHARGE = "ch.temparus.powerroot.intent.ACTION_BATTERY_CHARGE"
        const val ACTION_BATTERY_STOP = "ch.temparus.powerroot.intent.ACTION_BATTERY_STOP"

        const val CONTROL_STATE_UNKNOWN = -1
        const val CONTROL_STATE_DISABLED = 0
        const val CONTROL_STATE_STOP = 1
        const val CONTROL_STATE_STOP_FORCED = 2
        const val CONTROL_STATE_CHARGING = 3
        const val CONTROL_STATE_BOOST = 4

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
