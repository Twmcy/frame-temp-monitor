package com.nakkeez.frametempmonitor.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.nakkeez.frametempmonitor.MainActivity
import com.nakkeez.frametempmonitor.R

/**
 * Preferences screen
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val frameRatePreference = findPreference<SwitchPreferenceCompat>("frame_rate")
            frameRatePreference?.onPreferenceChangeListener = this

            val batteryTemperaturePreference =
                findPreference<SwitchPreferenceCompat>("battery_temperature")
            batteryTemperaturePreference?.onPreferenceChangeListener = this
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            // Send a message to users telling about potential negative impacts these settings may cause
            if ((preference.key == "frame_rate") && (newValue is Boolean) && newValue) {
                Toast.makeText(
                    context,
                    "Calculating frame rate may negatively impact performance",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if ((preference.key == "battery_temperature") && (newValue is Boolean) && newValue) {
                Toast.makeText(
                    context,
                    "Tracking battery temperature may increase the battery drain",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if ((preference.key == "cpu_temperature") && (newValue is Boolean) && newValue) {
                Toast.makeText(
                    context,
                    "This application may not be able to track CPU temperature on every device",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Return to MainActivity when Up button is pressed
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                // Clear existing instance of MainActivity from the activity stack
                // and create a new instance so preferences will take effect
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}