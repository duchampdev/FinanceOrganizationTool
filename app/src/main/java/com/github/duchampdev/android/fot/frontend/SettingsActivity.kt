/*
 * Copyright (c) 2019 duchampdev.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.duchampdev.android.fot.frontend

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.github.duchampdev.android.fot.R

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
    }


    class SettingsFragment: PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val nightModePref = findPreference<ListPreference>(resources.getString(R.string.pref_key_nightmode))
            nightModePref?.setOnPreferenceChangeListener { preference, newValue  ->
                Toast.makeText(activity!!.applicationContext, preference.context.resources.getString(R.string.settings_nightmode_changed_restart_needed), Toast.LENGTH_LONG)
                                .show()
                        preference.summary = newValue.toString()
                        true
                    }
            nightModePref?.summary = nightModePref?.value
        }
    }
}