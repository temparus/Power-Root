package ch.temparus.powerroot.fragments


import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import ch.temparus.powerroot.R


/**
 * A [PreferenceFragment] subclass.
 * Use the [ConfigurationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConfigurationFragment : PreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.battery_service)
        addPreferencesFromResource(R.xml.proximity_service)
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
