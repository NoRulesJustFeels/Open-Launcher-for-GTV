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
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;
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

	// http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
	private static final Pattern REGEX_PATTERN_DASH = Pattern.compile("-");
	private static final Matcher REGEX_PATTERN_DASH_MATCHER = REGEX_PATTERN_DASH.matcher("");

	/**
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 *      android.content.Intent)
	 */
	public void onReceive(final Context context, Intent data) {
		if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
			return;
		}

		final PackageManager packageManager = context.getPackageManager();

		// get the shortcut data
		final Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		if (intent.getAction() == null) {
			intent.setAction(Intent.ACTION_VIEW);
		}
		Log.d(LOG_TAG, intent.toUri(Intent.URI_INTENT_SCHEME));
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		if (name == null && intent.getData() != null) {
			// Some shortcuts for TV channels don't have a name!
			// TODO should we even allow this case?
			name = extractUriInfo(intent.getDataString());
		}
		// Map local channel callsigns to broadcast network names
		if (intent.getData() != null && intent.getDataString().startsWith("tv")) {
			name = mapLocalChannelToNetwork(context, name);
		}
		if (name == null) {
			name = context.getString(R.string.unknown);
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
							} else if (info.getIntent().getScheme() != null && intent.getScheme() != null
									&& info.getIntent().getScheme().equals(intent.getScheme()) && info.getIntent().getDataString() != null
									&& intent.getDataString() != null && info.getIntent().getDataString().equals(intent.getDataString())) {
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

				final String intentIcon = filename;
				final String intentName = name;
				new Thread(new Runnable() {
					public void run() {
						String biggerIcon = null;
						if (intent.getData() != null && intent.getDataString().startsWith("http")) {
							biggerIcon = Utils.getWebSiteIcon(context, intent.getDataString());
						}
						// Invoke main launcher activity to let the user pick
						// the row to add the shortcut
						Intent launcherIntent = new Intent(Launcher.SHORTCUTS_INTENT, intent.getData(), context, Launcher.class);
						launcherIntent.putExtra("icon", biggerIcon == null ? intentIcon : biggerIcon);
						launcherIntent.putExtra("name", intentName);
						launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(launcherIntent);
					}
				}).start();
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
	private static String extractUriInfo(String data) {
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
				} else if (uri.getScheme().equals("http")) {
					return uri.getHost().toUpperCase();
				}
			} catch (Exception e) {
				Log.d(LOG_TAG, "extractChannelInfo: " + data, e);
			}
		}
		return null;
	}

	/**
	 * Map local USA channel callsigns to their broadcast network names.
	 * Use FCC data.
	 * 
	 * @param context
	 * @param name
	 * @return
	 */
	private String mapLocalChannelToNetwork(Context context, String name) {
		if (name != null && Utils.isUsa()) {
			name = Utils.stripBrackets(name.toUpperCase());
			boolean usLocalChannel = false;
			if ((name.startsWith("K") || name.startsWith("W") || name.startsWith("X"))) {
				if (name.length() >= 4) { // KXTX
					usLocalChannel = true;
				} else if (name.length() >= 3) { // WRC
					String[] nameParts = name.split(" ");
					if (nameParts.length > 1) {
						if (nameParts[0].length() == 3) { // WRC
							try {
								// WRC 4 WASHINGTON DC
								float value = Float.parseFloat(nameParts[1]);
								usLocalChannel = true;
							} catch (Exception ex) {
							}
						}
					} else if (nameParts.length == 1) {
						usLocalChannel = true;
					}
				}
			}
			if (usLocalChannel) {
				// normalize local channel names
				String[] suffix = { "DT1", "DT2", "DT3", "DT4", "DT5", "DT6", "SD1", "SD2", "LD2", "LD3", "LD4", "SAT", "CD2", "CD3", "DT", "TV", "HD", "SD",
						"D1", "D2", "D3", "D4", "D5", "LP", "CD", "CA", "LD", "WX", "D", "H" };
				Matcher dashMatcher = REGEX_PATTERN_DASH_MATCHER.reset("");
				for (int i = 0; i < suffix.length; i++) {
					// order is important; do '-' before others
					String sx = "-" + suffix[i];
					// 3 or 4
					if (name.endsWith(sx) && name.length() >= sx.length() + 3) {
						name = name.substring(0, name.length() - sx.length()).trim();
						break;
					}
					sx = " " + suffix[i];
					if (name.endsWith(sx) && name.length() >= sx.length() + 3) {
						name = name.substring(0, name.length() - sx.length()).trim();
						break;
					}
					sx = suffix[i];
					if (name.endsWith(sx) && name.length() >= sx.length() + 3) {
						name = name.substring(0, name.length() - sx.length()).trim();
						break;
					}
					dashMatcher = REGEX_PATTERN_DASH_MATCHER.reset(name);
					if (dashMatcher.find()) {
						String noDashName = dashMatcher.replaceAll("").trim();
						if (noDashName.endsWith(sx) && noDashName.length() >= sx.length() + 3) {
							name = noDashName.substring(0, noDashName.length() - sx.length()).trim();
							break;
						}
					}
				}
				if (name.length() >= 4) {
					String rest = name.substring(4).trim();
					try {
						float value = Float.parseFloat(rest); // WPIX11.1
						name = name.substring(0, 4).trim();
					} catch (Exception ex) {
					}
				} else if (name.length() >= 3) {
					String rest = name.substring(3).trim();
					try {
						float value = Float.parseFloat(rest); // WPI11.1
						name = name.substring(0, 4).trim();
					} catch (Exception ex) {
					}
				}

				Hashtable<String, String> callsigns = Utils.readUsLocalCallsigns(context);
				String callsign = callsigns.get(name);
				if (callsign != null) {
					name = callsign;
				}
			}
		}
		return name;
	}

}
