/*
 * Copyright (C) 2012 ENTERTAILION LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.entertailion.android.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.wallpaper.StaticWallpaperChooser;
import com.entertailion.android.launcher.wallpaper.WallpaperActivity;

/**
 * Handle settings for app. Invoked by the user from the menu.
 * 
 * @author leon_nicholls
 * 
 */
public class PreferencesActivity extends PreferenceActivity {
	private static final String LOG_TAG = "PreferencesActivity";
	public static final String GENERAL_CLOCK = "general.clock";
	public static final String GENERAL_WEATHER = "general.weather";
	public static final String GENERAL_LIVE_TV = "general.live_tv";
	public static final String GENERAL_WALLPAPER = "general.wallpaper";
	public static final String ROWS_ROW_NAME = "rows.row_name";
	public static final String ROWS_ITEM_NAME = "rows.item_name";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// General
		Preference pref = (Preference) findPreference(GENERAL_CLOCK);
		pref.setSummary(pref.getSharedPreferences().getBoolean(GENERAL_CLOCK, true) ? getString(R.string.preferences_general_clock_summary_checked)
				: getString(R.string.preferences_general_clock_summary_unchecked));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue.equals(Boolean.TRUE)) {
					preference.setSummary(getString(R.string.preferences_general_clock_summary_checked));
					Analytics.logEvent(Analytics.PREFERENCE_CLOCK_ON);
				} else {
					preference.setSummary(getString(R.string.preferences_general_clock_summary_unchecked));
					Analytics.logEvent(Analytics.PREFERENCE_CLOCK_OFF);
				}
				return true;
			}

		});

		pref = (Preference) findPreference(GENERAL_WEATHER);
		pref.setSummary(pref.getSharedPreferences().getBoolean(GENERAL_WEATHER, true) ? getString(R.string.preferences_general_weather_summary_checked)
				: getString(R.string.preferences_general_weather_summary_unchecked));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue.equals(Boolean.TRUE)) {
					preference.setSummary(getString(R.string.preferences_general_weather_summary_checked));
					Analytics.logEvent(Analytics.PREFERENCE_WEATHER_ON);
				} else {
					preference.setSummary(getString(R.string.preferences_general_weather_summary_unchecked));
					Analytics.logEvent(Analytics.PREFERENCE_WEATHER_OFF);
				}
				return true;
			}

		});
		
		pref = (Preference) findPreference(GENERAL_LIVE_TV);
		pref.setSummary(pref.getSharedPreferences().getBoolean(GENERAL_LIVE_TV, true) ? getString(R.string.preferences_general_livetv_summary_checked)
				: getString(R.string.preferences_general_livetv_summary_unchecked));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue.equals(Boolean.TRUE)) {
					preference.setSummary(getString(R.string.preferences_general_livetv_summary_checked));
					Analytics.logEvent(Analytics.PREFERENCE_LIVE_TV_ON);
				} else {
					preference.setSummary(getString(R.string.preferences_general_livetv_summary_unchecked));
					Analytics.logEvent(Analytics.PREFERENCE_LIVE_TV_OFF);
				}
				return true;
			}

		});

		// Wallpaper
		Preference purchasePref = (Preference) findPreference(GENERAL_WALLPAPER);
		purchasePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				//Intent intent = new Intent(PreferencesActivity.this, StaticWallpaperChooser.class);
				//Intent intent = new Intent(PreferencesActivity.this, SolidColorWallpaperChooser.class);
				//Intent intent = new Intent(PreferencesActivity.this, LiveWallpaperChooser.class);
				Intent intent = new Intent(PreferencesActivity.this, WallpaperActivity.class);
                startActivity(intent);
                PreferencesActivity.this.finish();
				return true;
			}

		});

		// Rows
		pref = (Preference) findPreference(ROWS_ROW_NAME);
		pref.setSummary(pref.getSharedPreferences().getBoolean(ROWS_ROW_NAME, true) ? getString(R.string.preferences_rows_row_name_summary_checked)
				: getString(R.string.preferences_rows_row_name_summary_unchecked));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue.equals(Boolean.TRUE)) {
					preference.setSummary(getString(R.string.preferences_rows_row_name_summary_checked));
					Analytics.logEvent(Analytics.PREFERENCE_ROW_NAME_ON);
				} else {
					preference.setSummary(getString(R.string.preferences_rows_row_name_summary_unchecked));
					Analytics.logEvent(Analytics.PREFERENCE_ROW_NAME_OFF);
				}
				return true;
			}

		});

		pref = (Preference) findPreference(ROWS_ITEM_NAME);
		pref.setSummary(pref.getSharedPreferences().getBoolean(ROWS_ITEM_NAME, true) ? getString(R.string.preferences_rows_item_name_summary_checked)
				: getString(R.string.preferences_rows_item_name_summary_unchecked));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue.equals(Boolean.TRUE)) {
					preference.setSummary(getString(R.string.preferences_rows_item_name_summary_checked));
					Analytics.logEvent(Analytics.PREFERENCE_ITEM_NAME_ON);
				} else {
					preference.setSummary(getString(R.string.preferences_rows_item_name_summary_unchecked));
					Analytics.logEvent(Analytics.PREFERENCE_ITEM_NAME_OFF);
				}
				return true;
			}

		});

		Analytics.createAnalytics(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Start Google Analytics for this activity
		Analytics.startAnalytics(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Stop Google Analytics for this activity
		Analytics.stopAnalytics(this);
	}
}