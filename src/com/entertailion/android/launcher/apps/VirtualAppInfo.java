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
package com.entertailion.android.launcher.apps;

import java.util.ArrayList;

import android.content.Context;
import android.widget.ImageView;

import com.entertailion.android.launcher.Dialogs;
import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.LauncherApplication;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Represent virtual apps like notifications. These aren't real Android apps
 * installed on the system. They are used to provide access to various features
 * in a manner consistent with other apps. Virtual apps appear in rows like
 * other native Android apps.
 * 
 * @author leon_nicholls
 * 
 */
public class VirtualAppInfo extends ItemInfo {

	private int type;

	public VirtualAppInfo() {
		super();
	}

	public VirtualAppInfo(int id, int position, String title, int type) {
		super(id, position, title, null);
		this.type = type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	@Override
	public void invoke(Launcher context) {
		switch (type) {
		case DatabaseHelper.VIRTUAL_NOTIFICATIONS_TYPE:
			Utils.showNotifications(context);
			Analytics.logEvent(Analytics.INVOKE_NOTIFICATIONS);
			return;
		case DatabaseHelper.VIRTUAL_BROWSER_BOOKMARKS_TYPE:
			Dialogs.displayBookmarks(context);
			return;
		case DatabaseHelper.VIRTUAL_BROWSER_HISTORY_TYPE:
			Dialogs.displayBrowserHistory(context);
			return;
		case DatabaseHelper.VIRTUAL_ALL_APPS_TYPE:
			ArrayList<ApplicationInfo> applications = ((LauncherApplication) context.getApplicationContext()).getApplications();
			Dialogs.displayAllApps(context, applications);
			return;
		case DatabaseHelper.VIRTUAL_SPOTLIGHT_WEB_APPS_TYPE:
			Dialogs.displayAllSpotlight(context);
			return;
		case DatabaseHelper.VIRTUAL_LIVE_TV_TYPE:
			Utils.launchLiveTV(context);
			return;
		}
	}

	@Override
	public void renderIcon(ImageView imageView) {
		if (type >= DatabaseHelper.VIRTUAL_APP_TYPE) {
			switch (type) {
			case DatabaseHelper.VIRTUAL_NOTIFICATIONS_TYPE:
				imageView.setImageResource(R.drawable.notifications);
				return;
			case DatabaseHelper.VIRTUAL_BROWSER_BOOKMARKS_TYPE:
				imageView.setImageResource(R.drawable.bookmarks);
				return;
			case DatabaseHelper.VIRTUAL_BROWSER_HISTORY_TYPE:
				imageView.setImageResource(R.drawable.browser_history);
				return;
			case DatabaseHelper.VIRTUAL_ALL_APPS_TYPE:
				imageView.setImageResource(R.drawable.all_apps);
				return;
			case DatabaseHelper.VIRTUAL_SPOTLIGHT_WEB_APPS_TYPE:
				imageView.setImageResource(R.drawable.spotlight);
				return;
			case DatabaseHelper.VIRTUAL_LIVE_TV_TYPE:
				imageView.setImageResource(R.drawable.livetv);
				return;
			}
		}
		super.renderIcon(imageView);
	}

	@Override
	public void persistInsert(Context context, int rowId, int position) throws Exception {
		ItemsTable.insertItem(context, rowId, position, getTitle(), getIntent(), null, type);
	}

	@Override
	public void persistUpdate(Context context, int rowId, int position) throws Exception {
		ItemsTable.updateItem(context, getId(), rowId, position, getTitle(), getIntent(), null, type);
	}

}
