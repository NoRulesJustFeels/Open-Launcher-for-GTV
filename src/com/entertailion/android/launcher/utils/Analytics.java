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
package com.entertailion.android.launcher.utils;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.entertailion.android.launcher.R;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * Utility class to manage Google analytics
 * 
 * @see https://developers.google.com/analytics/devguides/collection/android/v2/
 * @author leon_nicholls
 * 
 */
public class Analytics {
	private static final String LOG_CAT = "Analytics";

	public static final String ANALYTICS = "Analytics";
	public static final String LAUNCHER_MAIN = "launcher.main";
	public static final String LAUNCHER_HOME = "launcher.home";
	public static final String ABOUT_PRIVACY_POLICY = "about.privacy_policy";
	public static final String ABOUT_WEB_SITE = "about.website";
	public static final String ABOUT_MORE_APPS = "about.more_apps";
	public static final String DIALOG_INTRODUCTION = "dialog.introduction";
	public static final String DIALOG_ABOUT = "dialog.about";
	public static final String DIALOG_ALL_APPS = "dialog.all_apps";
	public static final String DIALOG_BOOKMARKS = "dialog.bookmarks";
	public static final String DIALOG_SPOTLIGHT_WEB_APPS = "dialog.spotlight_web_apps";
	public static final String DIALOG_ADD_APP = "dialog.add_app";
	public static final String DIALOG_ADD_SPOTLIGHT_WEB_APP = "dialog.add_spotlight_web_app";
	public static final String DIALOG_ADD_BROWSER_BOOKMARK = "dialog.add_browser_bookmark";
	public static final String DIALOG_ADD_SHORTCUT = "dialog.add_shortcut";
	public static final String DIALOG_DELETE_ITEM = "dialog.delete_item";
	public static final String DIALOG_DELETE_ROW = "dialog.delete_row";
	public static final String DIALOG_CHANGE_ROW_ORDER = "dialog.change_row_order";
	public static final String SETTINGS = "settings";
	public static final String SYSTEM_SETTINGS = "system.settings";
	public static final String PREFERENCE_CLOCK_ON = "preference.clock.on";
	public static final String PREFERENCE_CLOCK_OFF = "preference.clock.off";
	public static final String PREFERENCE_WEATHER_ON = "preference.weather.on";
	public static final String PREFERENCE_WEATHER_OFF = "preference.weather.off";
	public static final String PREFERENCE_LIVE_TV_ON = "preference.live_tv.on";
	public static final String PREFERENCE_LIVE_TV_OFF = "preference.live_tv.off";
	public static final String PREFERENCE_ROW_NAME_ON = "preference.row_name.on";
	public static final String PREFERENCE_ROW_NAME_OFF = "preference.row_name.off";
	public static final String PREFERENCE_ITEM_NAME_ON = "preference.item_name.on";
	public static final String PREFERENCE_ITEM_NAME_OFF = "preference.item_name.off";
	public static final String RATING_YES = "rating.yes";
	public static final String RATING_NO = "rating.no";
	public static final String RATING_LATER = "rating.later";
	public static final String INVOKE_APP = "invoke.app";
	public static final String INVOKE_SPOTLIGHT_WEB_APP = "invoke.spotlight_web_app";
	public static final String INVOKE_NOTIFICATIONS = "invoke.notifications";
	public static final String INVOKE_BOOKMARK = "invoke.bookmark";
	public static final String INVOKE_SHORTCUT = "invoke.shortcut";
	public static final String ADD_APP = "add.app";
	public static final String ADD_APP_WITH_ROW = "add.app_with_row";
	public static final String ADD_SPOTLIGHT_WEB_APP = "add.spotlight_web_app";
	public static final String ADD_SPOTLIGHT_WEB_APP_WITH_ROW = "add.spotlight_web_app_with_row";
	public static final String ADD_BROWSER_BOOKMARK = "add.browser_bookmark";
	public static final String ADD_BROWSER_BOOKMARK_WITH_ROW = "add.browser_bookmark_with_row";
	public static final String ADD_SHORTCUT = "add.shortcut";
	public static final String ADD_SHORTCUT_WITH_ROW = "add.shortcut_with_row";
	public static final String DELETE_ITEM = "delete.item";
	public static final String DELETE_ROW = "delete.row";
	public static final String UNINSTALL_APP = "uninstall.app";
	public static final String EASTER_EGG = "easter.egg";
	public static final String CHANGE_ROW_ORDER = "row.change_order";
	public static final String WIDGET_PICK = "widget.pick";
	public static final String WIDGET_ADD = "widget.add";
	public static final String MOVE_ITEM = "move.item";
	public static final String CHANGE_ROW_NAME = "row.change_name";

	private static Context context;

	public static void createAnalytics(Context context) {
		try {
			Analytics.context = context;
			EasyTracker.getInstance().setContext(context);
		} catch (Exception e) {
			Log.e(LOG_CAT, "createAnalytics", e);
		}
	}

	public static void startAnalytics(final Activity activity) {
		try {
			if (activity != null && activity.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getInstance().activityStart(activity);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "startAnalytics", e);
		}
	}

	public static void stopAnalytics(Activity activity) {
		try {
			if (activity != null && activity.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getInstance().activityStop(activity);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "stopAnalytics", e);
		}
	}

	public static void logEvent(String event) {
		try {
			if (context != null && context.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getTracker().trackEvent(ANALYTICS, event, event, 1L);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "logEvent", e);
		}
	}

	public static void logEvent(String event, Map<String, String> parameters) {
		try {
			if (context != null && context.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getTracker().trackEvent(ANALYTICS, event, event, 1L);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "logEvent", e);
		}
	}
}
