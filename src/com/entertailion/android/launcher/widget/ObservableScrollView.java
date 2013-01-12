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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Custom scroll view to track navigation. Used to track which gallery row is
 * visible in user interface.
 * 
 * @author leon_nicholls
 * 
 */
public class ObservableScrollView extends ScrollView {
	private static final String LOG_TAG = "ObservableScrollView";

	private ScrollViewListener scrollViewListener = null;
	private int level;

	public ObservableScrollView(Context context) {
		super(context);
	}

	public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScrollViewListener(ScrollViewListener scrollViewListener) {
		this.scrollViewListener = scrollViewListener;
	}

	/**
	 * Compute the amount to scroll in the Y direction in order to get a
	 * rectangle completely on the screen (or, if taller than the screen, at
	 * least the first screen size chunk of it).
	 * 
	 * @param rect
	 *            The rect.
	 * @return The scroll delta.
	 */
	protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
		if (getChildCount() == 0)
			return 0;

		// simplified logic to make the arrow movements be exactly the height of
		// the child
		int scrollYDelta = 0;

		ViewGroup viewGroup = (ViewGroup) getChildAt(0); // scrollview has one
															// child
		if (viewGroup.getChildCount() > 0) {
			int childHeight = viewGroup.getChildAt(0).getHeight();

			int height = getHeight();
			int screenTop = getScrollY();
			int screenBottom = screenTop + height;

			if (rect.bottom > screenBottom && rect.top > screenTop) {
				scrollYDelta = childHeight;
			} else if (rect.top < screenTop && rect.bottom < screenBottom) {
				scrollYDelta = -childHeight;
			}
		}

		return scrollYDelta;
	}

	public int getLevel() {
		return level;
	}

	/**
	 * Handle scrolling in response to an up or down arrow click.
	 * 
	 * @param direction
	 *            The direction corresponding to the arrow key that was pressed
	 * @return True if we consumed the event, false otherwise
	 */
	public boolean arrowScroll(int direction) {
		ViewGroup viewGroup = (ViewGroup) getChildAt(0);
		int children = viewGroup.getChildCount();
		if (direction == View.FOCUS_DOWN) {
			level++;
			if (level >= children) {
				level = children - 1;
			}
		} else {
			level--;
			if (level < 0) {
				level = 0;
			}
		}
		if (scrollViewListener != null) {
			scrollViewListener.arrowScroll(direction);
		}
		return super.arrowScroll(direction);
	}

	public void resetScroll() {
		level = 0;
		super.onScrollChanged(0, 0, 0, 0);
		invalidate();
	}
	
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		return true;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return true;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

}