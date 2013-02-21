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
package com.entertailion.android.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Browser;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.entertailion.android.launcher.apps.ApplicationInfo;
import com.entertailion.android.launcher.apps.VirtualAppInfo;
import com.entertailion.android.launcher.bookmark.BookmarkAdapter;
import com.entertailion.android.launcher.bookmark.BookmarkInfo;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.database.RecentAppsTable;
import com.entertailion.android.launcher.database.RowsTable;
import com.entertailion.android.launcher.database.SpotlightTable;
import com.entertailion.android.launcher.item.AllItemAdapter;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.row.RowAdapter;
import com.entertailion.android.launcher.row.RowInfo;
import com.entertailion.android.launcher.shortcut.InstallShortcutReceiver;
import com.entertailion.android.launcher.spotlight.AllSpotlightAdapter;
import com.entertailion.android.launcher.spotlight.SpotlightInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Utility class to display various dialogs for the main launcher
 * 
 * @author leon_nicholls
 * 
 */
public class Dialogs {

	private static final String LOG_TAG = "Dialogs";

	// Ratings dialog configuration
	public static final String DATE_FIRST_LAUNCHED = "date_first_launched";
	public static final String DONT_SHOW_RATING_AGAIN = "dont_show_rating_again";
	private final static int DAYS_UNTIL_PROMPT = 5;
	private final static int BROWSER_HISTORY_LIMIT = 20;

