/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.entertailion.android.launcher.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * From: https://github.com/AnderWeb/android_packages_apps_Launcher/tree/froyo
 * 
 */
public class FastBitmapDrawable extends Drawable {
	private Bitmap bitmap;

	public FastBitmapDrawable(Bitmap b) {
		bitmap = b;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawBitmap(bitmap, 0.0f, 0.0f, null);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}

	@Override
	public int getIntrinsicWidth() {
		return bitmap.getWidth();
	}

	@Override
	public int getIntrinsicHeight() {
		return bitmap.getHeight();
	}

	@Override
	public int getMinimumWidth() {
		return bitmap.getWidth();
	}

	@Override
	public int getMinimumHeight() {
		return bitmap.getHeight();
	}

	public Bitmap getBitmap() {
		return bitmap;
	}
}
