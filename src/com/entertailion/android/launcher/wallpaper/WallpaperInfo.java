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
package com.entertailion.android.launcher.wallpaper;

import android.graphics.drawable.Drawable;

/**
 * Data for the different kinds of wallpapers.
 * 
 * @author leon_nicholls
 *
 */
public class WallpaperInfo {

	public static final int WALLPAPER_TYPE = 0;
	public static final int PHOTOS_TYPE = 1;
	public static final int LIVE_WALLPAPER_TYPE = 2;
	
	private int type;
	private String title;
	private Drawable drawable;

	public WallpaperInfo(int type, String title, Drawable drawable) {
		this.type = type;
		this.title = title;
		this.drawable = drawable;
	}
	
	public int getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	public void setDrawable(Drawable drawable) {
		this.drawable = drawable;
	}

}