	// Shared OnKeyListener for list/grid views
	private static OnKeyListener onKeyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (v instanceof AbsListView) {
				AbsListView absListView = (AbsListView) v;
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
						// Jump to first item that starts with the typed letter
						char letter = Utils.keyCodeToLetter(keyCode);
						int count = absListView.getAdapter().getCount();
						for (int i = 0; i < count; i++) {
							ItemInfo itemInfo = (ItemInfo) absListView.getAdapter().getItem(i);
							if (itemInfo.getTitle().toUpperCase().charAt(0) == letter) {
								absListView.setSelection(i);
								break;
							}
						}
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
						// Go to the last item
						int count = absListView.getAdapter().getCount();
						absListView.setSelection(count - 1);
					} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
						// Go to the first item
						absListView.setSelection(0);
					} else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
						// Go to the numbered item position; use 1 for first
						// position, 0 for 10th position
						int count = absListView.getAdapter().getCount();
						if (keyCode == KeyEvent.KEYCODE_0 && count >= 10) {
							absListView.setSelection(9);
						} else {
							int index = keyCode - KeyEvent.KEYCODE_0;
							if (index <= count) {
								absListView.setSelection(index - 1);
							}
						}
					}
				}
			}
			return false;
		}

	};

	/**
	 * Display introduction to the user for first time launch
	 * 
	 * @param context
	 */
	public static void displayIntroduction(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.introduction);

		Typeface lightTypeface = ((LauncherApplication) context.getApplicationContext()).getLightTypeface(context);

		TextView titleTextView = (TextView) dialog.findViewById(R.id.intro_title);
		titleTextView.setTypeface(lightTypeface);
		TextView textView1 = (TextView) dialog.findViewById(R.id.intro_text1);
		textView1.setTypeface(lightTypeface);
		TextView textView2 = (TextView) dialog.findViewById(R.id.intro_text2);
		textView2.setTypeface(lightTypeface);

		((Button) dialog.findViewById(R.id.intro_button)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_INTRODUCTION);
	}

	/**
	 * Display about dialog to user when invoked from menu option.
	 * 
	 * @param context
	 */
	public static void displayAbout(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.about);

		Typeface lightTypeface = ((LauncherApplication) context.getApplicationContext()).getLightTypeface(context);

		TextView aboutTextView = (TextView) dialog.findViewById(R.id.about_text1);
		aboutTextView.setTypeface(lightTypeface);
		aboutTextView.setText(context.getString(R.string.about_version_title, Utils.getVersion(context)));
		aboutTextView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				context.showCover(false);
				dialog.dismiss();
				Intent intent = new Intent(context, EasterEggActivity.class);
				context.startActivity(intent);
				Analytics.logEvent(Analytics.EASTER_EGG);
				return true;
			}

		});
		TextView copyrightTextView = (TextView) dialog.findViewById(R.id.copyright_text);
		copyrightTextView.setTypeface(lightTypeface);
		TextView feedbackTextView = (TextView) dialog.findViewById(R.id.feedback_text);
		feedbackTextView.setTypeface(lightTypeface);

		((Button) dialog.findViewById(R.id.button_web)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(context.getString(R.string.about_button_web_url)));
				context.startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_WEB_SITE);
				context.showCover(false);
				dialog.dismiss();
			}

		});

		((Button) dialog.findViewById(R.id.button_privacy_policy)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(context.getString(R.string.about_button_privacy_policy_url)));
				context.startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_PRIVACY_POLICY);
				context.showCover(false);
				dialog.dismiss();
			}

		});
		((Button) dialog.findViewById(R.id.button_more_apps)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(context.getString(R.string.about_button_more_apps_url)));
				context.startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_MORE_APPS);
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ABOUT);
	}

	/**
	 * Display a grid of all installed apps + virtual apps. Allow user to launch
	 * apps.
	 * 
	 * @param context
	 * @param applications
	 */
	public static void displayAllApps(final Launcher context, final ArrayList<ApplicationInfo> applications) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.apps_grid);

		final GridView gridView = (GridView) dialog.findViewById(R.id.grid);
		gridView.setAdapter(new AllItemAdapter(context, getApplications(context, applications, false)));
		gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ItemInfo itemInfo = (ItemInfo) parent.getAdapter().getItem(position);
				itemInfo.invoke(context);
				context.showCover(false);
				dialog.dismiss();
				if (itemInfo instanceof ApplicationInfo) {
					ApplicationInfo applicationInfo = (ApplicationInfo) itemInfo;
					RecentAppsTable.persistRecentApp(context, applicationInfo);
				}
			}

		});
		gridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> gridView, View view, int pos, long arg3) {
				ItemInfo itemInfo = (ItemInfo) gridView.getAdapter().getItem(pos);
				if (itemInfo instanceof ApplicationInfo) {
					Uri packageURI = Uri.parse("package:" + itemInfo.getIntent().getComponent().getPackageName());
					Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
					context.startActivity(uninstallIntent);
					context.showCover(false);
					dialog.dismiss();
					Analytics.logEvent(Analytics.UNINSTALL_APP);
				}

				return false;
			}

		});
		gridView.setDrawingCacheEnabled(true);
		gridView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ALL_APPS);
	}

	/**
	 * Utility method to add virtual apps to the list of installed Android apps.
	 * 
	 * @param context
	 * @param applications
	 * @param addAllApps
	 * @return
	 */
	private static ArrayList<ItemInfo> getApplications(final Launcher context, ArrayList<ApplicationInfo> applications, boolean addAllApps) {
		ArrayList<ItemInfo> apps = new ArrayList<ItemInfo>(applications);

		// add notifications as a virtual app
		VirtualAppInfo notifications = new VirtualAppInfo();
		notifications.setType(DatabaseHelper.VIRTUAL_NOTIFICATIONS_TYPE);
		notifications.setTitle(context.getString(R.string.notifications));
		notifications.setDrawable(context.getResources().getDrawable(R.drawable.notifications));
		apps.add(notifications);

		// add bookmarks as a virtual app
		VirtualAppInfo bookmarks = new VirtualAppInfo();
		bookmarks.setType(DatabaseHelper.VIRTUAL_BROWSER_BOOKMARKS_TYPE);
		bookmarks.setTitle(context.getString(R.string.bookmarks));
		bookmarks.setDrawable(context.getResources().getDrawable(R.drawable.bookmarks));
		apps.add(bookmarks);

		// add browser history as a virtual app
		VirtualAppInfo browserHistory = new VirtualAppInfo();
		browserHistory.setType(DatabaseHelper.VIRTUAL_BROWSER_HISTORY_TYPE);
		browserHistory.setTitle(context.getString(R.string.browser_history));
		browserHistory.setDrawable(context.getResources().getDrawable(R.drawable.browser_history));
		apps.add(browserHistory);

		if (addAllApps) {
			// add all apps as a virtual app
			VirtualAppInfo allApps = new VirtualAppInfo();
			allApps.setType(DatabaseHelper.VIRTUAL_ALL_APPS_TYPE);
			allApps.setTitle(context.getString(R.string.all_apps));
			allApps.setDrawable(context.getResources().getDrawable(R.drawable.all_apps));
			apps.add(allApps);
		}

		// add spotlight web apps as a virtual app
		VirtualAppInfo spotlightWebApps = new VirtualAppInfo();
		spotlightWebApps.setType(DatabaseHelper.VIRTUAL_SPOTLIGHT_WEB_APPS_TYPE);
		spotlightWebApps.setTitle(context.getString(R.string.spotlight_web_apps));
		spotlightWebApps.setDrawable(context.getResources().getDrawable(R.drawable.spotlight));
		apps.add(spotlightWebApps);

		// add live TV as a virtual app
		VirtualAppInfo liveTV = new VirtualAppInfo();
		liveTV.setType(DatabaseHelper.VIRTUAL_LIVE_TV_TYPE);
		liveTV.setTitle(context.getString(R.string.live_tv));
		liveTV.setDrawable(context.getResources().getDrawable(R.drawable.livetv));
		apps.add(liveTV);

		Collections.sort(apps, new Comparator<ItemInfo>() {

			@Override
			public int compare(ItemInfo lhs, ItemInfo rhs) {
				return lhs.getTitle().toLowerCase().compareTo(rhs.getTitle().toLowerCase());
			}

		});

		return apps;
	}

	/**
	 * Display the list of browser bookmarks. Allow user to load bookmarked web
	 * site.
	 * 
	 * @param context
	 */
	public static void displayBookmarks(final Launcher context) {
		final ArrayList<BookmarkInfo> bookmarks = loadBookmarks(context);
		if (bookmarks.size()>0) {
			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.bookmarks_list);
	
			ListView listView = (ListView) dialog.findViewById(R.id.list);
			Collections.sort(bookmarks, new Comparator<BookmarkInfo>() {
	
				@Override
				public int compare(BookmarkInfo lhs, BookmarkInfo rhs) {
					return lhs.getTitle().toLowerCase().compareTo(rhs.getTitle().toLowerCase());
				}
	
			});
			listView.setAdapter(new BookmarkAdapter(context, bookmarks));
			listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
	
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					BookmarkInfo bookmark = (BookmarkInfo) parent.getAdapter().getItem(position);
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.getUrl()));
					context.startActivity(browserIntent);
					context.showCover(false);
					dialog.dismiss();
					Analytics.logEvent(Analytics.INVOKE_BOOKMARK);
				}
	
			});
			listView.setDrawingCacheEnabled(true);
			listView.setOnKeyListener(onKeyListener);
			dialog.setOnDismissListener(new OnDismissListener() {
	
				@Override
				public void onDismiss(DialogInterface dialog) {
					context.showCover(false);
				}
	
			});
			context.showCover(true);
			dialog.show();
			Analytics.logEvent(Analytics.DIALOG_BOOKMARKS);
		} else {
			displayAlert(context, context.getString(R.string.dialog_no_browser_bookmarks));
		}
	}

	/**
	 * Utility method to load the list of browser bookmarks.
	 */
	private static ArrayList<BookmarkInfo> loadBookmarks(Launcher context) {
		ArrayList<BookmarkInfo> bookmarks = new ArrayList<BookmarkInfo>();

		Cursor cursor = context.managedQuery(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			if (cursor.moveToFirst() && cursor.getCount() > 0) {
				while (cursor.isAfterLast() == false) {
					String bookmarkIndex = cursor.getString(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX);
					if (bookmarkIndex != null && bookmarkIndex.equals("1")) {
						BookmarkInfo bookmark = new BookmarkInfo();
						String title = cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);
						bookmark.setTitle(title);
						String url = cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
						bookmark.setUrl(url);

						// for some reason the favicons aren't good looking
						// images
						// byte[] data =
						// cursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
						// if (data != null) {
						// try {
						// Bitmap bitmap = BitmapFactory.decodeByteArray(data,
						// 0, data.length);
						// bookmark.setDrawable(new FastBitmapDrawable(bitmap));
						// } catch (Exception e) {
						// Log.e(LOG_TAG, "bookmark icon", e);
						// }
						// }
						bookmarks.add(bookmark);
					}

					cursor.moveToNext();
				}
			}
		}
		return bookmarks;
	}

	/**
	 * Display the list of browser history.
	 * 
	 * @param context
	 */
	public static void displayBrowserHistory(final Launcher context) {
		final ArrayList<BookmarkInfo> bookmarks = loadBrowserHistory(context);
		if (bookmarks.size()>0) {
			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.bookmarks_list);
	
			ListView listView = (ListView) dialog.findViewById(R.id.list);
			listView.setAdapter(new BookmarkAdapter(context, bookmarks));
			listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
	
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					BookmarkInfo bookmark = (BookmarkInfo) parent.getAdapter().getItem(position);
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.getUrl()));
					context.startActivity(browserIntent);
					context.showCover(false);
					dialog.dismiss();
					Analytics.logEvent(Analytics.INVOKE_BOOKMARK);
				}
	
			});
			listView.setDrawingCacheEnabled(true);
			listView.setOnKeyListener(onKeyListener);
			dialog.setOnDismissListener(new OnDismissListener() {
	
				@Override
				public void onDismiss(DialogInterface dialog) {
					context.showCover(false);
				}
	
			});
			context.showCover(true);
			dialog.show();
			Analytics.logEvent(Analytics.DIALOG_BOOKMARKS);
		} else {
			displayAlert(context, context.getString(R.string.dialog_no_browser_history));
		}
	}

	/**
	 * Utility method to load the list of browser history.
	 */
	private static ArrayList<BookmarkInfo> loadBrowserHistory(Launcher context) {
		ArrayList<BookmarkInfo> bookmarks = new ArrayList<BookmarkInfo>();

		Cursor cursor = context.managedQuery(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, Browser.BookmarkColumns.DATE + " DESC");
		if (cursor != null) {
			int count = 0;
			cursor.moveToFirst();
			if (cursor.moveToFirst() && cursor.getCount() > 0) {
				while (cursor.isAfterLast() == false) {
					String title = cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);
					String url = cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
					String bookmarkIndex = cursor.getString(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX);
					Log.d(LOG_TAG, "bookmarkIndex="+bookmarkIndex);
					if (url != null && title != null && !bookmarkIndex.equals("1")) {
						// check for duplicates
						boolean found = false;
						for (BookmarkInfo bookmarkInfo : bookmarks) {
							if (bookmarkInfo.getUrl() != null && bookmarkInfo.getUrl().equals(url)) {
								found = true;
								break;
							}
						}
						if (!found) {
							BookmarkInfo bookmark = new BookmarkInfo();
							bookmark.setTitle(title);
							bookmark.setUrl(url);
							bookmarks.add(bookmark);
						}
					}
					cursor.moveToNext();
					if (++count >= BROWSER_HISTORY_LIMIT) {
						break;
					}
				}
			}
		}
		return bookmarks;
	}

	/**
	 * Display the list of Spotlight web apps:
	 * https://www.google.com/tv/spotlight-gallery.html Allow the user to launch
	 * a web app in the browser.
	 * 
	 * @param context
	 */
	public static void displayAllSpotlight(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.spotlight_grid);

		final GridView gridView = (GridView) dialog.findViewById(R.id.grid);
		final ArrayList<SpotlightInfo> spotlights = SpotlightTable.getAllSpotlights(context);
		gridView.setAdapter(new AllSpotlightAdapter(context, spotlights));
		gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SpotlightInfo spotlightInfo = (SpotlightInfo) parent.getAdapter().getItem(position);
				spotlightInfo.invoke(context);
				context.showCover(false);
				dialog.dismiss();
				Analytics.logEvent(Analytics.INVOKE_SPOTLIGHT_WEB_APP);
			}

		});
		gridView.setDrawingCacheEnabled(true);
		gridView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_SPOTLIGHT_WEB_APPS);
	}

	/**
	 * Display dialog to allow user to add an app to a row. The user can add the
	 * app to an existing row or a new row.
	 * 
	 * @param context
	 * @param applications
	 */
	public static void displayAddApps(final Launcher context, final ArrayList<ApplicationInfo> applications) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.add_apps_grid);

		final EditText nameEditText = (EditText) dialog.findViewById(R.id.rowName);
		final RadioButton currentRadioButton = (RadioButton) dialog.findViewById(R.id.currentRadio);
		currentRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// hide the row name edit field if the current row radio button
				// is selected
				nameEditText.setVisibility(View.GONE);
			}

		});
		final RadioButton newRadioButton = (RadioButton) dialog.findViewById(R.id.newRadio);
		newRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// show the row name edit field if the new radio button is
				// selected
				nameEditText.setVisibility(View.VISIBLE);
				nameEditText.requestFocus();
			}

		});
		final GridView gridView = (GridView) dialog.findViewById(R.id.grid);
		gridView.setAdapter(new AllItemAdapter(context, getApplications(context, applications, true)));
		gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// if the new row radio button is selected, the user must enter
				// a name for the new row
				String name = nameEditText.getText().toString().trim();
				if (newRadioButton.isChecked() && name.length() == 0) {
					nameEditText.requestFocus();
					displayAlert(context, context.getString(R.string.dialog_new_row_name_alert));
					return;
				}
				ItemInfo itemInfo = (ItemInfo) parent.getAdapter().getItem(position);
				boolean currentRow = !newRadioButton.isChecked();
				context.addItem(itemInfo, currentRow ? null : name);
				context.showCover(false);
				dialog.dismiss();
				if (currentRow) {
					Analytics.logEvent(Analytics.DIALOG_ADD_APP);
				} else {
					Analytics.logEvent(Analytics.ADD_APP_WITH_ROW);
				}
			}

		});
		gridView.setDrawingCacheEnabled(true);
		gridView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ADD_APP);
	}

	/**
	 * Display dialog to the user for the Spotlight web apps:
	 * https://www.google.com/tv/spotlight-gallery.html Allow the user to add a
	 * web app to an existing row or a new row.
	 * 
	 * @param context
	 */
	public static void displayAddSpotlight(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.add_apps_grid);

		final EditText nameEditText = (EditText) dialog.findViewById(R.id.rowName);
		final RadioButton currentRadioButton = (RadioButton) dialog.findViewById(R.id.currentRadio);
		currentRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// hide the row name edit field if the current row radio button
				// is selected
				nameEditText.setVisibility(View.GONE);
			}

		});
		final RadioButton newRadioButton = (RadioButton) dialog.findViewById(R.id.newRadio);
		newRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// show the row name edit field if the new radio button is
				// selected
				nameEditText.setVisibility(View.VISIBLE);
				nameEditText.requestFocus();
			}

		});
		final GridView gridView = (GridView) dialog.findViewById(R.id.grid);
		final ArrayList<SpotlightInfo> spotlights = SpotlightTable.getAllSpotlights(context);
		gridView.setAdapter(new AllSpotlightAdapter(context, spotlights));
		gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// if the new row radio button is selected, the user must enter
				// a name for the new row
				String name = nameEditText.getText().toString().trim();
				if (newRadioButton.isChecked() && name.length() == 0) {
					nameEditText.requestFocus();
					displayAlert(context, context.getString(R.string.dialog_new_row_name_alert));
					return;
				}
				ItemInfo itemInfo = (ItemInfo) parent.getAdapter().getItem(position);
				boolean currentRow = !newRadioButton.isChecked();
				context.addItem(itemInfo, currentRow ? null : name);
				context.showCover(false);
				dialog.dismiss();
				if (currentRow) {
					Analytics.logEvent(Analytics.ADD_SPOTLIGHT_WEB_APP);
				} else {
					Analytics.logEvent(Analytics.ADD_SPOTLIGHT_WEB_APP_WITH_ROW);
				}
			}

		});
		gridView.setDrawingCacheEnabled(true);
		gridView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ADD_SPOTLIGHT_WEB_APP);
	}
	
	/**
	 * Display dialog to the user for the browser bookmarks. Allow the user to add a
	 * browser bookmark to an existing row or a new row.
	 * 
	 * @param context
	 */
	public static void displayAddBrowserBookmark(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.add_browser_bookmarks_list);
		
		final EditText nameEditText = (EditText) dialog.findViewById(R.id.rowName);
		final RadioButton currentRadioButton = (RadioButton) dialog.findViewById(R.id.currentRadio);
		currentRadioButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// hide the row name edit field if the current row radio button
				// is selected
				nameEditText.setVisibility(View.GONE);
			}

		});
		final RadioButton newRadioButton = (RadioButton) dialog.findViewById(R.id.newRadio);
		newRadioButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// show the row name edit field if the new radio button is
				// selected
				nameEditText.setVisibility(View.VISIBLE);
				nameEditText.requestFocus();
			}

		});

		ListView listView = (ListView) dialog.findViewById(R.id.list);
		final ArrayList<BookmarkInfo> bookmarks = loadBookmarks(context);
		Collections.sort(bookmarks, new Comparator<BookmarkInfo>() {

			@Override
			public int compare(BookmarkInfo lhs, BookmarkInfo rhs) {
				return lhs.getTitle().toLowerCase().compareTo(rhs.getTitle().toLowerCase());
			}

		});
		listView.setAdapter(new BookmarkAdapter(context, bookmarks));
		listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				// run in thread since network logic needed to get bookmark icon
				new Thread(new Runnable() {
					public void run() {
						// if the new row radio button is selected, the user must enter
						// a name for the new row
						String name = nameEditText.getText().toString().trim();
						if (newRadioButton.isChecked() && name.length() == 0) {
							nameEditText.requestFocus();
							displayAlert(context, context.getString(R.string.dialog_new_row_name_alert));
							return;
						}
						boolean currentRow = !newRadioButton.isChecked();
						try {
							BookmarkInfo bookmark = (BookmarkInfo) parent.getAdapter().getItem(position);
							int rowId = 0;
							int rowPosition = 0;
							if (currentRow) {
								rowId = context.getCurrentGalleryId();
								ArrayList<ItemInfo> items = ItemsTable.getItems(context, rowId);
								rowPosition = items.size(); // in last
															// position
															// for selected
															// row
							} else {
								rowId = (int) RowsTable.insertRow(context, name, 0, RowInfo.FAVORITE_TYPE);
								rowPosition = 0;
							}
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse(bookmark.getUrl()));
							
							Uri uri = Uri.parse(bookmark.getUrl());
							Log.d(LOG_TAG, "host=" + uri.getScheme() + "://" + uri.getHost());
							String icon = Utils.getWebSiteIcon(context, "http://" + uri.getHost());
							Log.d(LOG_TAG, "icon1=" + icon);
							if (icon == null) {
								// try base host address
								int count = StringUtils.countMatches(uri.getHost(), ".");
								if (count > 1) {
									int index = uri.getHost().indexOf('.');
									String baseHost = uri.getHost().substring(index + 1);
									icon = Utils.getWebSiteIcon(context, "http://" + baseHost);
									Log.d(LOG_TAG, "icon2=" + icon);
								}
							}
							
							ItemsTable.insertItem(context, rowId, rowPosition, bookmark.getTitle(), intent, icon, DatabaseHelper.SHORTCUT_TYPE);
						} catch (Exception e) {
							Log.e(LOG_TAG, "displayAddBrowserBookmark", e);
						}
						
						// need to do this on UI thread
						context.getHandler().post(new Runnable() {
							public void run() {
								context.showCover(false);
								dialog.dismiss();
								context.reloadAllGalleries();
							}
						});

						if (currentRow) {
							Analytics.logEvent(Analytics.ADD_BROWSER_BOOKMARK);
						} else {
							Analytics.logEvent(Analytics.ADD_BROWSER_BOOKMARK_WITH_ROW);
						}

					}
				}).start();
			}
		});
		listView.setDrawingCacheEnabled(true);
		listView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ADD_BROWSER_BOOKMARK);
	}

	/**
	 * Utility method to display an alert dialog. Use instead of AlertDialog to
	 * get the right styling.
	 * 
	 * @param context
	 * @param message
	 */
	public static void displayAlert(final Launcher context, String message) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.alert);

		final TextView alertTextView = (TextView) dialog.findViewById(R.id.alertText);
		alertTextView.setText(message);
		Button alertButton = (Button) dialog.findViewById(R.id.alertButton);
		alertButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
	}

	/**
	 * Display a dialog to confirm that the user wants to delete an item.
	 * 
	 * @param context
	 */
	public static void displayDeleteItem(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.confirmation);

		TextView confirmationTextView = (TextView) dialog.findViewById(R.id.confirmationText);
		confirmationTextView.setText(context.getString(R.string.dialog_delete_item_message));
		Button buttonYes = (Button) dialog.findViewById(R.id.button1);
		buttonYes.setText(context.getString(R.string.dialog_yes));
		buttonYes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.deleteCurrentItem();
				context.showCover(false);
				dialog.dismiss();
			}

		});
		Button buttonNo = (Button) dialog.findViewById(R.id.button2);
		buttonNo.setText(context.getString(R.string.dialog_no));
		buttonNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_DELETE_ITEM);
	}

	/**
	 * Display a dialog to confirm that a user wants to delete a row.
	 * 
	 * @param context
	 */
	public static void displayDeleteRow(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.confirmation);

		TextView confirmationTextView = (TextView) dialog.findViewById(R.id.confirmationText);
		confirmationTextView.setText(context.getString(R.string.dialog_delete_row_message));
		Button buttonYes = (Button) dialog.findViewById(R.id.button1);
		buttonYes.setText(context.getString(R.string.dialog_yes));
		buttonYes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.deleteCurrentRow();
				context.showCover(false);
				dialog.dismiss();
			}

		});
		Button buttonNo = (Button) dialog.findViewById(R.id.button2);
		buttonNo.setText(context.getString(R.string.dialog_no));
		buttonNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_DELETE_ROW);
	}

	/**
	 * Prompt the user to rate the app.
	 * 
	 * @param context
	 */
	public static void displayRating(final Launcher context) {
		SharedPreferences prefs = context.getSharedPreferences(Launcher.PREFERENCES_NAME, Activity.MODE_PRIVATE);

		if (prefs.getBoolean(DONT_SHOW_RATING_AGAIN, false)) {
			return;
		}

		final SharedPreferences.Editor editor = prefs.edit();

		// Get date of first launch
		Long date_firstLaunch = prefs.getLong(DATE_FIRST_LAUNCHED, 0);
		if (date_firstLaunch == 0) {
			date_firstLaunch = System.currentTimeMillis();
			editor.putLong(DATE_FIRST_LAUNCHED, date_firstLaunch);
		}

		// Wait at least n days before opening
		if (System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.confirmation);

			TextView confirmationTextView = (TextView) dialog.findViewById(R.id.confirmationText);
			confirmationTextView.setText(context.getString(R.string.rating_message));
			Button buttonYes = (Button) dialog.findViewById(R.id.button1);
			buttonYes.setText(context.getString(R.string.dialog_yes));
			buttonYes.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.entertailion.android.launcher"));
					context.startActivity(intent);
					if (editor != null) {
						editor.putBoolean(DONT_SHOW_RATING_AGAIN, true);
						editor.commit();
					}
					Analytics.logEvent(Analytics.RATING_YES);
					context.showCover(false);
					dialog.dismiss();
				}

			});
			Button buttonNo = (Button) dialog.findViewById(R.id.button2);
			buttonNo.setText(context.getString(R.string.dialog_no));
			buttonNo.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (editor != null) {
						editor.putBoolean(DONT_SHOW_RATING_AGAIN, true);
						editor.commit();
					}
					Analytics.logEvent(Analytics.RATING_NO);
					context.showCover(false);
					dialog.dismiss();
				}

			});
			dialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					context.showCover(false);
				}

			});
			context.showCover(true);
			dialog.show();
		}

		editor.commit();
	}

	/**
	 * Display dialog to allow user to select which row to add the shortcut. For
	 * TV channels let the user change the channel name.
	 * 
	 * @see InstallShortcutReceiver
	 * 
	 * @param context
	 * @param name
	 * @param icon
	 * @param uri
	 */
	public static void displayShortcutsRowSelection(final Launcher context, final String name, final String icon, final String uri) {
		if (uri==null) {
			return;
		}
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		final boolean isChannel = uri.startsWith("tv");
		dialog.setContentView(R.layout.select_row);

		final TextView channelTextView = (TextView) dialog.findViewById(R.id.channelText);
		final EditText channelNameEditText = (EditText) dialog.findViewById(R.id.channelName);
		if (isChannel) {
			channelTextView.setVisibility(View.VISIBLE);
			channelNameEditText.setVisibility(View.VISIBLE);
			channelNameEditText.setText(name);
		}

		final TextView selectTextView = (TextView) dialog.findViewById(R.id.selectText);
		selectTextView.setText(context.getString(R.string.dialog_select_row, name));

		final Spinner spinner = (Spinner) dialog.findViewById(R.id.spinner);

		final EditText nameEditText = (EditText) dialog.findViewById(R.id.rowName);
		final RadioButton currentRadioButton = (RadioButton) dialog.findViewById(R.id.currentRadio);
		currentRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// hide the row name edit field if the current row radio button
				// is selected
				nameEditText.setVisibility(View.GONE);
				spinner.setVisibility(View.VISIBLE);
			}

		});
		final RadioButton newRadioButton = (RadioButton) dialog.findViewById(R.id.newRadio);
		newRadioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// show the row name edit field if the new radio button is
				// selected
				nameEditText.setVisibility(View.VISIBLE);
				nameEditText.requestFocus();
				spinner.setVisibility(View.GONE);
			}

		});

		List<String> list = new ArrayList<String>();
		final ArrayList<RowInfo> rows = RowsTable.getRows(context);
		if (rows != null) {
			for (RowInfo row : rows) {
				list.add(row.getTitle());
			}
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);

		Button buttonYes = (Button) dialog.findViewById(R.id.buttonOk);
		buttonYes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String shortcutName = name;
				try {
					if (isChannel) {
						String channelName = channelNameEditText.getText().toString().trim();
						if (channelName.length() == 0) {
							channelNameEditText.requestFocus();
							displayAlert(context, context.getString(R.string.dialog_channel_name_alert));
							return;
						}
						shortcutName = channelName;
					}
					// if the new row radio button is selected, the user must
					// enter a name for the new row
					String rowName = nameEditText.getText().toString().trim();
					if (newRadioButton.isChecked() && rowName.length() == 0) {
						nameEditText.requestFocus();
						displayAlert(context, context.getString(R.string.dialog_new_row_name_alert));
						return;
					}
					boolean currentRow = !newRadioButton.isChecked();
					int rowId = 0;
					int rowPosition = 0;
					if (currentRow) {
						if (rows != null) {
							String selectedRow = (String) spinner.getSelectedItem();
							for (RowInfo row : rows) {
								if (row.getTitle().equals(selectedRow)) {
									rowId = row.getId();
									ArrayList<ItemInfo> items = ItemsTable.getItems(context, rowId);
									rowPosition = items.size(); // in last
																// position
																// for selected
																// row
									break;
								}
							}
						}
					} else {
						rowId = (int) RowsTable.insertRow(context, rowName, 0, RowInfo.FAVORITE_TYPE);
						rowPosition = 0;
					}

					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(uri));
					ItemsTable.insertItem(context, rowId, rowPosition, shortcutName, intent, icon, DatabaseHelper.SHORTCUT_TYPE);
					Toast.makeText(context, context.getString(R.string.shortcut_installed, shortcutName), Toast.LENGTH_SHORT).show();
					context.reloadAllGalleries();

					if (currentRow) {
						Analytics.logEvent(Analytics.ADD_SHORTCUT);
					} else {
						Analytics.logEvent(Analytics.ADD_SHORTCUT_WITH_ROW);
					}

				} catch (Exception e) {
					Log.d(LOG_TAG, "onClick", e);
				}

				context.showCover(false);
				dialog.dismiss();
			}

		});
		Button buttonNo = (Button) dialog.findViewById(R.id.buttonCancel);
		buttonNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
			}

		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_ADD_SHORTCUT);
	}

	/**
	 * Change the order of the favorite rows.
	 * 
	 * @param context
	 */
	public static void displayChangeRowOrder(final Launcher context) {
		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.row_list);

		final ListView listView = (ListView) dialog.findViewById(R.id.rowList);
		ArrayList<RowInfo> persistedRows = RowsTable.getRows(context);
		final ArrayList<RowInfo> rows = new ArrayList<RowInfo>();
		// Add in reverse order to match favorite rows order
		for (RowInfo rowInfo : persistedRows) {
			rows.add(0, rowInfo);
		}
		final RowAdapter rowAdapter = new RowAdapter(context, rows);
		listView.setAdapter(rowAdapter);
		listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				RowInfo rowInfo = (RowInfo) parent.getAdapter().getItem(position);
				if (rowInfo.isSelected()) {
					rowInfo.setSelected(false);

					// Persist the new row order
					try {
						int counter = 0;
						for (int i = rowAdapter.getCount() - 1; i >= 0; i--) {
							RowInfo currentRowInfo = (RowInfo) parent.getAdapter().getItem(i);
							RowsTable.updateRow(context, currentRowInfo.getId(), currentRowInfo.getTitle(), counter, currentRowInfo.getType());
							counter++;
						}
					} catch (Exception e) {
						Log.e(LOG_TAG, "displayChangeRowOrder", e);
					}
					Analytics.logEvent(Analytics.CHANGE_ROW_ORDER);
				} else {
					rowInfo.setSelected(true);
				}
				rowAdapter.notifyDataSetChanged();
			}

		});
		listView.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// Swap the selected item in the list
				int selectedRowIndex = -1;
				for (int i = 0; i < rows.size(); i++) {
					RowInfo rowInfo = rows.get(i);
					if (rowInfo.isSelected()) {
						selectedRowIndex = i;
						break;
					}
				}
				if (selectedRowIndex != -1) {
					try {
						Collections.swap(rows, position, selectedRowIndex);
						rowAdapter.notifyDataSetChanged();
					} catch (Exception e) {
						Log.e(LOG_TAG, "displayChangeRowOrder", e);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});
		listView.setDrawingCacheEnabled(true);
		listView.setOnKeyListener(onKeyListener);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				context.showCover(false);
				context.reloadAllGalleries();
			}

		});
		Button okButton = (Button) dialog.findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				context.showCover(false);
				dialog.dismiss();
				context.reloadAllGalleries();
			}

		});
		context.showCover(true);
		dialog.show();
		Analytics.logEvent(Analytics.DIALOG_CHANGE_ROW_ORDER);
	}
	
	/**
	 * Change the name of the current row
	 * 
	 * @param context
	 */
	public static void displayChangeRowName(final Launcher context) {
		// Set an EditText view to get user input 
		final EditText input = new EditText(context);
		new AlertDialog.Builder(context)
	    .setTitle(context.getString(R.string.dialog_row_name_title))
	    .setMessage(context.getString(R.string.dialog_row_name))
	    .setView(input)
	    .setPositiveButton(context.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            Editable value = input.getText(); 
	            context.changeCurrentRowName(value.toString());
	        }
	    }).setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    }).show();
	}
}
