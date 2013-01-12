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

package com.entertailion.android.launcher.row;


/**
 * Data structure for each row of favorite items. There are at least a recent
 * apps row and one favorites row.
 * 
 * @author leon_nicholls
 * 
 */
public class RowInfo implements Comparable<RowInfo> {
	private static String LOG_TAG = "RowInfo";

	public static final int FAVORITE_TYPE = 1;

	private int id;
	private int position;
	private String title;
	private int type;
	boolean selected;

	public RowInfo() {
		super();
	}

	public RowInfo(int id, String title, int position, int type) {
		this.id = id;
		this.position = position;
		this.title = title;
		this.type = type;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public int compareTo(RowInfo other) {
		if (position == other.getPosition()) {
			return 0;
		} else if (position < other.getPosition()) {
			return -1;
		} else {
			return 1;
		}
	}

}
