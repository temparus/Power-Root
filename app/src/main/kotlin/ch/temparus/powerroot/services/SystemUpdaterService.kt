package ch.temparus.powerroot.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import ch.temparus.powerroot.SharedMethods
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import ch.temparus.powerroot.services.notifications.SystemUpdaterServiceNotification

/**
 * An [Service] subclass for asynchronously monitor if there are new system updates
 * on a separate handler thread.
 */
class SystemUpdaterService : Service() {

    private var mNotification: SystemUpdaterServiceNotification? = null
    private var mConfiguration: SharedPreferences? = null

    override fun onCreate() {
        Log.d("SystemUpdaterService", "Service started!")
        running = true

        mConfiguration = PreferenceManager.getDefaultSharedPreferences(this@SystemUpdaterService)

        mNotification = SystemUpdaterServiceNotification(this)
        mNotification?.update()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    fun setUpdaterState(state: Int): Boolean {
        val hasChanged = SystemUpdaterService.state != state
        SystemUpdaterService.state = state
        if (hasChanged) {
            when (state) {
                STATE_UPDATE_DOWNLOADING -> {
                    // TODO
                }
                STATE_UPDATE_READY -> {
                    // TODO
                }
                STATE_UPDATE_INSTALLING -> {
                    // TODO
                }

                else -> { // STATE_NO_UPDATES
                    this.stopForeground(true)
                }
            }
            mNotification?.update()
        }
        return hasChanged
    }

    fun getUpdaterState(): Int {
        return SystemUpdaterService.state
    }

    override fun onDestroy() {
        // unregister the battery event receiver
        stopForeground(true)

        running = false
        Log.d("SystemUpdaterService", "Service stopped!")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        // Static configuration
        const val SYSTEM_UPDATE_DIRECTORY = "/.Ota"
        const val MAGISK_UPDATE_DIRECTORY = "/.Magisk"

        // SharedPreferences
        const val SYSTEM_UPDATE_STATE = "systemUpdateState"

        // Intent actions
        const val ACTION_INSTALL = "ch.temparus.powerroot.intent.ACTION_UPDATER_INSTALL"
        const val ACTION_IGNORE = "ch.temparus.powerroot.intent.ACTION_UPDATER_IGNORE"

        // BatteryService control states
        const val STATE_NO_UPDATE = 0
        const val STATE_UPDATE_DOWNLOADING = 1
        const val STATE_UPDATE_READY = 2
        const val STATE_UPDATE_INSTALLING = 3

        private var running = false
        private var state = STATE_NO_UPDATE

        fun isRunning(): Boolean {
            return running
        }

        fun start(context: Context) {
            Handler().post({
                if (SharedMethods.isDevicePluggedIn(context)) {
                    context.startService(Intent(context, SystemUpdaterService::class.java))
                }
            })
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SystemUpdaterService::class.java))
            state = STATE_NO_UPDATE
        }
    }
}
