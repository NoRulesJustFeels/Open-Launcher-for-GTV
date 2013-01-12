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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.entertailion.android.launcher.row.RowInfo;

/**
 * Track the rows of items (apps/channels/web apps). There can be multiple rows.
 * By default there is a recent apps row and at least one favorites row.
 * 
 * @author leon_nicholls
 * 
 */
public class RowsTable {
	private static String LOG_TAG = "RowTable";

	public static long insertRow(Context context, String title, int position, int type) throws Exception {
		Log.d(LOG_TAG, "insertRow: " + title);

		long id = DatabaseHelper.NO_ID;
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, title);
			values.put(DatabaseHelper.POSITION_COLUMN, position);
			values.put(DatabaseHelper.ROW_TYPE_COLUMN, type);
			id = db.insertOrThrow(DatabaseHelper.ROWS_TABLE, DatabaseHelper.TITLE_COLUMN, values);
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "insertRow: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "insertRow: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
		return id;
	}

	public static ArrayList<RowInfo> getRows(Context context) {
		Log.d(LOG_TAG, "getRows");
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = null;
		ArrayList<RowInfo> rows = null;
		try {
			cursor = db.rawQuery("SELECT " + DatabaseHelper.ID_COLUMN + ", " + DatabaseHelper.TITLE_COLUMN + ", " + DatabaseHelper.POSITION_COLUMN + ", "
					+ DatabaseHelper.ROW_TYPE_COLUMN + " FROM " + DatabaseHelper.ROWS_TABLE  + " ORDER BY " + DatabaseHelper.POSITION_COLUMN, null);
			if (cursor.moveToFirst()) {
				rows = new ArrayList<RowInfo>();
				do {
					RowInfo row = new RowInfo(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3));
					rows.add(row);
				} while ((cursor.moveToNext()));
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getRows failed", e);
		} finally {
			if (null != cursor)
				cursor.close();
			db.close();
		}
		return rows;
	}

	public static void updateRow(Context context, int id, String title, int position, int type) throws Exception {
		Log.d(LOG_TAG, "updateRow");

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.TITLE_COLUMN, title);
			values.put(DatabaseHelper.POSITION_COLUMN, position);
			values.put(DatabaseHelper.ROW_TYPE_COLUMN, type);
			db.update(DatabaseHelper.ROWS_TABLE, values, DatabaseHelper.ID_COLUMN + "=?", new String[] { String.valueOf(id) });
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "updateRow: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "updateRow: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
	}

	public static void deleteRow(Context context, int id) throws Exception {
		Log.d(LOG_TAG, "deleteRow");

		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(DatabaseHelper.ROWS_TABLE, DatabaseHelper.ID_COLUMN + "=?", new String[] { String.valueOf(id) });
			db.setTransactionSuccessful();
			Log.d(LOG_TAG, "deleteRow: success");
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteRow: failed", e);
			throw new Exception(e);
		} finally {
			db.endTransaction();
			db.close();
		}
	}

}
