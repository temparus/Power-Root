package ch.temparus.powerroot.services

import android.content.Context
import android.os.PowerManager
import ch.temparus.powerroot.SharedMethods

@Suppress("MemberVisibilityCanBePrivate")
object ScreenHandler {

    private var wakeLock: PowerManager.WakeLock? = null

    fun isOn(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        return powerManager.isInteractive
    }

    fun turnOn(context: Context, timeout: Long): Boolean {
        if (!isOn(context)) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeUpWakeLock")
            }
            if (wakeLock!!.isHeld) {
                wakeLock?.release()
            }
            wakeLock?.acquire(timeout)
            return true
        }
        return false

    }

    fun turnOff(context: Context): Boolean {
        if (isOn(context)) {
            // Simulate press of the power button
            SharedMethods.executeRootCommand("input keyevent 26")

            return true
        }
        return false
    }
}