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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Data structure for apps.
 * 
 * @author leon_nicholls
 * 
 */
public class ApplicationInfo extends ItemInfo {

	private static final String LOG_TAG = "ApplicationInfo";

	/**
	 * Track if the application icon has been resized.
	 */
	private boolean filtered;

	public ApplicationInfo() {

	}

	public ApplicationInfo(int id, int position, String title, Intent intent) {
		super(id, position, title, intent);
	}

	public boolean getFiltered() {
		return filtered;
	}

	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}

	@Override
	public void invoke(Launcher context) {
		super.invoke(context);
		Analytics.logEvent(Analytics.INVOKE_APP);
	}

	@Override
	public void renderIcon(ImageView imageView) {

		Drawable icon = getDrawable();
		if (icon != null) {
			if (filtered) {
				icon = Utils.createIconThumbnail(icon, imageView.getContext());
				setDrawable(icon);
				filtered = true;
			}
			imageView.setImageDrawable(icon);
		}

		super.renderIcon(imageView);
	}

}
