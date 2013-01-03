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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver to launch service after device boots up
 * 
 * @see manifest: android.intent.action.BOOT_COMPLETED
 * 
 * @author leon_nicholls
 * 
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String LOG_CAT = "BootReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_CAT, "bootReciever.onReciever");
		try {
			context.startService(new Intent(context, LauncherService.class));
		} catch (Exception e) {
			Log.e(LOG_CAT, "onReceive", e);
		}
	}
}