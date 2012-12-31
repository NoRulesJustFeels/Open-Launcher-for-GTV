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
package com.entertailion.android.launcher.bookmark;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.entertailion.android.launcher.R;

/**
 * Adapter for browser bookmark list.
 * 
 * @author leon_nicholls
 * 
 */
public class BookmarkAdapter extends ArrayAdapter<BookmarkInfo> {
	private static final String LOG_TAG = "BookmarkAdapter";
	private Context context;
	private LayoutInflater inflater;

	/**
	 * Data structure for caching reference for performance.
	 * 
	 */
	private static class ViewHolder {
		public TextView textView;
		public ImageView imageView;
	}

	public BookmarkAdapter(Context context, ArrayList<BookmarkInfo> bookmarks) {
		super(context, 0, bookmarks);
		this.context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			rowView = inflater.inflate(R.layout.list_row, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.textView = (TextView) rowView.findViewById(R.id.label);
			viewHolder.imageView = (ImageView) rowView.findViewById(R.id.icon);
			viewHolder.imageView.setImageResource(R.drawable.bookmark_icon);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		final BookmarkInfo info = getItem(position);

		holder.textView.setText(info.getTitle());

		return rowView;
	}
}