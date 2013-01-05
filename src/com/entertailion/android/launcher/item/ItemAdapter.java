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
package com.entertailion.android.launcher.item;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.entertailion.android.launcher.widget.GalleryAdapter;

/**
 * Adapter for gallery row. Used for favorite rows configured by user.
 * 
 * @author leon_nicholls
 * 
 */
public class ItemAdapter extends GalleryAdapter<ItemInfo> {
	
	private static final String LOG_TAG = "ItemAdapter";

	public ItemAdapter(Context context, ArrayList<ItemInfo> apps, boolean infiniteScrolling) {
		super(context, apps, infiniteScrolling);
	}

	protected void updateView(ImageView imageView, int position) {
		try {
			ItemInfo info = getItem(position);

			info.renderIcon(imageView);
		} catch (Exception e) {
			Log.d(LOG_TAG, "updateView", e);
		}
	}
}