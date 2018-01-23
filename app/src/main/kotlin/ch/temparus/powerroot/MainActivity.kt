package ch.temparus.powerroot

import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.View
import ch.temparus.powerroot.fragments.AboutFragment
import ch.temparus.powerroot.fragments.ConfigurationFragment
import ch.temparus.powerroot.services.BatteryService

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_configuration -> {
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, ConfigurationFragment.newInstance())
                transaction.commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_update -> {
                // mTextMessage!!.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_about -> {
                // mTextMessage!!.setText(R.string.title_notifications)
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, AboutFragment.newInstance())
                // transaction.addToBackStack(null)
                transaction.commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, ConfigurationFragment.newInstance())
        transaction.commit()

        Handler().post({ BatteryService.start(this) })
    }
}
