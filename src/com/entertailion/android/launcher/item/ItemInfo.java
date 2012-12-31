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

import java.text.Collator;
import java.util.Comparator;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;

/**
 * Base data structure for all kinds of items that the user can configure for
 * the favorite gallery rows.
 * 
 * @author leon_nicholls
 * 
 */
public class ItemInfo {
	private int id = DatabaseHelper.NO_ID;
	private int position;
	private String title;
	private Intent intent;
	private Drawable drawable;

	public ItemInfo() {
		this.id = DatabaseHelper.NO_ID;
	}

	public ItemInfo(int id, int position, String title, Intent intent) {
		this.id = id;
		this.position = position;
		this.title = title;
		this.intent = intent;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Intent getIntent() {
		return intent;
	}

	public void setIntent(Intent intent) {
		this.intent = intent;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	public void setDrawable(Drawable drawable) {
		this.drawable = drawable;
	}

	public void invoke(Launcher context) {
		if (intent != null) {
			context.startActivity(intent);
		}
	}

	public void renderIcon(ImageView imageView) {
		if (drawable != null) {
			imageView.setImageDrawable(drawable);
			return;
		}
		imageView.setImageResource(android.R.drawable.ic_input_get);
	}

	public void persistInsert(Context context, int rowId, int position) throws Exception {
		ItemsTable.insertItem(context, rowId, position, title, intent, null, DatabaseHelper.APP_TYPE);
	}

	public void persistUpdate(Context context, int rowId, int position) throws Exception {
		ItemsTable.updateItem(context, id, rowId, position, title, intent, null, DatabaseHelper.APP_TYPE);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ItemInfo)) {
			return false;
		}

		ItemInfo that = (ItemInfo) o;
		if (intent != null && that.intent != null && intent.getComponent() != null && that.intent.getComponent() != null) {
			return title.equals(that.title) && intent.getComponent().getClassName().equals(that.intent.getComponent().getClassName());
		} else {
			return title.equals(that.title);
		}
	}

	/**
	 * Perform alphabetical comparison of application entry objects.
	 */
	public static final Comparator<ItemInfo> ALPHA_COMPARATOR = new Comparator<ItemInfo>() {
		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(ItemInfo object1, ItemInfo object2) {
			return collator.compare(object1.getTitle(), object2.getTitle());
		}
	};

	@Override
	public String toString() {
		return "Item [title=" + getTitle() + ", intent=" + getIntent() + ", position=" + position + "]";
	}

	@Override
	public int hashCode() {
		int result;
		result = (title != null ? title.hashCode() : 0);
		if (intent != null && intent.getComponent() != null) {
			String name = intent.getComponent().getClassName();
			result = 31 * result + (name != null ? name.hashCode() : 0);
		}
		return result;
	}
}
