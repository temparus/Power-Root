package ch.temparus.powerroot.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v14.preference.PreferenceFragment
import ch.temparus.powerroot.R
import ch.temparus.powerroot.services.ProximityService
import ch.temparus.powerroot.SharedMethods
import android.R.string.cancel
import android.os.CountDownTimer
import android.widget.Toast
import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.util.Log


/**
 * A [PreferenceFragment] subclass.
 * Use the [ConfigurationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConfigurationFragment : PreferenceFragment() {

    private var mPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var mPreferences: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.battery_service)
        addPreferencesFromResource(R.xml.proximity_service)

        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        mPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            if (key == ProximityService.PREF_ENABLED) {
                if (mPreferences!!.getBoolean(key, false)) {
                    ProximityService.start(activity)
                } else {
                    ProximityService.stop(activity)
                }
            } else if (key == ProximityService.PREF_LOCK_SCREEN) {
                if (mPreferences!!.getBoolean(key, false)) {
                    Log.d("ProximitySensorFragmentGugus", "PREF_DEVICE_ADMIN = ${mPreferences!!.getBoolean(PREF_DEVICE_ADMIN, false)}")
                    if (!mPreferences!!.getBoolean(PREF_DEVICE_ADMIN, false)) {
                        Log.d("ProximitySensorGugus", "Request Admin Rights!")
                        SharedMethods.requestLockScreenAdminRights(activity, DEVICE_ADMIN_REQUEST_CODE)
                    }
                } else {
                    if (mPreferences!!.getBoolean(PREF_DEVICE_ADMIN, false)) {
                        SharedMethods.revokeDeviceAdminPermission(activity)
                        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                                .putBoolean(PREF_DEVICE_ADMIN, false)
                                .apply()
                    }
                }
            }
        }

        mPreferences?.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_ADMIN_REQUEST_CODE) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                    .putBoolean(ProximityService.PREF_LOCK_SCREEN, true)
                    .putBoolean(PREF_DEVICE_ADMIN, true)
                    .apply()
        }
    }


    override fun onDestroy() {
        mPreferences?.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener)
        super.onDestroy()
    }

    companion object {
        private const val PREF_DEVICE_ADMIN = "isDeviceAdmin"
        const val DEVICE_ADMIN_REQUEST_CODE = 2

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ConfigurationFragment.
         */
        fun newInstance(): ConfigurationFragment {
            return ConfigurationFragment()
        }
    }

}// Required empty public constructor
