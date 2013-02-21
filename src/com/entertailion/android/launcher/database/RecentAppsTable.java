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

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.entertailion.android.launcher.apps.ApplicationInfo;

/**
 * Persist the recent apps. With a restart the system resets its recents items.
 * This table will remember the recent apps across a reboot.
 * 
 * @author leon_nicholls
 * 
 */
public class RecentAppsTable {
	private static String LOG_TAG = "RecentAppsTable";

	public static final int MAX_RECENT_TASKS = 10;

	public static long insertRecentApp(Context context, Intent intent) throws Exception {
		Log.d(LOG_TAG, "insertRecentApp: " + intent);

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		long id = DatabaseHelper.NO_ID;
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.INTENT_COLUMN, intent.toUri(Intent.URI_INTENT_SCHEME));
			id = db.insertOrThrow(DatabaseHelper.RECENT_APPS_TABLE, DatabaseHelper.INTENT_COLUMN, values);
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "insertRecentApp: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "insertRecentApp: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
		return id;
	}

	public static ArrayList<ApplicationInfo> getAllRecentApps(Context context) {
		Log.d(LOG_TAG, "getAllRecentApps");
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = null;
		ArrayList<ApplicationInfo> recents = null;
		try {
			cursor = db.rawQuery("SELECT " + DatabaseHelper.ID_COLUMN + ", " + DatabaseHelper.INTENT_COLUMN + " FROM " + DatabaseHelper.RECENT_APPS_TABLE
					+ " ORDER BY " + DatabaseHelper.ID_COLUMN + " ASC", null);
			if (cursor.moveToFirst()) {
				recents = new ArrayList<ApplicationInfo>();
				do {
					Intent intent = null;
					String intentValue = cursor.getString(1);
					if (intentValue != null) {
						intent = Intent.parseUri(cursor.getString(1), Intent.URI_INTENT_SCHEME);
					}
					ApplicationInfo recent = new ApplicationInfo(cursor.getInt(0), 0, null, intent);
					recents.add(recent);
				} while ((cursor.moveToNext()));
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getAllRecentApps failed", e);
		} finally {
			if (null != cursor)
				cursor.close();
			db.close();
		}
		return recents;
	}

	public static void removeAllRecentApps(Context context) {
		Log.d(LOG_TAG, "removeAllRecentApps");
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.delete(DatabaseHelper.RECENT_APPS_TABLE, null, null);
		} catch (Exception e) {
			Log.e(LOG_TAG, "removeAllRecentApps failed", e);
		} finally {
			db.close();
		}
	}

	/**
	 * Persist the list of recent apps. The system looses track of recent apps
	 * across a reboot. Use the DB to store the last known recent apps.
	 */
	public static void persistRecents(Context context) {
		Log.d(LOG_TAG, "persistRecents");
		final ActivityManager tasksManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		final List<ActivityManager.RecentTaskInfo> recentTasks = tasksManager.getRecentTasks(MAX_RECENT_TASKS, 0);

		// clear the table
		removeAllRecentApps(context);

		// add latest recent apps
		int count = recentTasks.size();
		for (int i = count - 1; i >= 0; i--) {
			final Intent intent = recentTasks.get(i).baseIntent;

			if (Intent.ACTION_MAIN.equals(intent.getAction()) && !intent.hasCategory(Intent.CATEGORY_HOME)) {
				try {
					insertRecentApp(context, intent);
				} catch (Exception e) {
					Log.e(LOG_TAG, "persistRecents", e);
				}
			}
		}
	}

	public static void persistRecentApp(Context context, ApplicationInfo applicationInfo) {
		Log.d(LOG_TAG, "persistRecentApp");

		// Persist the app if it isn't already in the recents list
		ArrayList<ApplicationInfo> recents = getAllRecentApps(context);
		boolean found = false;
		if (recents != null) {
			for (ApplicationInfo recentApplicationInfo : recents) {
				if (recentApplicationInfo.getIntent() != null && applicationInfo.getIntent() != null
						&& recentApplicationInfo.getIntent().getComponent() != null && applicationInfo.getIntent().getComponent() != null
						&& recentApplicationInfo.getIntent().getComponent().getClassName().equals(applicationInfo.getIntent().getComponent().getClassName())) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			try {
				insertRecentApp(context, applicationInfo.getIntent());
			} catch (Exception e) {
				Log.e(LOG_TAG, "persistRecentApp", e);
			}
		}
		// trim the list of recents
		while (recents != null && recents.size() >= MAX_RECENT_TASKS) {
			Log.d(LOG_TAG, "size="+recents.size());
			try {
				deleteRecentApp(context, recents.get(0).getId());
			} catch (Exception e) {
				Log.e(LOG_TAG, "persistRecentApp", e);
			}
			recents = getAllRecentApps(context);
		}
	}

	public static void deleteRecentApp(Context context, int id) throws Exception {
		Log.d(LOG_TAG, "deleteRecentApp: "+id);

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			int num = db.delete(DatabaseHelper.RECENT_APPS_TABLE, DatabaseHelper.ID_COLUMN + "=?", new String[] { String.valueOf(id) });
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "deleteRecentApp: success: "+num);
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteRecentApp: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
	}

}
