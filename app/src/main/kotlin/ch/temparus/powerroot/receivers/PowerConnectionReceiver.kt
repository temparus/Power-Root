package ch.temparus.powerroot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ch.temparus.powerroot.services.BatteryService

class PowerConnectionReceiver(private val service: BatteryService) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (service.isStateChangePending()) return

        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            Log.d("PowerConnectionReceiver", "Power connected!")
            service.setConnectionState(BatteryService.CONNECTION_STATE_CONNECTED)
        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d("PowerConnectionReceiver", "Power disconnected!")
            service.setConnectionState(BatteryService.CONNECTION_STATE_DISCONNECTED)
        }
    }
}
