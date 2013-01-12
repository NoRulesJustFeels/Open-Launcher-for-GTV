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
package com.entertailion.android.launcher.spotlight;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.entertailion.android.launcher.database.SpotlightTable;
import com.entertailion.android.launcher.utils.HttpRequestHelper;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Extract the Spotlight web app data from the Spotlight JSON feed.
 * 
 * @author leon_nicholls
 * 
 */
public class ProcessSpotlight implements Runnable {
	private static final String LOG_TAG = "ProcessSpotlight";
	public static String SPOTLIGHT_FEED_URL = "http://www.google.com/tv/static/js/spotlight_sites.js"; // https://www.google.com/tv/spotlight-gallery.html

	private Context context = null;

	public ProcessSpotlight(Context context) {
		this.context = context;
	}

	/**
	 * Get the latest spotlight data from the Google TV web site
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String getFeed() throws Exception {
		StringBuilder builder = new StringBuilder();
		InputStream stream = new HttpRequestHelper().getHttpStream(SPOTLIGHT_FEED_URL);
		if (stream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			String result = builder.toString();
			if (result.startsWith("var sites = ")) {
				result = result.replace("var sites = ", "");
			}
			return result;
		}
		return null;
	}

	/**
	 * Get the spotlight data from the asset file
	 * 
	 * @param context
	 * @return
	 */
	public static String getAssetFeed(Context context) {
		String result = null;
		try {
			StringBuffer buffer = new StringBuffer();
			InputStream is = context.getAssets().open("spotlight_sites.js");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			is.close();
			result = buffer.toString();
		} catch (Throwable x) {
		}
		return result;
	}

	/**
	 * Process the spotlight JSON data
	 * 
	 * @param jsonFeed
	 * @return
	 * @throws JSONException
	 */
	public static List<SpotlightInfo> process(Context context, String jsonFeed) throws JSONException {
		JSONObject jsonObj = new JSONObject(jsonFeed);
		List<SpotlightInfo> result = null;
		if (null != jsonObj) {
			JSONArray models = (JSONArray) jsonObj.get("models");
			if (null != models && models.length() > 0) {
				result = new ArrayList<SpotlightInfo>();
				int position = 0;
				for (int i = 0; i < models.length(); i++) {
					JSONObject model = (JSONObject) models.get(i);
					String title = model.getString("title");
					String url = model.getString("url");
					String logo = model.getString("hdpiLogo");
					String icon = null;
					if (null == SpotlightInfo.spotlightIconMap.get(title.toLowerCase())) {
						// icon file path
						try {
							icon = "spotlight_" + sanitizeName(title) + ".png";
							File file = context.getFileStreamPath(icon);
							if (!file.exists()) {
								Uri uri = Uri.parse(url);
								String alternateLogo = Utils.getWebSiteIcon(context, uri.getScheme()+"://"+uri.getHost());
								if (alternateLogo!=null && alternateLogo.trim().length()>0) {
									icon = alternateLogo;
								} else {
									// create a file-based icon from original
									// 360x203
									Bitmap bitmap = Utils.getBitmapFromURL(logo);
									if (bitmap!=null) {
										bitmap = Utils.crop(bitmap, 30, 30);
										Utils.saveToFile(context, bitmap, 100, 100, icon);
										bitmap.recycle();
										bitmap = null;
									} else {
										icon = null;
									}
								}
							}
						} catch (Exception e) {
							Log.e(LOG_TAG, "create spotlight icon", e);
						}
					}
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					result.add(new SpotlightInfo(position++, title, browserIntent, logo, icon));
				}
			}
		}
		return result;
	}

	public static void persistFeed(Context context, List<SpotlightInfo> spotlights) {
		try {
			SpotlightTable.insertSpotlights(context, spotlights);
		} catch (Exception e) {
			Log.e(LOG_TAG, "persistFeed", e);
		}
	}

	@Override
	public void run() {
		String jsonFeed = null;
		try {
			// attempt to get feed from internet
			jsonFeed = getFeed();
		} catch (Exception e) {
			Log.e(LOG_TAG, "getFeed", e);
		}
		try {
			Log.i(LOG_TAG, "Begin fetching launcher spotlight data" + System.currentTimeMillis() + "]");
			if (null != jsonFeed && jsonFeed.trim().length() > 0) {
				List<SpotlightInfo> spotlights = process(context, jsonFeed);
				// persist the latest data
				persistFeed(context, spotlights);
			}
			Log.i(LOG_TAG, "End fetching launcher spotlight data" + System.currentTimeMillis() + "]");
		} catch (Exception e) {
			Log.e(LOG_TAG, "run", e);
		}
	}

	/**
	 * Clean the name of special characters so it can be used for a filename
	 * 
	 * @param name
	 * @return
	 */
	private static String sanitizeName(String name) {
		String result = name.replaceAll("\\s+", "_");
		result = result.replaceAll("[^a-zA-Z0-9]", "");
		return result;
	}

}
