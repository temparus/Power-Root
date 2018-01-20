package ch.temparus.powerroot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ch.temparus.powerroot.services.BatteryService

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED && !BatteryService.isRunning()) {
            Log.d("Power State", "ACTION_POWER_CONNECTED")
            BatteryService.start(context)
        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED &&
                BatteryService.isRunning() && !BatteryService.isStateChangePending()) {
            Log.d("Power State", "ACTION_POWER_DISCONNECTED")
            BatteryService.stop(context)
        }
    }
}
