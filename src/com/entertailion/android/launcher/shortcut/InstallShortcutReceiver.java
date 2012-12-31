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

package com.entertailion.android.launcher.shortcut;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.database.RowsTable;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.row.RowInfo;
import com.entertailion.android.launcher.utils.FastBitmapDrawable;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Receiver to allow other apps to install shortcuts in the launcher. Examples
 * include browser bookmarks and TV channels.
 * 
 * @see manifest: com.android.launcher.action.INSTALL_SHORTCUT
 * 
 * @author leon_nicholls
 * 
 */
public class InstallShortcutReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "InstallShortcutReceiver";
	private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

	/**
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 *      android.content.Intent)
	 */
	public void onReceive(Context context, Intent data) {
		if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
			return;
		}

		final PackageManager packageManager = context.getPackageManager();

		// get the shortcut data
		Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		if (intent.getAction() == null) {
			intent.setAction(Intent.ACTION_VIEW);
		}
		Log.d(LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		if (name == null) {
			// Some shortcuts for TV channels don't have a name!
			// TODO should we even allow this case?
			name = extractChannelInfo(intent.getDataString());
			if (name == null) {
				name = context.getString(R.string.unknown);
			}
		}

		// Extract the icon
		Parcelable icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
		Bitmap bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
		Drawable drawable = null;
		if (icon != null && icon instanceof ShortcutIconResource) {
			try {
				ShortcutIconResource iconResource = (ShortcutIconResource) icon;
				Resources resources = packageManager.getResourcesForApplication(iconResource.packageName);
				int id = resources.getIdentifier(iconResource.resourceName, null, null);
				drawable = resources.getDrawable(id);
			} catch (Exception e) {
				Log.e(LOG_TAG, "icon", e);
			}
		} else if (bitmap != null) {
			// TV channels: 180x135, Web sites: 64x64
			drawable = new FastBitmapDrawable(Utils.createBitmapThumbnail(bitmap, context));
		}

		// default icon if null
		if (drawable == null) {
			drawable = packageManager.getDefaultActivityIcon();
		}

		// check for duplicates
		boolean duplicate = data.getBooleanExtra(EXTRA_SHORTCUT_DUPLICATE, true);
		boolean exists = false;
		if (!duplicate) {
			ArrayList<RowInfo> rows = RowsTable.getRows(context);
			if (rows != null) {
				// Get the favorite items for each row
				for (RowInfo row : rows) {
					ArrayList<ItemInfo> items = ItemsTable.getItems(context, row.getId());
					for (ItemInfo info : items) {
						// intent://channel/KDFW?deviceId=Logitech01&channelNumber=4#Intent;scheme=tv;action=android.intent.action.VIEW;launchFlags=0x14000000;component=com.google.tv.player/.PlayerActivity;end
						// intent://www.google.com/#Intent;scheme=http;action=android.intent.action.VIEW;S.com.android.browser.application_id=-376878250306478472;end
						if (info.getTitle().equals(name) && info.getIntent() != null) {
							if (info.getIntent().getComponent() != null && intent.getComponent() != null
									&& info.getIntent().getComponent().getClassName().equals(intent.getComponent().getClassName())) {
								exists = true;
								break;
							} else if (info.getIntent().getScheme().equals(intent.getScheme())
									&& info.getIntent().getDataString().equals(intent.getDataString())) {
								exists = true;
								break;
							} else if (info.getIntent().toUri(Intent.URI_INTENT_SCHEME).equals(intent.toUri(Intent.URI_INTENT_SCHEME))) {
								exists = true;
								break;
							}
						}
					}
					if (exists) {
						break;
					}
				}
			}
		}
		if (duplicate || !exists) {
			try {
				String filename = null;
				if (drawable != null) {
					filename = "shortcut_" + Utils.clean(intent.toString()) + ".png";
					Utils.saveToFile(context, Utils.drawableToBitmap(drawable), drawable.getBounds().width(), drawable.getBounds().height(), filename);
				}
				// Invoke main launcher activity to let the user pick the row to
				// add the shortcut
				Intent launcherIntent = new Intent(Launcher.SHORTCUTS_INTENT, intent.getData(), context, Launcher.class);
				launcherIntent.putExtra("icon", filename);
				launcherIntent.putExtra("name", name);
				launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(launcherIntent);
			} catch (Exception e) {
				Log.e(LOG_TAG, "onReceive", e);
				Toast.makeText(context, context.getString(R.string.shortcut_not_installed, name), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Utility method to extract the channel name or callsign from the TV
	 * channel URI.
	 * 
	 * @see https://developers.google.com/tv/android/docs/gtv_channel_lineup
	 * 
	 * @param data
	 * @return
	 */
	private static String extractChannelInfo(String data) {
		if (data != null) {
			// Various TV URI formats from different Google TV device OEMs:
			// tv://channel/VODDM?deviceId=irb_0&channelNumber=250
			// tv://channel?deviceId=irb_0&channelNumber=250
			// tv://channel/EHD?deviceId=Time%20Warner%20Cable
			try {
				Uri uri = Uri.parse(data);
				if (uri.getScheme().equals("tv")) {
					if (uri.getPath() != null && uri.getPath().length() > 0) {
						// extract the callsign
						return uri.getPath().substring(1);
					} else if (uri.getQuery() != null && uri.getQuery().length() > 0) {
						// extract the channel number
						return uri.getQueryParameter("channelNumber");
					}
				}
			} catch (Exception e) {
				Log.d(LOG_TAG, "extractChannelInfo: " + data, e);
			}
		}
		return null;
	}

}
