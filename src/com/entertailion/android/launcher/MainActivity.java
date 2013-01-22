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
package com.entertailion.android.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Main launcher activity to inform users on how to use the launcher. Also
 * provides a shortcut to uninstall the launcher.
 * 
 * @author leon_nicholls
 * 
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		Typeface lightTypeface = ((LauncherApplication) getApplicationContext()).getLightTypeface(this);

		TextView aboutTextView = (TextView) findViewById(R.id.about_text1);
		aboutTextView.setTypeface(lightTypeface);
		aboutTextView.setText(getString(R.string.about_version_title, Utils.getVersion(this)));
		TextView copyrightTextView = (TextView) findViewById(R.id.copyright_text);
		copyrightTextView.setTypeface(lightTypeface);
		TextView feedbackTextView = (TextView) findViewById(R.id.feedback_text);
		feedbackTextView.setTypeface(lightTypeface);
		TextView instructionsTextView = (TextView) findViewById(R.id.instructions_text);
		instructionsTextView.setTypeface(lightTypeface);
		if (Utils.isVizioCoStar()) {
			instructionsTextView.setText(getString(R.string.about_instructions_vizio));
		}

		((Button) findViewById(R.id.button_web)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getString(R.string.about_button_web_url)));
				startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_WEB_SITE);
			}

		});

		((Button) findViewById(R.id.button_privacy_policy)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getString(R.string.about_button_privacy_policy_url)));
				startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_PRIVACY_POLICY);
			}

		});
		((Button) findViewById(R.id.button_more_apps)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getString(R.string.about_button_more_apps_url)));
				startActivity(intent);
				Analytics.logEvent(Analytics.ABOUT_MORE_APPS);
			}

		});
		((Button) findViewById(R.id.button_uninstall)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Uri packageURI = Uri.parse("package:com.entertailion.android.launcher");
				Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
				startActivity(uninstallIntent);
			}

		});
		Analytics.logEvent(Analytics.LAUNCHER_MAIN);
	}

}
