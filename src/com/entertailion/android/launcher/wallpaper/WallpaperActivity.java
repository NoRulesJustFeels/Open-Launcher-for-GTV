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

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;

/**
 * Let the user select how to set the system wallpaper. Use can pick from static
 * images part of the app, use the Photos app or set live wallpapers.
 * 
 * @author leon_nicholls
 * 
 */
public class WallpaperActivity extends Activity {
	private static final String LOG_TAG = "WallpaperActivity";

	public static final String FIRST_PHOTOS = "first_photos";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.wallpaper);

		final PackageManager packageManager = getPackageManager();

		final ArrayList<WallpaperInfo> wallpapers = new ArrayList<WallpaperInfo>();
		wallpapers
				.add(new WallpaperInfo(WallpaperInfo.WALLPAPER_TYPE, getString(R.string.wallpapers), getResources().getDrawable(R.drawable.wallpapers)));

		Drawable photosDrawable = null;
		try {
			// Get the photos icon from the Photos app
			photosDrawable = packageManager.getApplicationIcon("com.google.tv.mediabrowser");
			wallpapers.add(new WallpaperInfo(WallpaperInfo.PHOTOS_TYPE, getString(R.string.photos), photosDrawable));
		} catch (NameNotFoundException e) {
			Log.e(LOG_TAG, "getApplicationIcon", e);
		}

		Drawable liveWallpaperDrawable = getLiveWallpaperPickerDrawable(packageManager);
		if (liveWallpaperDrawable == null) {
			try {
				liveWallpaperDrawable = packageManager.getApplicationIcon("com.android.wallpaper.livepicker");
				wallpapers.add(new WallpaperInfo(WallpaperInfo.LIVE_WALLPAPER_TYPE, getString(R.string.live_wallpapers), liveWallpaperDrawable));
			} catch (NameNotFoundException e) {
				Log.e(LOG_TAG, "getApplicationIcon", e);
			}
		} else {
			wallpapers.add(new WallpaperInfo(WallpaperInfo.LIVE_WALLPAPER_TYPE, getString(R.string.live_wallpapers), liveWallpaperDrawable));
		}

		final GridView gridView = (GridView) findViewById(R.id.grid);
		gridView.setAdapter(new WallpaperGalleryImageAdapter(this, wallpapers));
		gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				WallpaperInfo wallpaperInfo = (WallpaperInfo) wallpapers.get(position);
				switch (wallpaperInfo.getType()) {
				case WallpaperInfo.WALLPAPER_TYPE:
					Intent staticIntent = new Intent(WallpaperActivity.this, StaticWallpaperChooser.class);
					startActivity(staticIntent);
					finish();
					break;
				case WallpaperInfo.PHOTOS_TYPE:
					// There isn't a default gallery app installed on Google TV so
					// Intent.ACTION_PICK will not work
					// Instead just launch the Photos app and let the user
					// manually set the wallpaper using the Photos menu option

					// For first time show a dialog with some user instructions
					SharedPreferences settings = getSharedPreferences(Launcher.PREFERENCES_NAME, Activity.MODE_PRIVATE);
					boolean firstPhotos = settings.getBoolean(FIRST_PHOTOS, true);
					if (firstPhotos) {
						try {
							displayPhotosInstructions(WallpaperActivity.this, packageManager);

							// persist not to show instructions again
							SharedPreferences.Editor editor = settings.edit();
							editor.putBoolean(FIRST_PHOTOS, false);
							editor.commit();
						} catch (Exception e) {
							Log.d(LOG_TAG, "first photos", e);
						}
					} else {
						launchPhotosApp(packageManager);
						finish();
					}

					break;
				case WallpaperInfo.LIVE_WALLPAPER_TYPE:
					Intent liveIntent = new Intent(WallpaperActivity.this, LiveWallpaperChooser.class);
					startActivity(liveIntent);
					finish();
					break;
				}
			}

		});
	}

	/**
	 * Get the icon for the live wallpaper preview icon from the live wallpaper
	 * app manifest meta-data.
	 * 
	 * @param packageManager
	 * @return
	 */
	private Drawable getLiveWallpaperPickerDrawable(PackageManager packageManager) {
		Drawable liveWallpaperDrawable = null;
		Intent mainIntent = new Intent("android.service.wallpaper.LIVE_WALLPAPER_CHOOSER", null);
		mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
		final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);
		for (ResolveInfo info : apps) {
			if (info.activityInfo.applicationInfo.packageName.equals("com.android.wallpaper.livepicker")) {
				Bundle metaData = info.activityInfo.metaData;
				if (metaData != null) {
					XmlResourceParser xpp = info.activityInfo.loadXmlMetaData(packageManager, "android.wallpaper.preview");
					try {
						xpp.next();
						int eventType = xpp.getEventType();
						while (eventType != XmlPullParser.END_DOCUMENT) {
							if (eventType == XmlPullParser.START_TAG) {
								if (xpp.getName().equals("wallpaper-preview")) {
									try {
										int resource = xpp
												.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "staticWallpaperPreview", -1);
										if (resource != -1) {
											liveWallpaperDrawable = packageManager.getResourcesForApplication(info.activityInfo.applicationInfo).getDrawable(
													resource);
											break;
										}
									} catch (Exception e) {
										Log.e(LOG_TAG, "getResourcesForApplication", e);
									}
								}
							}
							eventType = xpp.next();
						}
					} catch (Exception e) {
						Log.e(LOG_TAG, "XmlResourceParser", e);
					}
				} else {
					Log.d(LOG_TAG, "metaData is null");
				}
				break;
			}
		}
		return liveWallpaperDrawable;
	}

	/**
	 * Tell the user how to use the Photos app to set the wallpaper. Do this
	 * once only.
	 * 
	 * @param context
	 * @param packageManager
	 */
	private void displayPhotosInstructions(final Activity context, final PackageManager packageManager) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.alert);

		final TextView alertTextView = (TextView) dialog.findViewById(R.id.alertText);
		alertTextView.setText(getString(R.string.wallpaper_photos_instructions));
		Button alertButton = (Button) dialog.findViewById(R.id.alertButton);
		alertButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
				context.finish();
				launchPhotosApp(packageManager);
			}

		});
		dialog.show();
	}

	/**
	 * Launch the Photos app so the user can set the wallpaper manually.
	 * 
	 * @param packageManager
	 */
	private void launchPhotosApp(PackageManager packageManager) {
		try {
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
			for (ResolveInfo info : apps) {
				if (info.activityInfo.applicationInfo.packageName.equals("com.google.tv.mediabrowser")) {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					startActivity(intent);
					break;
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "com.google.tv.mediabrowser missing", e);
		}
	}

}