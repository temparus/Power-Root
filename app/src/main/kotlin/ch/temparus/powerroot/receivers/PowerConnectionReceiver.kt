package ch.temparus.powerroot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.temparus.powerroot.services.BatteryService

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED && !BatteryService.isRunning()) {
            BatteryService.start(context)
        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED &&
                BatteryService.isRunning() && !BatteryService.isStateChangePending()) {
            BatteryService.stop(context)
        }
    }
}
