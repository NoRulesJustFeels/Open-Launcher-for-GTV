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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.util.Log;

import com.entertailion.android.launcher.apps.ApplicationInfo;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.database.RecentAppsTable;
import com.entertailion.android.launcher.database.RowsTable;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.row.RowInfo;
import com.entertailion.android.launcher.utils.LocationData;
import com.entertailion.android.launcher.utils.Utils;
import com.entertailion.android.launcher.weather.WeatherSet;

/**
 * Application shared data.
 * 
 * @author leon_nicholls
 * 
 */
public class LauncherApplication extends Application {

	private static final String LOG_TAG = "LauncherApplication";

	private static final String WEATHER_LONGITUDE = "weather.longitude";
	private static final String WEATHER_LATITUDE = "weather.latitude";
	private static final String WEATHER_ZIPCODE = "weather.zipcode";
	private static final String WEATHER_COUNTRY_NAME = "weather.country_name";
	private static final String WEATHER_COUNTRY_CODE = "weather.country_code";

	private Typeface lightTypeface = null;
	private Typeface thinTypeface = null;
	private Typeface mediumTypeface = null;
	private Typeface italicTypeface = null;
	private WeatherSet weatherSet = null;
	private LocationData locationData;
	private final BroadcastReceiver applicationsReceiver = new ApplicationsIntentReceiver();
	private ArrayList<ApplicationInfo> applications;
	private ArrayList<ApplicationInfo> recents;

	@Override
	public void onCreate() {
		super.onCreate();
		locationData = Utils.getLocationData(this);
		registerIntentReceivers();
		// cache app data and icons for performance
		loadApplications();
		loadRecents();
	}

	/**
	 * Get the light typeface
	 * 
	 * @param context
	 * @return
	 */
	public Typeface getLightTypeface(Context context) {
		if (lightTypeface == null) {
			lightTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
		}
		return lightTypeface;
	}

	/**
	 * Get the thin typeface
	 * 
	 * @param context
	 * @return
	 */
	public Typeface getThinTypeface(Context context) {
		if (thinTypeface == null) {
			thinTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
		}
		return thinTypeface;
	}

	/**
	 * Get the medium typeface
	 * 
	 * @param context
	 * @return
	 */
	public Typeface getMediumTypeface(Context context) {
		if (mediumTypeface == null) {
			mediumTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Medium.ttf");
		}
		return mediumTypeface;
	}

	/**
	 * Get the italic typeface
	 * 
	 * @param context
	 * @return
	 */
	public Typeface getItalicTypeface(Context context) {
		if (italicTypeface == null) {
			italicTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Italic.ttf");
		}
		return italicTypeface;
	}

	/**
	 * Get the weather data
	 * 
	 * @return
	 */
	public WeatherSet getWeatherSet() {
		return weatherSet;
	}

	/**
	 * Set the latest weather data
	 * 
	 * @param weatherSet
	 */
	public void setWeatherSet(WeatherSet weatherSet) {
		this.weatherSet = weatherSet;
	}

	/**
	 * Get the location data.
	 * 
	 * @return
	 */
	public LocationData getLocationData() {
		if (locationData == null) {
			locationData = Utils.getLocationData(this);
		}
		if (locationData != null && locationData.getLatitude() != 0.0d && locationData.getLongitude() != 0.0d) {
			persistLocationData();
		} else {
			retrieveLocationData();
		}

		return locationData;
	}

	/**
	 * Persist the location data. After a reboot, the location provider does not
	 * always return location data immediately. Use cached location data
	 * instead.
	 */
	private void persistLocationData() {
		SharedPreferences settings = getSharedPreferences(Launcher.PREFERENCES_NAME, Activity.MODE_PRIVATE);
		try {
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(WEATHER_LONGITUDE, String.valueOf(locationData.getLongitude()));
			editor.putString(WEATHER_LATITUDE, String.valueOf(locationData.getLatitude()));
			editor.putString(WEATHER_ZIPCODE, locationData.getZipcode());
			editor.putString(WEATHER_COUNTRY_NAME, locationData.getCountryName());
			editor.putString(WEATHER_COUNTRY_CODE, locationData.getCountryCode());
			editor.commit();
		} catch (Exception e) {
			Log.d(LOG_TAG, "persistLocationData", e);
		}
	}

	/**
	 * Retrieve the persisted location data.
	 */
	private void retrieveLocationData() {
		SharedPreferences settings = getSharedPreferences(Launcher.PREFERENCES_NAME, Activity.MODE_PRIVATE);
		try {
			if (settings.getString(WEATHER_LONGITUDE, null) != null) {
				locationData = new LocationData();
				locationData.setLongitude(Double.parseDouble(settings.getString(WEATHER_LONGITUDE, null)));
				locationData.setLatitude(Double.parseDouble(settings.getString(WEATHER_LATITUDE, null)));
				locationData.setZipcode(settings.getString(WEATHER_ZIPCODE, null));
				locationData.setCountryName(settings.getString(WEATHER_COUNTRY_NAME, null));
				locationData.setCountryCode(settings.getString(WEATHER_COUNTRY_CODE, null));
			}
		} catch (Exception e) {
			Log.d(LOG_TAG, "retrieveLocationData", e);
		}
	}

