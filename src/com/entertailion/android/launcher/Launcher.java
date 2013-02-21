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

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.entertailion.android.launcher.apps.ApplicationInfo;
import com.entertailion.android.launcher.apps.AppsAdapter;
import com.entertailion.android.launcher.appwidget.CellLayout;
import com.entertailion.android.launcher.appwidget.LauncherAppWidgetHost;
import com.entertailion.android.launcher.appwidget.Workspace;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.database.RecentAppsTable;
import com.entertailion.android.launcher.database.RowsTable;
import com.entertailion.android.launcher.item.ItemAdapter;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.row.RowInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.Utils;
import com.entertailion.android.launcher.widget.Clock;
import com.entertailion.android.launcher.widget.CustomAdapterView;
import com.entertailion.android.launcher.widget.CustomAdapterView.OnItemClickListener;
import com.entertailion.android.launcher.widget.CustomAdapterView.OnItemLongClickListener;
import com.entertailion.android.launcher.widget.CustomAdapterView.OnItemSelectedListener;
import com.entertailion.android.launcher.widget.EcoGallery;
import com.entertailion.android.launcher.widget.GalleryAdapter;
import com.entertailion.android.launcher.widget.ObservableScrollView;
import com.entertailion.android.launcher.widget.RowGallery;
import com.entertailion.android.launcher.widget.ScrollViewListener;
import com.entertailion.android.launcher.widget.Weather;

/**
 * Home activity for the launcher. The launcher is mostly transparent to show
 * the live TV. There is a top bar that displays the weather and clock. The
 * bottom bar displays rows that can be navigated with the d-pad/arrow keys on a
 * remote control. There is at least a recent apps row and one favorite apps
 * row. The user can use the menu key to add more apps/rows and change settings.
 * 
 * @author leon_nicholls
 * 
 */
public class Launcher extends Activity implements OnItemSelectedListener, OnItemClickListener, OnItemLongClickListener, ScrollViewListener {
	private static final String LOG_TAG = "Launcher";

	private static final String KEY_SAVE_GALLERY_ROW = "gallery.row";
	private static final String KEY_SAVE_GALLERY_ITEM = "gallery.item";

	public static final String PREFERENCES_NAME = "preferences";
	public static final String FIRST_INSTALL = "first_install";
	public static final String FIRST_MOVE = "first_move";

	public static final String SHORTCUTS_INTENT = "com.entertailion.android.launcher.SHORTCUTS_INTENT";

	private static final int REQUEST_PICK_APPWIDGET = 1;
	private static final int REQUEST_CREATE_APPWIDGET = 2;

	// Identifiers for menu items
	private static final int MENU_SETTINGS = Menu.FIRST + 1;
	private static final int MENU_ABOUT = MENU_SETTINGS + 1;
	private static final int MENU_ADD_APP = MENU_ABOUT + 1;
	private static final int MENU_ADD_SPOTLIGHT_WEB_APP = MENU_ADD_APP + 1;
	private static final int MENU_ADD_BROWSER_BOOKMARK = MENU_ADD_SPOTLIGHT_WEB_APP + 1;
	private static final int MENU_DELETE_ITEM = MENU_ADD_BROWSER_BOOKMARK + 1;
	private static final int MENU_DELETE_ROW = MENU_DELETE_ITEM + 1;
	private static final int MENU_UNINSTALL_APP = MENU_DELETE_ROW + 1;
	private static final int MENU_SYSTEM_SETTINGS = MENU_UNINSTALL_APP + 1;
	private static final int MENU_MOVE_ITEM = MENU_SYSTEM_SETTINGS + 1;
	private static final int MENU_CHANGE_ROW_ORDER = MENU_MOVE_ITEM + 1;
	private static final int MENU_ADD_WIDGET = MENU_CHANGE_ROW_ORDER + 1;
	private static final int MENU_CHANGE_ROW_NAME = MENU_ADD_WIDGET + 1;

	private static final int UPDATE_ITEM_STATUS = 1;

	private static final int ITEM_CLICK_ANIMATION_DELAY = 100;
	private static final int UPDATE_STATUS_DELAY = 300;
	private final static float COVER_ALPHA_VALUE = 0.8f;

	private static final int APPWIDGET_HOST_ID = 1023;

	private ImageView topGradient, bottomGradient, selector, arrowUp, arrowDown, coverImageView, menuImageView;
	private RowGallery recentsGallery, currentGallery;
	private int currentGalleryRow;
	private TextView itemName, layerName;
	private String lastItemName, lastLayerName;
	private Handler handler = new Handler();
	private Handler updateStatusHandler = new Handler(new UpdateStatusCallback());
	private ObservableScrollView scrollView;
	private ViewGroup scrollViewContent;
	private boolean infiniteScrolling = false; // TODO not working completely
	private ServiceConnection launcherServiceConnection;
	private LauncherService launcherService;
	private boolean handledLongClick = false; // short-circuit for item clicks
	private SharedPreferences preferences;
	private Clock clock;
	private Weather weather;
	private Animation fadeIn, fadeOut, zoom;
	private boolean displayRowName, displayItemName;
	private boolean isMovingItem;
	private int movingRow = 0;
	private int movingPosition = 0;
	private boolean isSwitching;
	private MenuItem aboutMenuItem;
	private AppWidgetManager appWidgetManager;
	private LauncherAppWidgetHost appWidgetHost;
	private CellLayout.CellInfo addItemCellInfo;
	private Workspace workspace;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		connectToLauncherService();

		Typeface lightTypeface = ((LauncherApplication) getApplicationContext()).getLightTypeface(this);
		Typeface italicTypeface = ((LauncherApplication) getApplicationContext()).getItalicTypeface(this);

		preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.dock);

		appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
		appWidgetHost.startListening();

		itemName = (TextView) findViewById(R.id.name);
		itemName.setTypeface(lightTypeface);
		lastItemName = "";
		layerName = (TextView) findViewById(R.id.layer);
		layerName.setTypeface(italicTypeface);
		lastLayerName = "";
		clock = (Clock) findViewById(R.id.clock);
		weather = (Weather) findViewById(R.id.weather);

		topGradient = (ImageView) findViewById(R.id.top_gradient);
		bottomGradient = (ImageView) findViewById(R.id.bottom_gradient);
		selector = (ImageView) findViewById(R.id.selector);
		scrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
		scrollView.setScrollViewListener(this);
		arrowUp = (ImageView) findViewById(R.id.arrow_up);
		arrowUp.setVisibility(View.INVISIBLE);
		arrowDown = (ImageView) findViewById(R.id.arrow_down);
		arrowDown.setVisibility(View.INVISIBLE);
		scrollViewContent = (ViewGroup) findViewById(R.id.scroll_view_content);
		coverImageView = (ImageView) findViewById(R.id.cover);
		menuImageView = (ImageView) findViewById(R.id.menu);

		fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		zoom = AnimationUtils.loadAnimation(this, R.anim.zoom);

		handleIntent(getIntent());

		// Set the context for Google Analytics
		Analytics.createAnalytics(this);
		Utils.logDeviceInfo(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Start Google Analytics for this activity
		Analytics.startAnalytics(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Stop Google Analytics for this activity
		Analytics.stopAnalytics(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// Close the menu
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			getWindow().closeAllPanels();
		}
		handleIntent(intent);
	}

	/**
	 * Handle the incoming shortcut intent generated by InstallShortcutReceiver.
	 * Let the user select which row to add the new shortcut.
	 * 
	 * @param intent
	 */
	private void handleIntent(Intent intent) {
		Log.d(LOG_TAG, "handleIntent: " + intent);
		if (intent != null && intent.getAction() != null) {
			if (intent.getAction().equals(SHORTCUTS_INTENT)) {
				if (intent.getExtras() != null) {
					String name = intent.getExtras().getString("name");
					String icon = intent.getExtras().getString("icon");
					String uri = intent.getDataString();
					Dialogs.displayShortcutsRowSelection(Launcher.this, name, icon, uri);
					return;
				}
			}
		}
		// boot up flag: 0x10800000
		if (intent.getFlags() == (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)) {
			boolean liveTV = preferences.getBoolean(PreferencesActivity.GENERAL_LIVE_TV, true);
			if (liveTV) {
				Utils.launchLiveTV(this);
				finish();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		disconnectFromLauncherService();
	}

	/**
	 * Connect to the background service.
	 */
	private void connectToLauncherService() {
		launcherServiceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(LOG_TAG, "onServiceConnected");
				launcherService = ((LauncherService.LocalBinder) service).getService();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.d(LOG_TAG, "onServiceDisconnected");
				launcherService = null;
			}
		};
		if (launcherServiceConnection != null) {
			Intent intent = new Intent(Launcher.this, LauncherService.class);
			bindService(intent, launcherServiceConnection, BIND_AUTO_CREATE);
		}
	}

	/**
	 * Close the connection to the background service.
	 */
	private synchronized void disconnectFromLauncherService() {
		if (launcherServiceConnection != null) {
			unbindService(launcherServiceConnection);
			launcherServiceConnection = null;
		}
		launcherService = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		topGradient.clearAnimation();
		// Persist the current row and selected item
		SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		try {
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(KEY_SAVE_GALLERY_ROW, currentGalleryRow);
			editor.putInt(KEY_SAVE_GALLERY_ITEM, currentGallery.getSelectedItemPosition());
			editor.commit();
		} catch (Exception e) {
			Log.d(LOG_TAG, "onPause", e);
		}
		stopMoveItem();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Reset the user interface components
		reloadAllGalleries();

		// For first time install show the introduction dialog with some user
		// instructions
		SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		boolean firstInstall = settings.getBoolean(FIRST_INSTALL, true);
		if (firstInstall) {
			try {
				Dialogs.displayIntroduction(this);

				// persist not to show introduction again
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(FIRST_INSTALL, false);
				editor.commit();
			} catch (Exception e) {
				Log.d(LOG_TAG, "first install", e);
			}
		} else {
			Dialogs.displayRating(this);
		}
		// restore the persisted row and selected item
		int row = settings.getInt(KEY_SAVE_GALLERY_ROW, currentGalleryRow);
		int position = settings.getInt(KEY_SAVE_GALLERY_ITEM, currentGallery.getSelectedItemPosition());
		View child = scrollViewContent.getChildAt(row);
		if (child != null) {
			// TODO not working...
			// scrollViewContent.scrollBy(0, (int) child.getY());
			// scrollView.arrowScroll(View.FOCUS_DOWN);
			// currentGallery = (RowGallery)child;
			// currentGalleryRow = row;
		}
		// currentGallery.setSelectedItemPosition(position);
		Analytics.logEvent(Analytics.LAUNCHER_HOME);
	}

	/**
	 * Use the configured settings to update the user interface. User can use
	 * the menu key to change the app settings.
	 */
	private void updateUserInterface() {
		showCover(false);
		displayRowName = preferences.getBoolean(PreferencesActivity.ROWS_ROW_NAME, true);
		layerName.setVisibility(displayRowName ? View.VISIBLE : View.INVISIBLE);
		displayItemName = preferences.getBoolean(PreferencesActivity.ROWS_ITEM_NAME, true);
		itemName.setVisibility(displayItemName ? View.VISIBLE : View.INVISIBLE);
		boolean displayClock = preferences.getBoolean(PreferencesActivity.GENERAL_CLOCK, true);
		boolean displayWeather = preferences.getBoolean(PreferencesActivity.GENERAL_WEATHER, Utils.isUsa());
		if (!displayClock && !displayWeather) {
			topGradient.setVisibility(View.INVISIBLE);
			clock.setVisibility(View.INVISIBLE);
			weather.setVisibility(View.INVISIBLE);
		} else {
			topGradient.setVisibility(View.VISIBLE);
			clock.setVisibility(displayClock ? View.VISIBLE : View.INVISIBLE);
			weather.setVisibility(displayWeather ? View.VISIBLE : View.INVISIBLE);
		}
		// display menu hint
		fadeOut.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				menuImageView.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}

		});
		menuImageView.startAnimation(fadeOut);
	}

	/**
	 * Bind the list of recent apps with the row in the user interface.
	 */
	private void bindRecents() {
		ArrayList<ApplicationInfo> recents = ((LauncherApplication) getApplicationContext()).getRecents();
		AppsAdapter adapter = new AppsAdapter(this, recents, infiniteScrolling);
		if (recentsGallery == null) {
			recentsGallery = new RowGallery(this, -1, getString(R.string.layer_recent_apps), adapter);
			scrollViewContent.addView(recentsGallery);
		} else {
			recentsGallery.setAdapter(adapter);
		}
	}

	/**
	 * Bind the rows of favorite apps configured by the user.
	 */
	private void bindItems() {
		try {
			ArrayList<RowInfo> rows = RowsTable.getRows(this);
			if (rows != null) {
				// Get the items for each row
				for (RowInfo row : rows) {
					Log.d(LOG_TAG, "row=" + row.getPosition());
					try {
						ArrayList<ItemInfo> rowItems = ItemsTable.getItems(this, row.getId());
						mapApplicationIcons(rowItems);
						ItemAdapter adapter = new ItemAdapter(this, rowItems, infiniteScrolling);
						RowGallery gallery = new RowGallery(this, row.getId(), row.getTitle().toUpperCase(), adapter);
						scrollViewContent.addView(gallery, 0);
					} catch (Exception e) {
						Log.e(LOG_TAG, "bindItems", e);
					}
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "bindItems", e);
		}
	}

	/**
	 * Invoke the selected app.
	 * 
	 * @see com.entertailion.android.launcher.widget.CustomAdapterView.OnItemClickListener#onItemClick(com.entertailion.android.launcher.widget.CustomAdapterView,
	 *      android.view.View, int, long)
	 */
	@Override
	public void onItemClick(CustomAdapterView<?> gallery, View dockView, int pos, long arg3) {
		if (isMovingItem) {
			stopMoveItem();
		} else if (!handledLongClick) {
			if (dockView != null) {
				// dockView.startAnimation(zoom); // TODO why not working?
			}
			selector.setImageResource(R.drawable.gallery_item_down);
			itemName.setTextColor(getResources().getColor(R.color.app_name_selected));
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					selector.setImageResource(R.drawable.gallery_item_over);
					itemName.setTextColor(getResources().getColor(R.color.app_name));
				}

			}, ITEM_CLICK_ANIMATION_DELAY);

			if (infiniteScrolling) {
				pos = pos % currentGallery.getAdapter().getRealCount();
			}
			try {
				ItemInfo itemInfo = (ItemInfo) gallery.getItemAtPosition(pos);
				itemInfo.invoke(this);
				if (itemInfo instanceof ApplicationInfo) {
					ApplicationInfo applicationInfo = (ApplicationInfo) itemInfo;
					RecentAppsTable.persistRecentApp(this, applicationInfo);
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "onItemClick", e);
			}
		}
		handledLongClick = false;
	}

	/**
	 * User can use a long click to invoke the menu.
	 * 
	 * @see onCreateContextMenu
	 * 
	 * @see com.entertailion.android.launcher.widget.CustomAdapterView.OnItemLongClickListener#onItemLongClick(com.entertailion.android.launcher.widget.CustomAdapterView,
	 *      android.view.View, int, long)
	 */
	@Override
	public boolean onItemLongClick(CustomAdapterView<?> gallery, View dockView, int arg, long pos) {
		handledLongClick = true; // short circuit item clicks
		return false;
	}

	/**
	 * Update the item name as the user navigates within a gallery row.
	 * 
	 * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView,
	 *      android.view.View, int, long)
	 */
	public void onItemSelected(CustomAdapterView<?> gallery, View dockView, int pos, long arg3) {
		if (isMovingItem) {
			moveItem(movingPosition, pos);
			movingPosition = pos;
		} else {
			updateItemStatus();
		}
	}

	/**
	 * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
	 */
	public void onNothingSelected(CustomAdapterView<?> gallery) {

	}

	/**
	 * Create the context menu for the gallery item long click.
	 * 
	 * @see onItemLongClick
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 *      android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu contextMenu, View v, ContextMenu.ContextMenuInfo menuInfo) {

		createMenu(contextMenu, false);

		MenuItem deleteRow = contextMenu.findItem(MENU_DELETE_ROW);
		updateMenuDeleteRow(deleteRow);
		MenuItem deleteItem = contextMenu.findItem(MENU_DELETE_ITEM);
		updateMenuDeleteItem(deleteItem);
		MenuItem moveItem = contextMenu.findItem(MENU_MOVE_ITEM);
		updateMenuMoveItem(moveItem);
		MenuItem changeRowOrderItem = contextMenu.findItem(MENU_CHANGE_ROW_ORDER);
		updateMenuChangeRowOrder(changeRowOrderItem);
		MenuItem changeRowNameItem = contextMenu.findItem(MENU_CHANGE_ROW_NAME);
		updateMenuChangeRowName(changeRowNameItem);
		MenuItem uninstallApp = contextMenu.findItem(MENU_UNINSTALL_APP);
		updateMenuUninstallApp(uninstallApp);
	}

	/**
	 * Utility method to set the state of the delete row menu option.
	 * 
	 * @param deleteRow
	 */
	private void updateMenuDeleteRow(MenuItem deleteRow) {
		if (deleteRow != null) {
			if (currentGallery == recentsGallery) {
				// cannot delete recents row
				deleteRow.setEnabled(false);
			} else if (scrollViewContent.getChildCount() == 2) {
				// cannot delete last custom row
				deleteRow.setEnabled(false);
			} else {
				deleteRow.setEnabled(true);
			}
		}
	}

	/**
	 * Utility method to set the state of the delete item menu option.
	 * 
	 * @param deleteItem
	 */
	private void updateMenuDeleteItem(MenuItem deleteItem) {
		if (deleteItem != null) {
			if (currentGallery == recentsGallery) {
				// cannot remote item from recents row
				deleteItem.setEnabled(false);
			} else if (scrollViewContent.getChildCount() == 2 && currentGallery.getAdapter().getCount() == 1) {
				// cannot delete last item of last custom row
				deleteItem.setEnabled(false);
			} else {
				deleteItem.setEnabled(true);
			}
		}
	}

	/**
	 * Utility method to set the state of the move item menu option.
	 * 
	 * @param moveItem
	 */
	private void updateMenuMoveItem(MenuItem moveItem) {
		if (moveItem != null) {
			if (currentGallery == recentsGallery) {
				// cannot move item from recents row
				moveItem.setEnabled(false);
			} else {
				moveItem.setEnabled(true);
			}
		}
	}

	/**
	 * Utility method to set the state of the change row order menu option.
	 * 
	 * @param moveItem
	 */
	private void updateMenuChangeRowOrder(MenuItem changeRowOrderItem) {
		if (changeRowOrderItem != null) {
			if (currentGallery == recentsGallery) {
				// cannot move item from recents row
				changeRowOrderItem.setEnabled(false);
			} else {
				ArrayList<RowInfo> rows = RowsTable.getRows(this);
				if (rows != null && rows.size() > 1) {
					changeRowOrderItem.setEnabled(true);
				} else {
					changeRowOrderItem.setEnabled(false);
				}
			}
		}
	}

	/**
	 * Utility method to set the state of the change row name menu option.
	 * 
	 * @param moveItem
	 */
	private void updateMenuChangeRowName(MenuItem changeRowNameItem) {
		if (changeRowNameItem != null) {
			if (currentGallery == recentsGallery) {
				// cannot move item from recents row
				changeRowNameItem.setEnabled(false);
			} else {
				changeRowNameItem.setEnabled(true);
			}
		}
	}

	/**
	 * Utility method to set the state of the uninstall app menu option.
	 * 
	 * @param uninstallApp
	 */
	private void updateMenuUninstallApp(MenuItem uninstallApp) {
		if (uninstallApp != null) {
			if (currentGallery == recentsGallery) {
				// cannot remote item from recents row
				uninstallApp.setEnabled(false);
			} else {
				// can only uninstall Android apps
				int position = currentGallery.getSelectedItemPosition();
				if (infiniteScrolling) {
					position = position % currentGallery.getAdapter().getRealCount();
				}
				ItemInfo itemInfo = (ItemInfo) currentGallery.getAdapter().getItem(position);
				if (itemInfo instanceof ApplicationInfo) {
					uninstallApp.setEnabled(true);
				} else {
					uninstallApp.setEnabled(false);
				}
			}
		}
	}

	/**
	 * Handle the context menu selections. Invoked by the user when pressing the
	 * menu key.
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (handleMenuItemSelected(item.getItemId())) {
			return true;
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * Set the menu item states just before it is shown to the user.
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean showItems = false;
		if (currentGallery != null && currentGallery != recentsGallery) {
			showItems = true;
		}
		Log.d(LOG_TAG, "onPrepareOptionsMenu: " + showItems);
		MenuItem addApp = menu.findItem(MENU_ADD_APP);
		if (addApp != null) {
			addApp.setVisible(showItems);
		}
		MenuItem addSpotlightWebApp = menu.findItem(MENU_ADD_SPOTLIGHT_WEB_APP);
		if (addSpotlightWebApp != null) {
			addSpotlightWebApp.setVisible(showItems);
		}
		MenuItem addBrowserBookmarkWebApp = menu.findItem(MENU_ADD_BROWSER_BOOKMARK);
		if (addBrowserBookmarkWebApp != null) {
			addBrowserBookmarkWebApp.setVisible(showItems);
		}
		MenuItem deleteItem = menu.findItem(MENU_DELETE_ITEM);
		if (deleteItem != null) {
			deleteItem.setVisible(showItems);
			updateMenuDeleteItem(deleteItem);
		}
		MenuItem deleteRow = menu.findItem(MENU_DELETE_ROW);
		if (deleteRow != null) {
			deleteRow.setVisible(showItems);
			updateMenuDeleteRow(deleteRow);
		}
		MenuItem moveItem = menu.findItem(MENU_MOVE_ITEM);
		if (moveItem != null) {
			moveItem.setVisible(showItems);
			updateMenuMoveItem(moveItem);
		}
		MenuItem changeRowOrderItem = menu.findItem(MENU_CHANGE_ROW_ORDER);
		if (changeRowOrderItem != null) {
			changeRowOrderItem.setVisible(showItems);
			updateMenuChangeRowOrder(changeRowOrderItem);
		}
		MenuItem changeRowNameItem = menu.findItem(MENU_CHANGE_ROW_NAME);
		if (changeRowNameItem != null) {
			changeRowNameItem.setVisible(showItems);
			updateMenuChangeRowName(changeRowNameItem);
		}
		MenuItem uninstallApp = menu.findItem(MENU_UNINSTALL_APP);
		if (uninstallApp != null) {
			uninstallApp.setVisible(showItems);
			updateMenuUninstallApp(uninstallApp);
		}

		if (aboutMenuItem != null) {
			// a bit of fun: randomly change about menu icon
			int index = (int) (Math.random() * 8);
			int resource = R.drawable.ic_menu_clipboard;
			switch (index) {
			case 0:
				resource = R.drawable.ic_menu_rattle;
				break;
			case 1:
				resource = R.drawable.ic_menu_pizza;
				break;
			case 2:
				resource = R.drawable.ic_menu_lollipop;
				break;
			case 3:
				resource = R.drawable.ic_menu_cupcake;
				break;
			case 4:
				resource = R.drawable.ic_menu_rocket;
				break;
			case 5:
				resource = R.drawable.ic_menu_glasses;
				break;
			case 6:
				resource = R.drawable.ic_menu_cone;
				break;
			case 7:
				resource = R.drawable.ic_menu_heart;
				break;
			}
			aboutMenuItem.setIcon(resource);
		}
		return true;
	}

	/**
	 * Create the menu. Invoked by the user by pressing the menu key.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		createMenu(menu, true);

		return true;
	}

	/**
	 * Handle the menu selections.
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (handleMenuItemSelected(item.getItemId())) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Utility method to create the menu options.
	 * 
	 * @param menu
	 * @param addAllMenuItems
	 *            if true all the menu options are added. For the recent apps
	 *            row, the edit menu options are now displayed.
	 */
	private void createMenu(Menu menu, boolean addAllMenuItems) {
		Log.d(LOG_TAG, "createMenu=" + currentGallery);
		if (!addAllMenuItems) {
			if (currentGallery != null) {
				if (currentGallery != recentsGallery) {
					addAllMenuItems = true;
				}
			}
		}
		if (addAllMenuItems) {
			menu.add(0, MENU_ADD_APP, 0, R.string.menu_add_app).setIcon(R.drawable.ic_menu_add);
			menu.add(0, MENU_ADD_SPOTLIGHT_WEB_APP, 0, R.string.menu_add_spotlight_web_app).setIcon(R.drawable.ic_menu_add);
			// TODO can't add this feature until find better way to get high-res
			// icons for most web sites
			// menu.add(0, MENU_ADD_BROWSER_BOOKMARK, 0,
			// R.string.menu_add_browser_bookmark).setIcon(R.drawable.ic_menu_add);
			menu.add(0, MENU_MOVE_ITEM, 0, R.string.menu_move_item).setIcon(R.drawable.ic_menu_left_right);
			menu.add(0, MENU_CHANGE_ROW_ORDER, 0, R.string.menu_change_row_order).setIcon(R.drawable.ic_menu_up_down);
			menu.add(0, MENU_CHANGE_ROW_NAME, 0, R.string.menu_change_row_name).setIcon(R.drawable.ic_menu_row_name);
			menu.add(0, MENU_DELETE_ITEM, 0, R.string.menu_delete_item).setIcon(R.drawable.ic_menu_delete);
			menu.add(0, MENU_DELETE_ROW, 0, R.string.menu_delete_row).setIcon(R.drawable.ic_menu_delete);
			menu.add(0, MENU_UNINSTALL_APP, 0, R.string.menu_uninstall_app).setIcon(R.drawable.ic_menu_trash);
		}
		// TODO widget not ready yet
		// /menu.add(0, MENU_ADD_WIDGET, 0,
		// R.string.menu_add_widget).setIcon(R.drawable.ic_menu_widget).setAlphabeticShortcut('W');
		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(R.drawable.ic_menu_settings).setAlphabeticShortcut('S');
		menu.add(0, MENU_SYSTEM_SETTINGS, 0, R.string.menu_system_settings).setIcon(R.drawable.ic_menu_settings);

		aboutMenuItem = menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		aboutMenuItem.setIcon(R.drawable.ic_menu_clipboard).setAlphabeticShortcut('A');
	}

	/**
	 * Handle the menu option selected by the user.
	 * 
	 * @param id
	 * @return
	 */
	private boolean handleMenuItemSelected(int id) {
		switch (id) {
		case MENU_ABOUT:
			Dialogs.displayAbout(this);
			return true;
		case MENU_ADD_APP:
			ArrayList<ApplicationInfo> applications = ((LauncherApplication) getApplicationContext()).getApplications();
			Dialogs.displayAddApps(this, applications);
			return true;
		case MENU_DELETE_ITEM:
			handleDeleteItem();
			return true;
		case MENU_MOVE_ITEM:
			handleMoveItem();
			return true;
		case MENU_CHANGE_ROW_ORDER:
			Dialogs.displayChangeRowOrder(this);
			return true;
		case MENU_CHANGE_ROW_NAME:
			Dialogs.displayChangeRowName(this);
			return true;
		case MENU_DELETE_ROW:
			Dialogs.displayDeleteRow(this);
			return true;
		case MENU_UNINSTALL_APP:
			ItemInfo itemInfo = (ItemInfo) currentGallery.getAdapter().getItem(currentGallery.getSelectedItemPosition());
			Uri packageURI = Uri.parse("package:" + itemInfo.getIntent().getComponent().getPackageName());
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
			startActivity(uninstallIntent);
			Analytics.logEvent(Analytics.UNINSTALL_APP);
			return true;
		case MENU_ADD_SPOTLIGHT_WEB_APP:
			Dialogs.displayAddSpotlight(this);
			return true;
		case MENU_ADD_BROWSER_BOOKMARK:
			Dialogs.displayAddBrowserBookmark(this);
			return true;
		case MENU_SETTINGS:
			Intent intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			Analytics.logEvent(Analytics.SETTINGS);
			return true;
		case MENU_SYSTEM_SETTINGS:
			Utils.showSystemSettings(this);
			Analytics.logEvent(Analytics.SYSTEM_SETTINGS);
			return true;
		case MENU_ADD_WIDGET:
			int appWidgetId = appWidgetHost.allocateAppWidgetId();
			Intent pickWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
			pickWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			startActivityForResult(pickWidgetIntent, REQUEST_PICK_APPWIDGET);
			Analytics.logEvent(Analytics.WIDGET_PICK);
			return true;
		}
		return false;
	}

	/**
	 * The user has selected to delete an item in a gallery row.
	 */
	private void handleDeleteItem() {
		// if only one item, delete the row instead
		if (currentGallery.getAdapter().getCount() == 1) {
			Dialogs.displayDeleteRow(this);
		} else {
			Dialogs.displayDeleteItem(this);
		}
	}

	/**
	 * The user has selected to move an item in a gallery row.
	 */
	private void handleMoveItem() {
		// For first time move show the instructions
		// instructions
		SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
		boolean firstMove = settings.getBoolean(FIRST_MOVE, true);
		if (firstMove) {
			try {
				final Dialog dialog = new Dialog(this);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.alert);

				final TextView alertTextView = (TextView) dialog.findViewById(R.id.alertText);
				alertTextView.setText(getString(R.string.menu_move_item_instuctions));
				Button alertButton = (Button) dialog.findViewById(R.id.alertButton);
				alertButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						showCover(false);
						dialog.dismiss();

						startMoveItem();
					}

				});
				dialog.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						showCover(false);
					}

				});
				showCover(true);
				dialog.show();

				// persist not to show introduction again
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(FIRST_MOVE, false);
				editor.commit();
			} catch (Exception e) {
				Log.d(LOG_TAG, "first move", e);
			}
		} else {
			startMoveItem();
		}
	}

	private void startMoveItem() {
		isMovingItem = true;
		movingPosition = currentGallery.getSelectedItemPosition();
		movingRow = currentGalleryRow;
		isSwitching = false;
		currentGallery.setAnimation(false);
		PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.YELLOW, Mode.SRC_IN);
		selector.setColorFilter(filter);
		Analytics.logEvent(Analytics.MOVE_ITEM);
	}

	private void stopMoveItem() {
		isMovingItem = false;
		isSwitching = false;
		currentGallery.setAnimation(true);
		selector.setColorFilter(null);
	}

	/**
	 * Track the scroll view arrow key movements. The rows of galleries are
	 * inside of the vertical scroll view.
	 * 
	 * @see com.entertailion.android.launcher.widget.ScrollViewListener#arrowScroll(int)
	 */
	public void arrowScroll(int direction) {
		stopMoveItem();
		int previousGalleryRow = currentGalleryRow;
		currentGalleryRow = scrollView.getLevel();
		if (previousGalleryRow != currentGalleryRow) {
			updateStatusHandler.removeMessages(UPDATE_ITEM_STATUS);
			updateStatus();
		}
	}

	/**
	 * Update the status text for the row name and the selected item name.
	 */
	private void updateStatus() {
		currentGallery = getCurrentGallery();
		doRowStatus();
		updateItemStatus();
	}

	/**
	 * Get the currently selected gallery row in the scroll view.
	 * 
	 * @return
	 */
	private RowGallery getCurrentGallery() {
		Log.d(LOG_TAG, "currentGalleryRow=" + currentGalleryRow);
		if (currentGalleryRow == scrollViewContent.getChildCount()) {
			return recentsGallery;
		}
		return (RowGallery) scrollViewContent.getChildAt(currentGalleryRow);
	}

	/**
	 * Update the row name and the arrow button hints.
	 */
	private void doRowStatus() {
		if (displayRowName) {
			if (currentGallery != null) {
				String layer = (String) currentGallery.getTag(R.id.gallery_title);
				if (layerName != null) {
					if (layer != null && !lastLayerName.equals(layer)) {
						layerName.setText(layer);
						lastLayerName = layer;
					}
				}
				arrowUp.setVisibility(View.VISIBLE);
				arrowDown.setVisibility(View.VISIBLE);
				if (currentGalleryRow == 0) {
					arrowUp.setVisibility(View.INVISIBLE);
				} else if (currentGallery == recentsGallery) {
					arrowDown.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	/**
	 * Update the item name and layer name. It is critical that this is done as
	 * quickly as possible otherwise the gallery animation will not be smooth.
	 * 
	 * @param gallery
	 */
	private void updateItemStatus(final EcoGallery... gallery) {
		if (displayItemName) {
			updateStatusHandler.removeMessages(UPDATE_ITEM_STATUS);
			handler.post(new Runnable() {
				public void run() {
					// while the gallery animation is active hide the item name
					itemName.setVisibility(View.INVISIBLE);
				}
			});
			// delay the selected item name update
			updateStatusHandler.sendEmptyMessageDelayed(UPDATE_ITEM_STATUS, UPDATE_STATUS_DELAY);
		}
	}

	/**
	 * Update the current layer name and the currently selected item in the
	 * gallery
	 * 
	 * @param gallery
	 */
	private void doItemStatus(final EcoGallery... gallery) {
		if (currentGallery != null) {
			GalleryAdapter<ItemInfo> adapter = (GalleryAdapter<ItemInfo>) currentGallery.getAdapter();
			int position = currentGallery.getSelectedItemPosition();
			if (infiniteScrolling) {
				position = position % adapter.getRealCount();
			}
			ItemInfo itemInfo = adapter.getItem(position);
			String name = itemInfo.getTitle();

			if (itemName != null) {
				if (name != null && !name.equals(lastItemName)) {
					itemName.setText(name);
					lastItemName = name;
				}
			}
			itemName.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Handler callback for delayed selected item name updates.
	 * 
	 * @see updateItemStatus
	 * 
	 */
	private class UpdateStatusCallback implements Handler.Callback {

		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ITEM_STATUS:
				doItemStatus();
				return true;
			}
			return false;
		}
	}

	/**
	 * Add a new item to a gallery row.
	 * 
	 * @param itemInfo
	 * @param rowName
	 *            the name of the new row or null if existing row
	 */
	public void addItem(ItemInfo itemInfo, String rowName) {
		Log.d(LOG_TAG, "addItem: " + itemInfo.getTitle() + ", " + rowName + ", " + currentGalleryRow);
		if (rowName != null) {
			// create a new row
			try {
				int rowId = DatabaseHelper.NO_ID;
				ArrayList<RowInfo> rows = RowsTable.getRows(this);
				if (rows != null) {
					int counter = 0;
					// adjust the positions of existing rows
					int mappedGalleryRow = rows.size() - currentGalleryRow - 1;
					for (int i = 0; i < rows.size(); i++) {
						RowInfo row = rows.get(i);
						RowsTable.updateRow(this, row.getId(), row.getTitle(), counter, row.getType());
						counter++;
						if (i == mappedGalleryRow) {
							rowId = (int) RowsTable.insertRow(this, rowName, mappedGalleryRow + 1, RowInfo.FAVORITE_TYPE);
							counter++;
						}
					}
				}
				itemInfo.persistInsert(this, rowId, 0);

				ArrayList<ItemInfo> rowItems = ItemsTable.getItems(this, rowId);
				mapApplicationIcons(rowItems);
				ItemAdapter adapter = new ItemAdapter(this, rowItems, infiniteScrolling);
				currentGallery = new RowGallery(this, rowId, rowName.toUpperCase(), adapter);
				scrollViewContent.addView(currentGallery, currentGalleryRow);

				updateStatus();
			} catch (Exception e) {
				Log.d(LOG_TAG, "addItem", e);
			}
		} else {
			int position = currentGallery.getSelectedItemPosition();
			try {
				int rowId = (Integer) currentGallery.getTag(R.id.row_id);
				Log.d(LOG_TAG, "rowId=" + rowId);
				GalleryAdapter<ItemInfo> adapter = (GalleryAdapter<ItemInfo>) currentGallery.getAdapter();
				int counter = 0;
				// adjust the positions of the existing items
				for (int i = 0; i < adapter.getCount(); i++) {
					int pos = i;
					if (infiniteScrolling) {
						pos = pos % adapter.getRealCount();
					}
					ItemInfo currentItemInfo = (ItemInfo) adapter.getItem(pos);
					// add the new item in the right position
					if (i == position) {
						itemInfo.persistInsert(this, rowId, position);
						counter++;
					}
					currentItemInfo.persistUpdate(this, rowId, counter);
					counter++;
				}
				// get the persisted list of items for the current row
				ArrayList<ItemInfo> rowItems = ItemsTable.getItems(this, rowId);
				mapApplicationIcons(rowItems);
				adapter = new ItemAdapter(this, rowItems, infiniteScrolling);
				currentGallery.setAdapter(adapter);
				currentGallery.setSelectedItemPosition(position);
			} catch (Exception e) {
				Log.e(LOG_TAG, "addItem", e);
			}
		}
	}

	/**
	 * Moves an item.
	 * 
	 * @param fromPosition
	 * @param toPosition
	 */
	public void moveItem(int fromPosition, int toPosition) {
		Log.d(LOG_TAG, "moveItem: " + fromPosition + ", " + toPosition);
		if (!isSwitching && Math.abs(fromPosition - toPosition) == 1) {
			isSwitching = true;
			try {
				int rowId = (Integer) currentGallery.getTag(R.id.row_id);
				Log.d(LOG_TAG, "rowId=" + rowId);
				GalleryAdapter<ItemInfo> adapter = (GalleryAdapter<ItemInfo>) currentGallery.getAdapter();
				ItemInfo fromItemInfo = (ItemInfo) adapter.getItem(fromPosition);
				fromItemInfo.persistUpdate(this, rowId, toPosition);
				ItemInfo toItemInfo = (ItemInfo) adapter.getItem(toPosition);
				toItemInfo.persistUpdate(this, rowId, fromPosition);

				reloadGalleries(rowId);
			} catch (Exception e) {
				Log.d(LOG_TAG, "moveItem", e);
			}
			isSwitching = false;
		}
	}

	/**
	 * Give the items their application icons.
	 * 
	 * @param items
	 */
	private void mapApplicationIcons(ArrayList<ItemInfo> items) {
		if (items != null) {
			for (ItemInfo itemInfo : items) {
				if (itemInfo instanceof ApplicationInfo) {
					ApplicationInfo applicationInfo = (ApplicationInfo) itemInfo;
					if (applicationInfo.getDrawable() == null) {
						applicationInfo.setDrawable(getApplicationDrawable(itemInfo.getIntent()));
						applicationInfo.setFiltered(true); // already resized
															// icon
					}
				}
			}
		}
	}

	/**
	 * Find the drawable for a particular application intent
	 * 
	 * @param intent
	 * @return
	 */
	private Drawable getApplicationDrawable(Intent intent) {
		if (intent != null) {
			ArrayList<ApplicationInfo> applications = ((LauncherApplication) getApplicationContext()).getApplications();
			for (ApplicationInfo applicationInfo : applications) {
				if (applicationInfo.getIntent() != null
						&& applicationInfo.getIntent().getComponent().getClassName().equals(intent.getComponent().getClassName())) {
					Drawable icon = applicationInfo.getDrawable();
					if (!applicationInfo.getFiltered()) {
						icon = Utils.createIconThumbnail(icon, this);
						applicationInfo.setDrawable(icon);
						applicationInfo.setFiltered(true);
					}
					return icon;
				}
			}
		}
		return null;
	}

	/**
	 * Handle delete key in gallery
	 */
	public void doDelete() {
		Log.d(LOG_TAG, "doDelete");
		handleDeleteItem();
	}

	/**
	 * Delete the currently selected item in the gallery row.
	 * 
	 */
	public void deleteCurrentItem() {
		Log.d(LOG_TAG, "deleteCurrentItem");
		int position = currentGallery.getSelectedItemPosition();
		try {
			int rowId = (Integer) currentGallery.getTag(R.id.row_id);
			Log.d(LOG_TAG, "rowId=" + rowId);
			GalleryAdapter<ItemInfo> adapter = (GalleryAdapter<ItemInfo>) currentGallery.getAdapter();
			ItemInfo currentItemInfo = (ItemInfo) adapter.getItem(position);
			ItemsTable.deleteItem(this, currentItemInfo.getId());
			// get the persisted list of items for the current row
			ArrayList<ItemInfo> rowItems = ItemsTable.getItems(this, rowId);
			mapApplicationIcons(rowItems);
			adapter = new ItemAdapter(this, rowItems, infiniteScrolling);
			currentGallery.setAdapter(adapter);
			if (position >= rowItems.size()) {
				position = rowItems.size() - 1;
			}
			currentGallery.setSelectedItemPosition(position);
			Analytics.logEvent(Analytics.DELETE_ITEM);
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteCurrentItem", e);
		}
	}

	/**
	 * Delete the currently selected gallery row.
	 */
	public void deleteCurrentRow() {
		Log.d(LOG_TAG, "deleteCurrentRow");
		try {
			int rowId = (Integer) currentGallery.getTag(R.id.row_id);
			Log.d(LOG_TAG, "rowId=" + rowId);
			RowsTable.deleteRow(this, rowId);
			// Adjust other row positions
			ArrayList<RowInfo> rows = RowsTable.getRows(this);
			if (rows != null) {
				for (int i = 0; i < rows.size(); i++) {
					RowInfo row = rows.get(i);
					RowsTable.updateRow(this, row.getId(), row.getTitle(), i, row.getType());
				}
			}

			scrollViewContent.removeViewAt(scrollView.getLevel());
			currentGalleryRow = 0;
			scrollView.scrollTo(0, 0);
			currentGallery = getCurrentGallery();
			updateStatus();
			Analytics.logEvent(Analytics.DELETE_ROW);
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteCurrentRow", e);
		}
	}

	/**
	 * Change the current row name.
	 */
	public void changeCurrentRowName(String name) {
		Log.d(LOG_TAG, "changeCurrentRowName");
		try {
			int rowId = (Integer) currentGallery.getTag(R.id.row_id);
			Log.d(LOG_TAG, "rowId=" + rowId);
			// Adjust other row positions
			ArrayList<RowInfo> rows = RowsTable.getRows(this);
			if (rows != null) {
				for (RowInfo row : rows) {
					if (row.getId() == rowId) {
						RowsTable.updateRow(this, row.getId(), name, row.getPosition(), row.getType());
						break;
					}
				}
			}

			reloadAllGalleries();
			Analytics.logEvent(Analytics.CHANGE_ROW_NAME);
		} catch (Exception e) {
			Log.e(LOG_TAG, "changeCurrentRowName", e);
		}
	}

	/**
	 * Reload the item data for a row.
	 * 
	 * @param rowId
	 */
	public void reloadGalleries(int rowId) {
		int position = currentGallery.getSelectedItemPosition();
		int count = scrollViewContent.getChildCount();
		for (int i = 0; i < count; i++) {
			RowGallery rowGallery = (RowGallery) scrollViewContent.getChildAt(i);
			int id = (Integer) rowGallery.getTag(R.id.row_id);
			if (rowId == id) {
				ArrayList<ItemInfo> rowItems = ItemsTable.getItems(this, rowId);
				mapApplicationIcons(rowItems);
				GalleryAdapter<ItemInfo> adapter = new ItemAdapter(this, rowItems, infiniteScrolling);
				rowGallery.setAdapter(adapter);
				currentGallery.setSelectedItemPosition(position);
				break;
			}
		}
	}

	/**
	 * Reload the data for all the rows.
	 */
	public void reloadAllGalleries() {
		Log.d(LOG_TAG, "reloadAllGalleries");
		scrollViewContent.removeAllViews();
		recentsGallery = null;
		currentGalleryRow = 0;
		updateUserInterface();
		bindItems();
		((LauncherApplication) getApplicationContext()).loadRecents();
		bindRecents();
		updateStatus();
		scrollView.scrollTo(0, 0);
		scrollView.resetScroll();
	}

	/**
	 * Display the cover layer to darken the screen for dialogs.
	 * 
	 * @param isVisible
	 */
	public void showCover(boolean isVisible) {
		if (isVisible) {
			coverImageView.setAlpha(COVER_ALPHA_VALUE);
			coverImageView.setVisibility(View.VISIBLE);
		} else {
			coverImageView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d(LOG_TAG, "onActivityResult=" + requestCode);
		if (resultCode == RESULT_OK && addItemCellInfo != null) {
			switch (requestCode) {
			case REQUEST_PICK_APPWIDGET:
				addAppWidget(intent);
				break;
			case REQUEST_CREATE_APPWIDGET:
				completeAddAppWidget(intent, addItemCellInfo);
				break;
			}
		} else if ((requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET) && resultCode == RESULT_CANCELED && intent != null) {
			// Clean up the appWidgetId if we canceled
			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId != -1) {
				appWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	private void addAppWidget(Intent intent) {
		// TODO: catch bad widget exception when sent
		int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId);

		if (appWidget.configure != null) {
			// Launch over to configure widget, if needed
			Intent configureIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			configureIntent.setComponent(appWidget.configure);
			configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResult(configureIntent, REQUEST_CREATE_APPWIDGET);
		} else {
			// Otherwise just add it
			onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, intent);
		}
	}

	/**
	 * Add a widget to the workspace.
	 * 
	 * @param data
	 *            The intent describing the appWidgetId.
	 * @param cellInfo
	 *            The position on screen where to create the widget.
	 */
	private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		Log.d(LOG_TAG, "dumping extras content=" + extras.toString());

		AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);

		// TODO widgets not ready yet

		// // Calculate the grid spans needed to fit this widget
		// CellLayout layout = (CellLayout)
		// mWorkspace.getChildAt(cellInfo.screen);
		// int[] spans = layout.rectToCell(appWidgetInfo.minWidth,
		// appWidgetInfo.minHeight);
		//
		// // Try finding open space on Launcher screen
		// final int[] xy = mCellCoordinates;
		// if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
		// if (appWidgetId != -1) mAppWidgetHost.deleteAppWidgetId(appWidgetId);
		// return;
		// }
		//
		// // Build Launcher-specific widget info and save to database
		// LauncherAppWidgetInfo launcherInfo = new
		// LauncherAppWidgetInfo(appWidgetId);
		// launcherInfo.spanX = spans[0];
		// launcherInfo.spanY = spans[1];
		//
		// LauncherModel.addItemToDatabase(this, launcherInfo,
		// LauncherSettings.Favorites.CONTAINER_DESKTOP,
		// mWorkspace.getCurrentScreen(), xy[0], xy[1], false);
		//
		// if (!mRestoring) {
		// mDesktopItems.add(launcherInfo);
		//
		// // Perform actual inflation because we're live
		// launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId,
		// appWidgetInfo);
		//
		// launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
		// launcherInfo.hostView.setTag(launcherInfo);
		//
		// mWorkspace.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1],
		// launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());
		// }
	}

	public Handler getHandler() {
		return handler;
	}

	public int getCurrentGalleryId() {
		return (Integer) currentGallery.getTag(R.id.row_id);
	}
}
