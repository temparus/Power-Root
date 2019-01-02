package ch.temparus.powerroot.receivers

import android.content.*
import android.os.BatteryManager
import android.preference.PreferenceManager
import android.util.Log
import ch.temparus.powerroot.services.BatteryService

class BatteryReceiver(private val service: BatteryService) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (service.getConnectionState() != BatteryService.CONNECTION_STATE_CONNECTED) {
            return
        }

        val controlState = service.getControlState()

        if (intent.action == BatteryService.ACTION_BATTERY_STOP) {
            if (controlState == BatteryService.CONTROL_STATE_CHARGING || controlState == BatteryService.CONTROL_STATE_BOOST) {
                Log.d("BatteryReceiver", "Stopped")
                service.setControlState(BatteryService.CONTROL_STATE_STOP_FORCED)
            }
            return
        }

        if (intent.action == BatteryService.ACTION_BATTERY_CHARGE) {
            if (controlState == BatteryService.CONTROL_STATE_STOP_FORCED) {
                Log.d("BatteryReceiver", "Charging")
                service.setControlState(BatteryService.CONTROL_STATE_CHARGING)
            }
            return
        }

        if (intent.action == BatteryService.ACTION_BATTERY_BOOST) {
            if (controlState == BatteryService.CONTROL_STATE_STOP || controlState == BatteryService.CONTROL_STATE_STOP_FORCED ||
                    controlState == BatteryService.CONTROL_STATE_CHARGING) {
                Log.d("BatteryReceiver", "Boost Charging")
                service.setControlState(BatteryService.CONTROL_STATE_BOOST)
            }
            return
        }

        // Battery state changed -> update service state
        service.update()
    }
}
