package ch.temparus.powerroot.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v14.preference.PreferenceFragment
import ch.temparus.powerroot.R
import ch.temparus.powerroot.services.ProximityService


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
            }
        }

        mPreferences?.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)
    }

    override fun onDestroy() {
        mPreferences?.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener)
        super.onDestroy()
    }

    companion object {
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
