/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.util.ArrayList;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.entertailion.android.launcher.R;

/**
 * Let the user select from wallpaper images embedded in the app.
 * 
 * Some code from:
 * https://github.com/AnderWeb/android_packages_apps_Launcher/tree/froyo
 * 
 */
public class StaticWallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener, OnClickListener {
	private static final String LOG_TAG = "StaticWallpaperChooser";

	private Gallery gallery;
	private ImageView imageView;
	private boolean isWallpaperSet;
	private Bitmap bitmap;
	private ArrayList<Integer> thumbs;
	private ArrayList<Integer> images;
	private WallpaperLoader wallpaperLoader;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		findWallpapers();

		setContentView(R.layout.wallpaper_chooser);

		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new ImageAdapter(this));
		gallery.setOnItemSelectedListener(this);
		gallery.setCallbackDuringFling(false);

		findViewById(R.id.set).setOnClickListener(this);

		imageView = (ImageView) findViewById(R.id.wallpaper);
	}

	private void findWallpapers() {
		thumbs = new ArrayList<Integer>(24);
		images = new ArrayList<Integer>(24);

		final Resources resources = getResources();
		final String packageName = getApplication().getPackageName();

		addWallpapers(resources, packageName, R.array.wallpapers);
	}

	private void addWallpapers(Resources resources, String packageName, int list) {
		final String[] extras = resources.getStringArray(list);
		for (String extra : extras) {
			int res = resources.getIdentifier(extra, "drawable", packageName);
			if (res != 0) {
				final int thumbRes = resources.getIdentifier(extra + "_small", "drawable", packageName);

				if (thumbRes != 0) {
					thumbs.add(thumbRes);
					images.add(res);
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		isWallpaperSet = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (wallpaperLoader != null && wallpaperLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			wallpaperLoader.cancel(true);
			wallpaperLoader = null;
		}
	}

	public void onItemSelected(AdapterView parent, View v, int position, long id) {
		if (wallpaperLoader != null && wallpaperLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
			wallpaperLoader.cancel();
		}
		wallpaperLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
	}

	/*
	 * When using touch if you tap an image it triggers both the onItemClick and
	 * the onTouchEvent causing the wallpaper to be set twice. Ensure we only
	 * set the wallpaper once.
	 */
	private void selectWallpaper(int position) {
		if (isWallpaperSet) {
			return;
		}

		isWallpaperSet = true;
		try {
			WallpaperManager wallpaperManager = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
			wallpaperManager.setResource(images.get(position));
			setResult(RESULT_OK);
			finish();
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to set wallpaper: " + e);
		}
	}

	public void onNothingSelected(AdapterView parent) {
	}

	private class ImageAdapter extends BaseAdapter {
		private LayoutInflater layoutInflater;

		ImageAdapter(StaticWallpaperChooser context) {
			layoutInflater = context.getLayoutInflater();
		}

		public int getCount() {
			return thumbs.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView image;

			if (convertView == null) {
				image = (ImageView) layoutInflater.inflate(R.layout.wallpaper_item, parent, false);
			} else {
				image = (ImageView) convertView;
			}

			int thumbRes = thumbs.get(position);
			image.setImageResource(thumbRes);
			Drawable thumbDrawable = image.getDrawable();
			if (thumbDrawable != null) {
				thumbDrawable.setDither(true);
			} else {
				Log.e(LOG_TAG, String.format("Error decoding thumbnail resId=%d for wallpaper #%d", thumbRes, position));
			}
			image.setBackgroundDrawable(getResources().getDrawable(R.drawable.gallery_selector));
			return image;
		}
	}

	public void onClick(View v) {
		gallery.setVisibility(View.INVISIBLE);
		selectWallpaper(gallery.getSelectedItemPosition());
	}

	private class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
		BitmapFactory.Options options;

		WallpaperLoader() {
			options = new BitmapFactory.Options();
			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		}

		protected Bitmap doInBackground(Integer... params) {
			if (isCancelled())
				return null;
			try {
				return BitmapFactory.decodeResource(getResources(), images.get(params[0]), options);
			} catch (OutOfMemoryError e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap b) {
			if (b == null)
				return;

			if (!isCancelled() && !options.mCancel) {
				// Help the GC
				if (bitmap != null) {
					bitmap.recycle();
				}

				final ImageView view = imageView;
				view.setImageBitmap(b);

				bitmap = b;

				final Drawable drawable = view.getDrawable();
				drawable.setFilterBitmap(true);
				drawable.setDither(true);

				view.postInvalidate();

				wallpaperLoader = null;
			} else {
				b.recycle();
			}
		}

		void cancel() {
			options.requestCancelDecode();
			super.cancel(true);
		}
	}
}
