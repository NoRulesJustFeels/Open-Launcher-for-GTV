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

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.row.RowInfo;
import com.entertailion.android.launcher.spotlight.ProcessSpotlight;
import com.entertailion.android.launcher.spotlight.SpotlightInfo;

/**
 * Database management utility.
 * 
 * The user interface consists of rows of items (apps/channels/web apps). Each
 * item has at least a title, icon and an intent/url that is invoked when the
 * user selects the item. There is one recent apps row and the rest are favorite
 * rows customized by the user. By default a favorite row is created with some
 * default items. Spotlight web apps
 * (http://www.google.com/tv/static/js/spotlight_sites.js) data are downloaded
 * and cached in the db. Virtual apps are created for notifications, bookmarks,
 * all apps and all spotlight web apps.
 * 
 * @author leon_nicholls
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {
	private static String LOG_TAG = "DatabaseHelper";

	public static final int NO_ID = -1;
	public static final int APP_TYPE = 1;
	public static final int SPOTLIGHT_TYPE = 2;
	public static final int SHORTCUT_TYPE = 3;
	public static final int VIRTUAL_APP_TYPE = 1000000;
	public static final int VIRTUAL_NOTIFICATIONS_TYPE = VIRTUAL_APP_TYPE + 0;
	public static final int VIRTUAL_ALL_APPS_TYPE = VIRTUAL_APP_TYPE + 1;
	public static final int VIRTUAL_BROWSER_BOOKMARKS_TYPE = VIRTUAL_APP_TYPE + 2;
	public static final int VIRTUAL_SPOTLIGHT_WEB_APPS_TYPE = VIRTUAL_APP_TYPE + 3;
	public static final int VIRTUAL_LIVE_TV_TYPE = VIRTUAL_APP_TYPE + 4;
	public static final int VIRTUAL_BROWSER_HISTORY_TYPE = VIRTUAL_APP_TYPE + 5;

	private Context context;

	public final static int CURRENT_DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "launcher.db";

	// Row table
	public final static String ROWS_TABLE = "rows";
	public final static String TITLE_COLUMN = "title";
	public final static String ROW_TYPE_COLUMN = "rowType";

	// Items table
	public final static String ITEMS_TABLE = "items";
	public final static String ID_COLUMN = "_id";
	public final static String ROW_ID_COLUMN = "rowId";
	public final static String POSITION_COLUMN = "position";
	public final static String INTENT_COLUMN = "intent";
	public final static String ITEM_TYPE_COLUMN = "itemType";
	public final static String ICON_COLUMN = "icon"; // file path

	// Spotlight table
	public final static String SPOTLIGHT_TABLE = "spotlight";
	public final static String URL_COLUMN = "url";
	public final static String LOGO_COLUMN = "logo";

	// Recent apps table
	public final static String RECENT_APPS_TABLE = "recent_apps";

	// Track id of default items row
	private long itemsRowId;

	/**
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */
	public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		this.context = context;
	}

	/**
	 * @param context
	 */
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, CURRENT_DATABASE_VERSION);
		this.context = context;
	}

	/**
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(LOG_TAG, "create database");
		// create tables
		createRowsTable(db);
		populateRowsTable(db);
		createItemsTable(db);
		populateItemsTable(db);
		createSpotlightTable(db);
		populateSpotlightTable(db);
		createRecentAppsTable(db);
	}

	/**
	 * Delete database
	 */
	private void deleteDatabase() {
		context.deleteDatabase(DATABASE_NAME);
	}

	/**
	 * Create row table
	 * 
	 * @param db
	 */
	private void createRowsTable(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + ROWS_TABLE + " (" + ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TITLE_COLUMN + " STRING, "
				+ POSITION_COLUMN + " INTEGER, " + ROW_TYPE_COLUMN + " INTEGER" + ");";

		db.execSQL(TABLE_CREATE);
		Log.i(LOG_TAG, ROWS_TABLE + " table was created successfully");
	}

	/**
	 * Initialize the row table with default row
	 */
	private void populateRowsTable(SQLiteDatabase db) {
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.layer_favorites));
			values.put(DatabaseHelper.POSITION_COLUMN, 0);
			values.put(DatabaseHelper.ROW_TYPE_COLUMN, RowInfo.FAVORITE_TYPE);
			itemsRowId = db.insertOrThrow(DatabaseHelper.ROWS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateRowsTable", e);
		}
	}

	/**
	 * Create items table
	 * 
	 * @param db
	 */
	private void createItemsTable(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + ITEMS_TABLE + " (" + ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ROW_ID_COLUMN + " INTEGER, "
				+ POSITION_COLUMN + " INTEGER, " + TITLE_COLUMN + " STRING, " + INTENT_COLUMN + " STRING, " + ITEM_TYPE_COLUMN + " INTEGER, " + ICON_COLUMN
				+ " STRING " + ");";

		db.execSQL(TABLE_CREATE);
		Log.i(LOG_TAG, ITEMS_TABLE + " table was created successfully");
	}

	/**
	 * Initialize the items table with default apps
	 */
	private void populateItemsTable(SQLiteDatabase db) {
		// NOTE: remember to update FavoriteTable.getAllFavorites
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.ROW_ID_COLUMN, itemsRowId);
			values.put(DatabaseHelper.POSITION_COLUMN, 0);
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.notifications));
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, VIRTUAL_NOTIFICATIONS_TYPE);
			db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateItemsTable", e);
		}
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.ROW_ID_COLUMN, itemsRowId);
			values.put(DatabaseHelper.POSITION_COLUMN, 1);
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.live_tv));
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, VIRTUAL_LIVE_TV_TYPE);
			db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateItemsTable", e);
		}
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.ROW_ID_COLUMN, itemsRowId);
			values.put(DatabaseHelper.POSITION_COLUMN, 2);
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.all_apps));
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, VIRTUAL_ALL_APPS_TYPE);
			db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateItemsTable", e);
		}
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.ROW_ID_COLUMN, itemsRowId);
			values.put(DatabaseHelper.POSITION_COLUMN, 3);
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.bookmarks));
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, VIRTUAL_BROWSER_BOOKMARKS_TYPE);
			db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateItemsTable", e);
		}
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.ROW_ID_COLUMN, itemsRowId);
			values.put(DatabaseHelper.POSITION_COLUMN, 4);
			values.put(DatabaseHelper.TITLE_COLUMN, context.getString(R.string.spotlight_web_apps));
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, VIRTUAL_SPOTLIGHT_WEB_APPS_TYPE);
			db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateItemsTable", e);
		}
	}

	/**
	 * Create spotlight table
	 * 
	 * @param db
	 */
	private void createSpotlightTable(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + SPOTLIGHT_TABLE + " (" + ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TITLE_COLUMN + " STRING, "
				+ URL_COLUMN + " STRING, " + LOGO_COLUMN + " STRING, " + ICON_COLUMN + " STRING " + ");";

		db.execSQL(TABLE_CREATE);
		Log.i(LOG_TAG, SPOTLIGHT_TABLE + " table was created successfully");
	}

	/**
	 * Initialize the spotlight table with data from embedded asset file
	 */
	private void populateSpotlightTable(SQLiteDatabase db) {
		String jsonFeed = ProcessSpotlight.getAssetFeed(context);
		try {
			if (null != jsonFeed && jsonFeed.trim().length() > 0) {
				List<SpotlightInfo> spotlights = ProcessSpotlight.process(context, jsonFeed);
				for (SpotlightInfo spotlight : spotlights) {
					ContentValues values = new ContentValues();
					values.put(DatabaseHelper.TITLE_COLUMN, spotlight.getTitle());
					values.put(DatabaseHelper.URL_COLUMN, spotlight.getIntent().getDataString());
					values.put(DatabaseHelper.LOGO_COLUMN, spotlight.getLogo());
					values.put(DatabaseHelper.ICON_COLUMN, spotlight.getIcon());
					db.insertOrThrow(DatabaseHelper.SPOTLIGHT_TABLE, DatabaseHelper.TITLE_COLUMN, values);
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "populateSpotlightTable", e);
		}
	}

	/**
	 * Create recent apps table
	 * 
	 * @param db
	 */
	private void createRecentAppsTable(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + RECENT_APPS_TABLE + " (" + ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " + INTENT_COLUMN + " STRING "
				+ ");";

		db.execSQL(TABLE_CREATE);
		Log.i(LOG_TAG, RECENT_APPS_TABLE + " table was created successfully");
	}

	/**
	 * Upgrade database
	 * 
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
	 *      int, int)
	 */
	@Override
	public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == newVersion) {
			Log.i(LOG_TAG, "Database upgrade not needed");
			return;
		}
		Log.i(LOG_TAG, "Database needs to be upgraded from version " + oldVersion + " to version " + newVersion);
		boolean success = doUpgrade(db, oldVersion, newVersion);
		if (success) {
			Log.i(LOG_TAG, "Database was updated from version " + oldVersion + " to version " + newVersion);
		} else {
			Log.w(LOG_TAG, "Database was NOT updated from version " + oldVersion + " to version " + newVersion);
			rebuildTables(db);
		}
	}

	/**
	 * Rebuild tables
	 * 
	 * @param db
	 */
	private void rebuildTables(final SQLiteDatabase db) {
		// something very bad happened...
		deleteDatabase();
		System.exit(1);
	}

	/**
	 * Upgrade database tables
	 * 
	 * @param db
	 * @param oldVersion
	 * @param newVersion
	 * @return
	 */
	private boolean doUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(LOG_TAG, "doUpgrade: " + oldVersion + " to " + newVersion);
		boolean success = true;
		try {
			db.beginTransaction();
			int currentVersion = oldVersion;
			for (int i = currentVersion; i < newVersion; i++) {
				if (currentVersion == 1) {
					upgradeFrom1To2(db);
				}
				currentVersion++;
			}
			db.setVersion(newVersion);
			db.setTransactionSuccessful();
			Log.i(LOG_TAG, "Database upgrade was successful");
		} catch (Exception e) {
			Log.e(LOG_TAG, "Database upgrade was NOT successful", e);
			success = false;
		} finally {
			db.endTransaction();
		}
		return success;
	}

	/**
	 * Clear database tables
	 * 
	 * @param context
	 * @return
	 */
	public static boolean clearTables(Context context) {
		boolean success = true;
		Log.i(LOG_TAG, "Clearing all databases");
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.beginTransaction();

			Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
			if (cursor.moveToFirst()) {
				String tableName = cursor.getString(0);
				Log.d(LOG_TAG, "clearing " + tableName);
				db.delete(tableName, null, null);
			}
			if (null != cursor)
				cursor.close();
			Log.i(LOG_TAG, "Databased tables cleared");
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(LOG_TAG, "Database tables NOT cleared", e);
			success = false;
		} finally {
			db.endTransaction();
			db.close();
		}
		return success;
	}

	/**
	 * Upgrade database from version 1 to version 2
	 * 
	 * @param db
	 */
	private void upgradeFrom1To2(SQLiteDatabase db) {
		// TODO future versions...
	}

}
