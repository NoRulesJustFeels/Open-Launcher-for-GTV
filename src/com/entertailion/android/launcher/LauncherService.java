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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.entertailion.android.launcher.spotlight.SpotlightReceiver;
import com.entertailion.android.launcher.utils.Utils;
import com.entertailion.android.launcher.weather.WeatherReceiver;

/**
 * Launcher service to do background tasks
 * 
 * @author leon_nicholls
 * 
 */
public class LauncherService extends Service {

	private static final String LOG_TAG = "LauncherService";

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		LauncherService getService() {
			return LauncherService.this;
		}
	}

	private final IBinder binder = new LocalBinder();

	@Override
	public void onCreate() {
		super.onCreate();

		// get spotlight data
		Log.d(LOG_TAG, "starting alarms...");
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		PendingIntent spotlightIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, SpotlightReceiver.class), 0);
		// start immediately, then twice daily
		am.setRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_HALF_DAY, spotlightIntent);

		// free weather data only supported for USA
		if (Utils.isUsa()) {
			PendingIntent weatherIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, WeatherReceiver.class), 0);
			// start immediately, then half hour
			am.setRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_HALF_HOUR, weatherIntent);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

}