	/**
	 * Get the list of Android apps installed in the system.
	 * 
	 * @return
	 */
	public ArrayList<ApplicationInfo> getApplications() {
		if (applications == null) {
			loadApplications();
			loadRecents();
		}
		return applications;
	}

	/**
	 * Get the list of recent apps invoked by the user.
	 * 
	 * @return
	 */
	public ArrayList<ApplicationInfo> getRecents() {
		if (recents == null) {
			loadApplications();
			loadRecents();
		}
		return recents;
	}

	/**
	 * Utility method to force a reload of the list of system applications.
	 */
	public void loadApplications() {
		PackageManager manager = getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

		if (apps != null) {
			Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
			final int count = apps.size();

			if (applications == null) {
				applications = new ArrayList<ApplicationInfo>(count);
			}
			applications.clear();

			for (int i = 0; i < count; i++) {
				ApplicationInfo application = new ApplicationInfo();
				ResolveInfo info = apps.get(i);

				application.setTitle(info.loadLabel(manager).toString());
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				application.setIntent(intent);
				application.setDrawable(info.activityInfo.loadIcon(manager));

				applications.add(application);
			}
		}
	}

	/**
	 * Utility method to force a reload of the recent apps.
	 */
	public void loadRecents() {
		if (recents == null) {
			recents = new ArrayList<ApplicationInfo>();
		} else {
			recents.clear();
		}

		try {
			ArrayList<ApplicationInfo> persistedRecents = RecentAppsTable.getAllRecentApps(this);
			if (persistedRecents == null || persistedRecents.size() == 0) {
				// first time, persist the system recent apps
				RecentAppsTable.persistRecents(this);
				persistedRecents = RecentAppsTable.getAllRecentApps(this);
			}

			// get persisted recent apps
			if (persistedRecents != null) {
				for (ApplicationInfo recent : persistedRecents) {
					boolean found = false;
					for (ApplicationInfo application : applications) {
						if (recent.getIntent() != null && application.getIntent() != null && recent.getIntent().getComponent() != null
								&& application.getIntent().getComponent() != null
								&& recent.getIntent().getComponent().getClassName().equals(application.getIntent().getComponent().getClassName())) {
							recent.setTitle(application.getTitle());
							recent.setDrawable(application.getDrawable());
							recents.add(recent);
							found = true;
							break;
						}
					}
					if (!found) {
						// remove recent apps that don't exist anymore
						try {
							RecentAppsTable.deleteRecentApp(this, recent.getId());
						} catch (Exception e) {
							Log.e(LOG_TAG, "loadRecents", e);
						}
					}
				}
			}

			// recents are empty immediately after a reboot and user hasn't invoked
			// any apps and launcher just installed:
			// add a default app to make the recents row visible
			if (recents.size() == 0) {
				boolean found = false;
				for (ApplicationInfo applicationInfo : applications) {
					// find the Google Play Store app
					if (applicationInfo.getIntent().getComponent().getClassName().startsWith("com.android.vending")) {
						recents.add(applicationInfo);
						found = true;
						break;
					}
				}
				if (!found) {
					// else grab the first app
					recents.add(applications.get(0));
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "loadRecents", e);
		}
	}

	/**
	 * Registers for app updates.
	 */
	private void registerIntentReceivers() {
		// Get informed about app installations
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(applicationsReceiver, filter);
	}

	/**
	 * Receive system broadcasts when applications are added/removed.
	 */
	private class ApplicationsIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action != null
					&& (Intent.ACTION_PACKAGE_CHANGED.equals(action) || Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_ADDED
							.equals(action))) {
				String packageName = intent.getData().getSchemeSpecificPart();
				boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
				Log.d(LOG_TAG, "replacing=" + replacing);
				if (!replacing && intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
					// Remove app from rows
					ArrayList<RowInfo> rows = RowsTable.getRows(context);
					if (rows != null) {
						for (RowInfo row : rows) {
							ArrayList<ItemInfo> rowItems = ItemsTable.getItems(context, row.getId());
							if (rowItems != null) {
								for (ItemInfo itemInfo : rowItems) {
									if (itemInfo instanceof ApplicationInfo) {
										try {
											if (itemInfo.getIntent().getComponent().getPackageName().equals(packageName)) {

												if (rowItems.size() == 1) {
													// TODO what if last row?
													if (rows.size() > 1) {
														ItemsTable.deleteItem(context, itemInfo.getId());
														RowsTable.deleteRow(context, row.getId());
													}
												} else {
													ItemsTable.deleteItem(context, itemInfo.getId());
												}
											}
										} catch (Exception e) {
											Log.d(LOG_TAG, "onReceive", e);
										}
									}
								}
							}
						}
					}
					// Remove app from recents
					ArrayList<ApplicationInfo> persistedRecents = RecentAppsTable.getAllRecentApps(context);
					if (persistedRecents != null) {
						for (ApplicationInfo applicationInfo : persistedRecents) {
							if (applicationInfo.getIntent().getComponent().getPackageName().equals(packageName)) {
								try {
									RecentAppsTable.deleteRecentApp(context, applicationInfo.getId());
									break;
								} catch (Exception e) {
									Log.d(LOG_TAG, "onReceive", e);
								}
							}
						}
					}
				}
				loadApplications();
				loadRecents();
			}
		}
	}
}
