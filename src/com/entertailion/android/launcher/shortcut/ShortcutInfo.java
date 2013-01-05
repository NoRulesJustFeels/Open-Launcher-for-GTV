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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.FastBitmapDrawable;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Data structure for shortcuts like browser bookmarks or TV channels.
 * 
 * @see InstallShortcutReceiver
 * 
 * @author leon_nicholls
 * 
 */
public class ShortcutInfo extends ItemInfo {
	private static final String LOG_TAG = "ShortcutInfo";

	private String icon;

	public ShortcutInfo(int position, String title, Intent intent, String icon) {
		this(DatabaseHelper.NO_ID, position, title, intent, icon);
	}

	public ShortcutInfo(int id, int position, String title, Intent intent, String icon) {
		super(id, position, title, intent);
		this.icon = icon;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Override
	public void invoke(Launcher context) {
		super.invoke(context);
		Analytics.logEvent(Analytics.INVOKE_SHORTCUT);
	}

	@Override
	public void renderIcon(ImageView imageView) {

		if (getDrawable() != null) {
			imageView.setImageDrawable(getDrawable());
			return;
		}

		Integer resource = networkIconMap.get(getTitle().toUpperCase());
		if (null != resource) {
			imageView.setImageResource(resource);
			return;
		}

		if (icon != null) {
			try {
				FileInputStream fis = imageView.getContext().openFileInput(icon);
				Bitmap bitmap = BitmapFactory.decodeStream(fis);
				fis.close();
				bitmap = Utils.createBitmapThumbnail(bitmap, imageView.getContext());
				setDrawable(new FastBitmapDrawable(bitmap));
				imageView.setImageDrawable(getDrawable());
				return;
			} catch (Exception e) {
				Log.d(LOG_TAG, "renderIcon", e);
			}
		}

		super.renderIcon(imageView);
	}

	@Override
	public void persistInsert(Context context, int rowId, int position) throws Exception {
		ItemsTable.insertItem(context, rowId, position, getTitle(), getIntent(), icon, DatabaseHelper.SHORTCUT_TYPE);
	}

	@Override
	public void persistUpdate(Context context, int rowId, int position) throws Exception {
		ItemsTable.updateItem(context, getId(), rowId, position, getTitle(), getIntent(), icon, DatabaseHelper.SHORTCUT_TYPE);
	}

	@Override
	public String toString() {
		return "Shortcut [title=" + getTitle() + ", intent=" + getIntent() + ", icon=" + getIcon() + "]";
	}

	/**
	 * Cache of local copies of the icons for the USA TV networks. The icons
	 * have been resized/clipped to fit in the gallery views.
	 */
	public static Map<String, Integer> networkIconMap = new HashMap<String, Integer>();
	static {
		networkIconMap.put("ABC", R.drawable.channel_abc);
		networkIconMap.put("CBS", R.drawable.channel_cbs);
		networkIconMap.put("FOX", R.drawable.channel_fox);
		networkIconMap.put("NBC", R.drawable.channel_nbc);
		networkIconMap.put("PBS", R.drawable.channel_pbs);
		networkIconMap.put("UNIVISION", R.drawable.channel_univision);
		networkIconMap.put("CW", R.drawable.channel_cw);
		networkIconMap.put("THE CW NETWORK", R.drawable.channel_cw);
		networkIconMap.put("MY NETWORK TV", R.drawable.channel_mynetwork);
		networkIconMap.put("MY NETWORK", R.drawable.channel_mynetwork);
		networkIconMap.put("MYNETWORK TV", R.drawable.channel_mynetwork);
		networkIconMap.put("MYNETWORKTV", R.drawable.channel_mynetwork);
		networkIconMap.put("MYTV", R.drawable.channel_mynetwork);
		networkIconMap.put("ION", R.drawable.channel_ion);
		networkIconMap.put("TRINITY BROADCASTING NETWORK", R.drawable.channel_tbn);
		networkIconMap.put("TBN", R.drawable.channel_tbn);
		networkIconMap.put("TELEMUNDO", R.drawable.channel_telemundo);
		networkIconMap.put("TELEFUTURA", R.drawable.channel_telefutura);
		networkIconMap.put("DAYSTAR", R.drawable.channel_daystar);
	}
}
