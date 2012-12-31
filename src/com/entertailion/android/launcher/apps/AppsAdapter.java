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
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.entertailion.android.launcher.utils.Utils;
import com.entertailion.android.launcher.widget.GalleryAdapter;

/**
 * Adapter for recent apps gallery row.
 * 
 * @author leon_nicholls
 * 
 */
public class AppsAdapter extends GalleryAdapter<ApplicationInfo> {

	public AppsAdapter(Context context, ArrayList<ApplicationInfo> apps, boolean infiniteScrolling) {
		super(context, apps, infiniteScrolling);
	}

	@Override
	protected void updateView(ImageView imageView, int position) {
		ApplicationInfo info = getItem(position);

		Drawable icon = info.getDrawable();
		if (!info.getFiltered()) {
			icon = Utils.createIconThumbnail(icon, getContext());
			info.setDrawable(icon);
			info.setFiltered(true);
		}
		imageView.setImageDrawable(icon);
	}
}