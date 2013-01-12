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
package com.entertailion.android.launcher.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Bundle;

/**
 * Invoke the system live wallpaper manager chooser.
 * 
 * @author leon_nicholls
 * 
 */
public class LiveWallpaperChooser extends Activity {
	private static final int REQUEST_CODE = 1;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Intent intent = new Intent();
		intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
		startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == REQUEST_CODE) {
			finish();
		}
	}
}
