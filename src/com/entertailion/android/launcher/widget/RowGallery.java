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

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Gallery widget for each row of apps.
 * 
 * @author leon_nicholls
 * 
 */
public class RowGallery extends LinearLayout {
	private static String LOG_TAG = "RowGallery";
	private static final float GALLERY_UNSELECTECTED_ALPHA = 0.6f;
	public static final int GALLERY_ANIMATION_DURATION = 400;
	private EcoGallery gallery;

	public RowGallery(final Launcher launcher, int id, String title, GalleryAdapter adapter) {
		super(launcher, null);

		addView(inflate(launcher, R.layout.gallery_widget, null));

		gallery = (EcoGallery) findViewById(R.id.gallery);
		gallery.setCallbackDuringFling(false);
		gallery.setUnselectedAlpha(GALLERY_UNSELECTECTED_ALPHA);
		gallery.setGravity(Gravity.CENTER_HORIZONTAL);
		gallery.setAnimationDuration(GALLERY_ANIMATION_DURATION);
		gallery.setHorizontalFadingEdgeEnabled(true);
		gallery.setEmptyView(createEmptyView());
		gallery.setDrawingCacheEnabled(true);
		gallery.setOnItemSelectedListener(launcher);
		gallery.setOnItemClickListener(launcher);
		gallery.setOnItemLongClickListener(launcher);
		gallery.setFocusableInTouchMode(false);
		gallery.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
						// Jump to first item that starts with the typed letter
						char letter = Utils.keyCodeToLetter(keyCode);
						int count = gallery.getAdapter().getCount();
						for (int i = 0; i < count; i++) {
							ItemInfo itemInfo = (ItemInfo) gallery.getAdapter().getItem(i);
							if (itemInfo.getTitle().toUpperCase().charAt(0) == letter) {
								gallery.setSelection(i);
								break;
							}

						}
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
						// Go to the last item
						int count = gallery.getAdapter().getCount();
						gallery.setSelection(count - 1);
					} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
						// Go to the first item
						gallery.setSelection(0);
					} else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
						// Go to the numbered item position; use 1 for first
						// position, 0 for 10th position
						int count = gallery.getAdapter().getCount();
						if (keyCode == KeyEvent.KEYCODE_0 && count >= 10) {
							gallery.setSelection(9);
						} else {
							int index = keyCode - KeyEvent.KEYCODE_0;
							if (index <= count) {
								gallery.setSelection(index - 1);
							}
						}
					} else if (keyCode == KeyEvent.KEYCODE_DEL) {
						launcher.doDelete();
					}
				}
				return false;
			}

		});
		launcher.registerForContextMenu(gallery); // long clicks
		gallery.setTag(R.id.row_id, id);
		gallery.setTag(R.id.gallery_title, title);
		gallery.setAdapter(adapter);
		int count = adapter.getCount();
		int middle = 0;
		if (count % 2 == 0) { // even
			middle = count / 2 - 1;
		} else { // odd
			middle = count / 2;
		}
		gallery.setSelection(middle, false);
	}

	/**
	 * Create a view for an empty gallery
	 * 
	 * @return default empty view
	 */
	private View createEmptyView() {
		ImageView iconView = new ImageView(getContext());
		iconView.setScaleType(ScaleType.FIT_XY);
		iconView.setBackgroundColor(getResources().getColor(R.color.transparent));
		iconView.setImageResource(R.drawable.gallery_item_over);
		iconView.setTag(R.id.pager_position, 0);
		return iconView;
	}

	public void setAdapter(GalleryAdapter adapter) {
		gallery.setAdapter(adapter);
	}

	public GalleryAdapter getAdapter() {
		return (GalleryAdapter) gallery.getAdapter();
	}

	public int getSelectedItemPosition() {
		return gallery.getSelectedItemPosition();
	}

	public void setSelectedItemPosition(int position) {
		try {
			gallery.setSelection(position, false);
		} catch (Exception e) {
			Log.d(LOG_TAG, "setSelectedItemPosition", e);
		}
	}

	public Object getTag(int key) {
		return gallery.getTag(key);
	}
	
	public void setAnimation(boolean animate) {
		if (animate) {
			gallery.setAnimationDuration(GALLERY_ANIMATION_DURATION);
		} else {
			gallery.setAnimationDuration(0);
		}
	}
}
