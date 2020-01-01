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

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.github.duchampdev.android.fot.R
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: AppCompatActivity() {

    private var activeMenuItemId = Int.MAX_VALUE
    private lateinit var activeFragment: Fragment

    private val navigationListener: BottomNavigationView.OnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
        val selectedFragment = when(menuItem.itemId) {
            activeMenuItemId -> return@OnNavigationItemSelectedListener true // nothing to change, don't corrupt current view
            R.id.navigation_dashboard -> {
                fab.show()
                DashboardFragment.newInstance()
            }
            R.id.navigation_stats -> {
                fab.hide()
                StatsFragment.newInstance()
            }
            else -> throw IllegalStateException()
        }

        activeFragment = selectedFragment
        activeMenuItemId = menuItem.itemId
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, selectedFragment)
        transaction.commit()
        return@OnNavigationItemSelectedListener true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fab.setOnClickListener {
            if (activeFragment is DashboardFragment) (activeFragment as DashboardFragment).addNewTransaction()
        }

        navigation.setOnNavigationItemSelectedListener(navigationListener)

        navigation.selectedItemId = R.id.navigation_dashboard
        activeMenuItemId = R.id.navigation_dashboard

        val possibleNightModeValues = resources.getStringArray(R.array.settings_nightmode_entry_vals)
        val nightModePrefValue = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(resources.getString(R.string.pref_key_nightmode), possibleNightModeValues[0])

        val nightMode = when(nightModePrefValue) {
            possibleNightModeValues[0] -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            possibleNightModeValues[1]  -> AppCompatDelegate.MODE_NIGHT_YES
            possibleNightModeValues[2] -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw IllegalStateException()
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        FinanceOrgaToolDB.getInstance(this).closeDB()
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.main_catman -> {
                startActivity(Intent(this, CategoryActivity::class.java))
                true
            }
            R.id.main_legal -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.licenses))
                        .setMessage(Html.fromHtml(resources.getString(R.string.license_text_mpandroidchart), Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(resources.getString(R.string.close)) { _, _ -> }
                        .create()
                        .show()
                true
            }
            R.id.main_about -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.about))
                        .setMessage(Html.fromHtml(resources.getString(R.string.license_text_app), Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(resources.getString(R.string.close)) { _, _ -> }
                        .create()
                        .show()
                true
            }
            R.id.main_dataprotection -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.dataprotection))
                        .setMessage(Html.fromHtml(resources.getString(R.string.dataprotection_content), Html.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(resources.getString(R.string.close)) { _, _ -> }
                        .create()
                        .show()
                true
            }
            R.id.main_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> false
        }
    }
}