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

package com.github.duchampdev.android.fot.frontend;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.duchampdev.android.fot.R;
import com.github.duchampdev.android.fot.backend.FinanceOrgaToolDB;
import com.github.duchampdev.android.fot.frontend.settings.SettingsActivity;


public class MainActivity extends AppCompatActivity {

    private int activeMenuItemId = Integer.MAX_VALUE;
    private Fragment activeFragment;

    private FloatingActionButton fab;

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = menuItem -> {
        Fragment selected = null;
        if (menuItem.getItemId() == activeMenuItemId)
            return true; // nothing to change, don't corrupt present view; inclusion into switch would require const val
        switch (menuItem.getItemId()) {
            case R.id.navigation_dashboard:
                selected = DashboardFragment.newInstance();
                fab.setVisibility(View.VISIBLE);
                break;
            case R.id.navigation_stats:
                selected = StatsFragment.newInstance();
                fab.setVisibility(View.GONE);
                break;
        }
        activeFragment = selected;
        activeMenuItemId = menuItem.getItemId();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, selected);
        transaction.commit();
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (activeFragment instanceof DashboardFragment) {
                ((DashboardFragment) activeFragment).addNewTransaction();
            }
        });
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        navigation.setSelectedItemId(R.id.navigation_dashboard);
        activeMenuItemId = R.id.navigation_dashboard;

        String[] possibleValues = getResources().getStringArray(R.array.settings_nightmode_entry_vals);
        String nightModePrefValue = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_key_nightmode), possibleValues[0]);
        if(nightModePrefValue.equals(possibleValues[0])) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        } else if(nightModePrefValue.equals(possibleValues[1])) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if(nightModePrefValue.equals(possibleValues[2])) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        FinanceOrgaToolDB.getInstance(this).closeDB();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_catman) {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.main_legal) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getString(R.string.licenses));
            dialogBuilder.setMessage(Html.fromHtml(getResources().getString(R.string.license_text_mpandroidchart), Html.FROM_HTML_MODE_LEGACY));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.close), (dialog, which) -> {
            });
            dialogBuilder.create().show();
            return true;
        }
        if (item.getItemId() == R.id.main_about) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getString(R.string.about));
            dialogBuilder.setMessage(Html.fromHtml(getResources().getString(R.string.license_text_app), Html.FROM_HTML_MODE_LEGACY));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.close), (dialog, which) -> {
            });
            dialogBuilder.create().show();
            return true;
        }
        if (item.getItemId() == R.id.main_dataprotection) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getString(R.string.dataprotection));
            dialogBuilder.setMessage(Html.fromHtml(getResources().getString(R.string.dataprotection_content), Html.FROM_HTML_MODE_LEGACY));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.close), (dialog, which) -> {
            });
            dialogBuilder.create().show();
            return true;
        }
        if (item.getItemId() == R.id.main_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        return false;
    }
}
