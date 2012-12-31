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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.entertailion.android.launcher.apps.ApplicationInfo;
import com.entertailion.android.launcher.apps.VirtualAppInfo;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.shortcut.ShortcutInfo;
import com.entertailion.android.launcher.spotlight.SpotlightInfo;

/**
 * Track the items the user has configured for each row.
 * 
 * @author leon_nicholls
 * 
 */
public class ItemsTable {
	private static String LOG_TAG = "ItemsTable";

	public static long insertItem(Context context, int row, int position, String title, Intent intent, String icon, int itemType) throws Exception {
		Log.d(LOG_TAG, "insertItem");

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		long id = DatabaseHelper.NO_ID;
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, title);
			values.put(DatabaseHelper.ROW_ID_COLUMN, row);
			values.put(DatabaseHelper.POSITION_COLUMN, position);
			if (intent != null) {
				values.put(DatabaseHelper.INTENT_COLUMN, intent.toUri(Intent.URI_INTENT_SCHEME));
			}
			values.put(DatabaseHelper.ICON_COLUMN, icon);
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, itemType);
			id = db.insertOrThrow(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "insertItem: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "insertItem: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
		return id;
	}

	public static ArrayList<ItemInfo> getItems(Context context, int row) {
		Log.d(LOG_TAG, "getItems: " + row);
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = null;
		ArrayList<ItemInfo> items = null;
		try {
			cursor = db.rawQuery("SELECT " + DatabaseHelper.ID_COLUMN + ", " + DatabaseHelper.POSITION_COLUMN + ", " + DatabaseHelper.TITLE_COLUMN + ", "
					+ DatabaseHelper.INTENT_COLUMN + ", " + DatabaseHelper.ICON_COLUMN + ", " + DatabaseHelper.ITEM_TYPE_COLUMN + " FROM "
					+ DatabaseHelper.ITEMS_TABLE + " WHERE " + DatabaseHelper.ROW_ID_COLUMN + "=" + row + " ORDER BY " + DatabaseHelper.POSITION_COLUMN, null);
			if (cursor.moveToFirst()) {
				items = new ArrayList<ItemInfo>();
				do {
					int position = cursor.getInt(1);
					Intent intent = null;
					String intentValue = cursor.getString(3);
					if (intentValue != null) {
						intent = Intent.parseUri(cursor.getString(3), Intent.URI_INTENT_SCHEME);
					}
					String title = cursor.getString(2);
					int type = cursor.getInt(5);
					String icon = cursor.getString(4);
					ItemInfo itemInfo = null;
					switch (type) {
					case DatabaseHelper.APP_TYPE:
						itemInfo = new ApplicationInfo(cursor.getInt(0), position, title, intent);
						break;
					case DatabaseHelper.SPOTLIGHT_TYPE:
						itemInfo = new SpotlightInfo(cursor.getInt(0), position, title, intent, null, icon);
						break;
					case DatabaseHelper.SHORTCUT_TYPE:
						itemInfo = new ShortcutInfo(cursor.getInt(0), position, title, intent, icon);
						break;
					default:
						if (type >= DatabaseHelper.VIRTUAL_APP_TYPE) {
							itemInfo = new VirtualAppInfo(cursor.getInt(0), position, title, type);
						}
					}
					items.add(itemInfo);
				} while ((cursor.moveToNext()));
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getItems failed", e);
		} finally {
			if (null != cursor)
				cursor.close();
			db.close();
		}
		return items;
	}

	public static void updateItem(Context context, int id, int row, int position, String title, Intent intent, String icon, int itemType) throws Exception {
		Log.d(LOG_TAG, "updateItem");

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, title);
			values.put(DatabaseHelper.ROW_ID_COLUMN, row);
			values.put(DatabaseHelper.POSITION_COLUMN, position);
			if (intent != null) {
				values.put(DatabaseHelper.INTENT_COLUMN, intent.toUri(Intent.URI_INTENT_SCHEME));
			}
			values.put(DatabaseHelper.ICON_COLUMN, icon);
			values.put(DatabaseHelper.ITEM_TYPE_COLUMN, itemType);
			db.update(DatabaseHelper.ITEMS_TABLE, values, DatabaseHelper.ID_COLUMN + "=?", new String[] { String.valueOf(id) });
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "updateItem: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "updateItem: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
	}

	public static void deleteItem(Context context, int id) throws Exception {
		Log.d(LOG_TAG, "deleteItem");

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(DatabaseHelper.ITEMS_TABLE, DatabaseHelper.ID_COLUMN + "=?", new String[] { String.valueOf(id) });
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "deleteItem: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteItem: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
	}

}
