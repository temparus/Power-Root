package ch.temparus.powerroot.receivers

import android.content.*
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Log
import ch.temparus.powerroot.services.BatteryService

class BatteryServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Restarting BatteryService...")
        val configuration: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (configuration.getBoolean(BatteryService.BATTERY_CHARGE_LIMIT_ENABLED, false)) {
//            context.startForgroundService(Intent(context, BatteryService::class.java))
            ContextCompat.startForegroundService(context, Intent(context, BatteryService::class.java))
        }
    }

    companion object {
        const val TAG: String = "BatteryServiceRestart"
    }
}
