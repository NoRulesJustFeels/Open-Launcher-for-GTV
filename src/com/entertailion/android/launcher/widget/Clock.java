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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.entertailion.android.launcher.R;

/**
 * Clock widget. Displays current time with minute accuracy.
 * 
 * @author leon_nicholls
 * 
 */
public class Clock extends LinearLayout {
	private static String LOG_TAG = "Clock";
	private Timer timer;
	private Handler handler = new Handler();
	private TextView timeView;
	private String lastTime;

	public Clock(Context context, AttributeSet attrs) {
		super(context, attrs);

		addView(inflate(context, R.layout.clock_widget, null));

		timeView = (TextView) findViewById(R.id.time);

		if (!isInEditMode()) { // support IDE editor
			Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Condensed.ttf");
			timeView.setTypeface(typeface);
		}

		start();
	}

	/**
	 * Thread to update clock every second
	 */
	public void start() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				final String time = DateUtils.formatDateTime(getContext(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME);
				if (!time.equals(lastTime)) {
					handler.post(new Runnable() {
						public void run() {
							timeView.setText(time);
						}
					});
					lastTime = time;
				}
			}
		}, 0, 1000); // every second
	}

	/**
	 * Stop the update thread
	 */
	public void stop() {
		timer.cancel();
	}
}
