/*
 * Copyright (C) 2012 ENTERTAILION, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.entertailion.android.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

/**
 * Activity to display easter egg image.
 * 
 * @author leon_nicholls
 * 
 */
public class EasterEggActivity extends Activity {

	private static final String LOG_TAG = "EasterEggActivity";

	private boolean finished = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.easter_egg);
	}

	/**
	 * Finish activity when the user interacts
	 */
	protected void doFinish() {
		synchronized (this) {
			if (!finished) {
				finished = true;
				new Thread(new Runnable() {

					@Override
					public void run() {
						EasterEggActivity.this.finish();
					}

				}).start();
			}
		}
	}

	/**
	 * @see android.app.Activity#dispatchKeyEvent(android.view.KeyEvent)
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		doFinish();
		return super.dispatchKeyEvent(e);
	};

	/**
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (!hasFocus) {
			doFinish();
		}
	}
}
