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
package com.entertailion.android.launcher.widget;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.item.ItemInfo;

/**
 * Base adapter for row galleries.
 * 
 * @author leon_nicholls
 * 
 * @param <T>
 */
public class GalleryAdapter<T> extends ArrayAdapter<T> {
	private static final String LOG_TAG = "GalleryAdapter";

	private boolean infiniteScrolling;

	public GalleryAdapter(Context context, ArrayList<T> apps, boolean infiniteScrolling) {
		super(context, 0, apps);
		this.infiniteScrolling = infiniteScrolling;
	}

	@Override
	public int getCount() {
		int count = super.getCount();
		if (infiniteScrolling && count > 0) {
			return Integer.MAX_VALUE;
		} else {
			return count;
		}
	}

	public int getRealCount() {
		return super.getCount();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (infiniteScrolling && super.getCount() > 0) {
			position = position % super.getCount();
		}

		ImageView iconView = (ImageView) convertView;
		if (iconView == null) {
			iconView = new ImageView(getContext());
			iconView.setScaleType(ScaleType.FIT_XY);
			// Fix for Gallery setUnselectedAlpha bug:
			// 1. Disable hardware acceleration on activity
			// 2. Use style="android:galleryItemBackground" in xml
			// 3. Then explicitly set the background color for each view:
			iconView.setBackgroundColor(getContext().getResources().getColor(R.color.transparent));
		}

		try {
			updateView(iconView, position);
			ItemInfo itemInfo = (ItemInfo) getItem(position);
			iconView.setContentDescription(itemInfo.getTitle());
		} catch (Exception e) {
			Log.d(LOG_TAG, "getView", e);
		}
		iconView.setTag(R.id.pager_position, position);
		return iconView;
	}

	protected void updateView(ImageView imageView, int position) {
		try {
			ItemInfo itemInfo = (ItemInfo) getItem(position);
			itemInfo.renderIcon(imageView);
		} catch (Exception e) {
			Log.d(LOG_TAG, "updateView", e);
		}
	}
}