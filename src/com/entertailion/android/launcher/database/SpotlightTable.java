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
package com.entertailion.android.launcher.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.entertailion.android.launcher.spotlight.SpotlightInfo;

/**
 * Cache the spotlight web apps:
 * https://www.google.com/tv/spotlight-gallery.html. The data
 * (http://www.google.com/tv/static/js/spotlight_sites.js) is downloaded
 * regularly by the alarm manager configured by the LauncherService.
 * 
 * @author leon_nicholls
 * 
 */
public class SpotlightTable {
	private static String LOG_TAG = "SpotlightTable";

	public static long insertSpotlight(Context context, String title, String url, String logo, String icon) throws Exception {
		Log.d(LOG_TAG, "insertSpotlight: " + title + ", " + url + ", " + logo + ", " + icon);

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		long id = DatabaseHelper.NO_ID;
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, title);
			values.put(DatabaseHelper.URL_COLUMN, url);
			values.put(DatabaseHelper.LOGO_COLUMN, logo);
			values.put(DatabaseHelper.ICON_COLUMN, icon);
			id = db.insertOrThrow(DatabaseHelper.SPOTLIGHT_TABLE, DatabaseHelper.TITLE_COLUMN, values);
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "insertSpotlight: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "insertSpotlight: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
		return id;
	}

	public static void insertSpotlights(Context context, List<SpotlightInfo> spotlights) throws Exception {
		if (spotlights != null) {
			Log.d(LOG_TAG, "insertSpotlights: " + spotlights.size());

			DatabaseHelper databaseHelper = new DatabaseHelper(context);
			SQLiteDatabase db = databaseHelper.getWritableDatabase();
			db.beginTransaction();
			try {
				// clear existing table first
				db.delete(DatabaseHelper.SPOTLIGHT_TABLE, null, null);
				// add new data
				for (SpotlightInfo spotlight : spotlights) {
					ContentValues values = new ContentValues();
					values.put(DatabaseHelper.TITLE_COLUMN, spotlight.getTitle());
					values.put(DatabaseHelper.URL_COLUMN, spotlight.getIntent().getDataString());
					values.put(DatabaseHelper.LOGO_COLUMN, spotlight.getLogo());
					values.put(DatabaseHelper.ICON_COLUMN, spotlight.getIcon());
					db.insertOrThrow(DatabaseHelper.SPOTLIGHT_TABLE, DatabaseHelper.TITLE_COLUMN, values);
				}
				db.setTransactionSuccessful();
				Log.d(LOG_TAG, "insertSpotlights: success");
			} catch (Exception e) {
				Log.e(LOG_TAG, "insertSpotlights: failed", e);
				throw new Exception(e);
			} finally {
				db.endTransaction();
				db.close();
			}
		}
		Log.d(LOG_TAG, "end insertSpotlights");
	}

	public static ArrayList<SpotlightInfo> getAllSpotlights(Context context) {
		Log.d(LOG_TAG, "getAllSpotlights");
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = null;
		ArrayList<SpotlightInfo> spotlights = null;
		try {
			cursor = db.rawQuery("SELECT " + DatabaseHelper.ID_COLUMN + ", " + DatabaseHelper.TITLE_COLUMN + ", " + DatabaseHelper.URL_COLUMN + ", "
					+ DatabaseHelper.LOGO_COLUMN + ", " + DatabaseHelper.ICON_COLUMN + " FROM " + DatabaseHelper.SPOTLIGHT_TABLE, null);
			if (cursor.moveToFirst()) {
				spotlights = new ArrayList<SpotlightInfo>();
				do {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cursor.getString(2)));
					SpotlightInfo spotlight = new SpotlightInfo(cursor.getInt(0), cursor.getString(1), browserIntent, cursor.getString(3), cursor.getString(4));
					spotlights.add(spotlight);
				} while ((cursor.moveToNext()));
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getAllSpotlights failed", e);
		} finally {
			if (null != cursor)
				cursor.close();
			db.close();
		}
		Log.d(LOG_TAG, "end getAllSpotlights");
		return spotlights;
	}

}
