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

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.entertailion.android.launcher.LauncherApplication;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.utils.Utils;
import com.entertailion.android.launcher.weather.WeatherSet;

/**
 * Weather widget. Displays current weather and 4-day forecast from NOAA weather
 * data.
 * 
 * @author leon_nicholls
 * 
 */
public class Weather extends LinearLayout {
	private static String LOG_TAG = "Weather";
	private static String DEGREES = "\u00B0";
	private static int DEFAULT_DELAY = 10 * 1000;
	private static int MAX_RETRIES = 20;
	private Timer timer;
	private Handler handler = new Handler();
	private int delay = DEFAULT_DELAY;
	private int retries = 0;
	private ProgressBar progressBar;
	private boolean isFahrenheit = true;

	public Weather(Context context, AttributeSet attrs) {
		super(context, attrs);

		addView(inflate(context, R.layout.weather_widget, null));

		progressBar = (ProgressBar) findViewById(R.id.progress);

		if (!isInEditMode()) { // support IDE editor
			Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Condensed.ttf");
			TextView currentHighlow = (TextView) findViewById(R.id.current_highlow);
			currentHighlow.setTypeface(tf);
			TextView forecast1Highlow = (TextView) findViewById(R.id.forecast1_highlow);
			forecast1Highlow.setTypeface(tf);
			TextView forecast2Highlow = (TextView) findViewById(R.id.forecast2_highlow);
			forecast2Highlow.setTypeface(tf);
			TextView forecast3Highlow = (TextView) findViewById(R.id.forecast3_highlow);
			forecast3Highlow.setTypeface(tf);
			TextView forecast4Highlow = (TextView) findViewById(R.id.forecast4_highlow);
			forecast4Highlow.setTypeface(tf);
			// TextView hazard = (TextView) findViewById(R.id.hazard);
			// hazard.setTypeface(tf);
		}

		// only have free weather data for USA
		if (Utils.isUsa()) {
			start();
		} else {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Update the text with the current time
	 */
	public void updateView() {
		Log.d(LOG_TAG, "updateView");
		WeatherSet weatherSet = ((LauncherApplication) getContext().getApplicationContext()).getWeatherSet();
		if (weatherSet != null) {
			setWeather(weatherSet);
		} else {
			// could be the first time run, so try again to get weather
			progressBar.setVisibility(View.VISIBLE);
			Runnable runnable = new Runnable() {
				public void run() {
					Log.d(LOG_TAG, "weather run: "+delay);
					WeatherSet weatherSet = ((LauncherApplication) getContext().getApplicationContext()).getWeatherSet();
					if (weatherSet == null && retries < MAX_RETRIES) {
						retries++;
						//delay = delay * 2;
						handler.postDelayed(this, delay);
					} else {
						setWeather(weatherSet);
					}
				}
			};
			handler.postDelayed(runnable, delay);
		}
	}

	private void setWeather(WeatherSet weatherSet) {
		if (weatherSet != null && weatherSet.getWeatherCurrentCondition() != null && weatherSet.getWeatherCurrentCondition().getTempFahrenheit()!=null) {
			try {
				TextView currentTemp = (TextView) findViewById(R.id.current_temp);
				if (isFahrenheit) {
					currentTemp.setText(String.valueOf(weatherSet.getWeatherCurrentCondition().getTempFahrenheit()) + DEGREES);
				} else {
					currentTemp.setText(String.valueOf(weatherSet.getWeatherCurrentCondition().getTempCelcius()) + DEGREES);
				}
				ImageView currentIcon = (ImageView) findViewById(R.id.current_icon);
				currentIcon.setImageResource(weatherSet.getWeatherCurrentCondition().getIcon());
				TextView currentHighlow = (TextView) findViewById(R.id.current_highlow);
				// TextView hazard = (TextView) findViewById(R.id.hazard);
				// if (weatherSet.getHazard()==null) {
				// hazard.setVisibility(INVISIBLE);
				// } else {
				// hazard.setTypeface(null,Typeface.BOLD);
				// hazard.setTextSize(15);
				// hazard.setText(weatherSet.getHazard());
				// hazard.setVisibility(VISIBLE);
				// }
				Calendar calendar = Calendar.getInstance();
				int index = 0;
				String today = getDay(DateUtils.getDayOfWeekString(calendar.get(Calendar.DAY_OF_WEEK), DateUtils.FORMAT_ABBREV_WEEKDAY));
				String tomorrow = getDay(weatherSet.getWeatherForecastConditions().get(1).getDayofWeek());
				if (today.equalsIgnoreCase(tomorrow)) { // overnight case
					index = 1;
				}
				TextView forecast1Day = (TextView) findViewById(R.id.forecast1_day);
				forecast1Day.setText(today);
				ImageView forecast1Icon = (ImageView) findViewById(R.id.forecast1_icon);
				forecast1Icon.setImageResource(weatherSet.getWeatherForecastConditions().get(index).getIcon());
				TextView forecast1Highlow = (TextView) findViewById(R.id.forecast1_highlow);
				if (weatherSet.getWeatherForecastConditions().get(index).getTempMinCelsius() == -10000) {
					forecast1Highlow.setText(" \n"
							+ String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index).getTempMaxCelsius()))
							+ DEGREES);
				} else {
					if (isFahrenheit) {
						forecast1Highlow.setText(String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index)
								.getTempMaxCelsius()))
								+ DEGREES
								+ "\n"
								+ String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index).getTempMinCelsius()))
								+ DEGREES);
					} else {
						forecast1Highlow.setText(String.valueOf(String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index).getTempMaxCelsius())
								+ DEGREES + "\n" + (int) weatherSet.getWeatherForecastConditions().get(index).getTempMinCelsius())
								+ DEGREES);
					}
				}
				TextView forecast2Day = (TextView) findViewById(R.id.forecast2_day);
				calendar.add(Calendar.DATE, 1);
				forecast2Day.setText(getDay(DateUtils.getDayOfWeekString(calendar.get(Calendar.DAY_OF_WEEK), DateUtils.FORMAT_ABBREV_WEEKDAY)));
				ImageView forecast2Icon = (ImageView) findViewById(R.id.forecast2_icon);
				forecast2Icon.setImageResource(weatherSet.getWeatherForecastConditions().get(index + 1).getIcon());
				TextView forecast2Highlow = (TextView) findViewById(R.id.forecast2_highlow);
				if (isFahrenheit) {
					forecast2Highlow.setText(String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 1)
							.getTempMaxCelsius()))
							+ DEGREES
							+ "\n"
							+ String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 1).getTempMinCelsius()))
							+ DEGREES);
				} else {
					forecast2Highlow.setText(String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 1).getTempMaxCelsius()) + DEGREES
							+ "\n" + String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 1).getTempMinCelsius()) + DEGREES);
				}
				TextView forecast3Day = (TextView) findViewById(R.id.forecast3_day);
				calendar.add(Calendar.DATE, 1);
				forecast3Day.setText(getDay(DateUtils.getDayOfWeekString(calendar.get(Calendar.DAY_OF_WEEK), DateUtils.FORMAT_ABBREV_WEEKDAY)));
				ImageView forecast3Icon = (ImageView) findViewById(R.id.forecast3_icon);
				forecast3Icon.setImageResource(weatherSet.getWeatherForecastConditions().get(index + 2).getIcon());
				TextView forecast3Highlow = (TextView) findViewById(R.id.forecast3_highlow);
				if (isFahrenheit) {
					forecast3Highlow.setText(String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 2)
							.getTempMaxCelsius()))
							+ DEGREES
							+ "\n"
							+ String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 2).getTempMinCelsius()))
							+ DEGREES);
				} else {
					forecast3Highlow.setText(String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 2).getTempMaxCelsius()) + DEGREES
							+ "\n" + String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 2).getTempMinCelsius()) + DEGREES);
				}
				TextView forecast4Day = (TextView) findViewById(R.id.forecast4_day);
				calendar.add(Calendar.DATE, 1);
				forecast4Day.setText(getDay(DateUtils.getDayOfWeekString(calendar.get(Calendar.DAY_OF_WEEK), DateUtils.FORMAT_ABBREV_WEEKDAY)));
				ImageView forecast4Icon = (ImageView) findViewById(R.id.forecast4_icon);
				forecast4Icon.setImageResource(weatherSet.getWeatherForecastConditions().get(index + 3).getIcon());
				TextView forecast4Highlow = (TextView) findViewById(R.id.forecast4_highlow);
				if (isFahrenheit) {
					forecast4Highlow.setText(String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 3)
							.getTempMaxCelsius()))
							+ DEGREES
							+ "\n"
							+ String.valueOf((int) Utils.celsiusToFahrenheit(weatherSet.getWeatherForecastConditions().get(index + 3).getTempMinCelsius()))
							+ DEGREES);
				} else {
					forecast4Highlow.setText(String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 3).getTempMaxCelsius()) + DEGREES
							+ "\n" + String.valueOf((int) weatherSet.getWeatherForecastConditions().get(index + 3).getTempMinCelsius()) + DEGREES);
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "setWeather", e);
			}
		}

		retries = 0;
		delay = DEFAULT_DELAY;
		progressBar.setVisibility(View.INVISIBLE);
	}

	/**
	 * Thread to update weather every hour
	 */
	public void start() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						updateView();
					}
				});
			}
		}, 0, 1000 * 60 * 60); // every hour since NOAA data is published once an hour
	}

	/**
	 * Stop the update thread
	 */
	public void stop() {
		timer.cancel();
	}

	private String getDay(String value) {
		if (value != null) {
			if (value.equalsIgnoreCase("Mon") || value.equalsIgnoreCase("Monday") || value.equalsIgnoreCase("Monday Night")) {
				return getContext().getString(R.string.day_monday);
			} else if (value.equalsIgnoreCase("Tue") || value.equalsIgnoreCase("Tuesday") || value.equalsIgnoreCase("Tuesday Night")) {
				return getContext().getString(R.string.day_tuesday);
			} else if (value.equalsIgnoreCase("Wed") || value.equalsIgnoreCase("Wednesday") || value.equalsIgnoreCase("Wednesday Night")) {
				return getContext().getString(R.string.day_wednesday);
			} else if (value.equalsIgnoreCase("Thu") || value.equalsIgnoreCase("Thursday") || value.equalsIgnoreCase("Thursday Night")) {
				return getContext().getString(R.string.day_thursday);
			} else if (value.equalsIgnoreCase("Fri") || value.equalsIgnoreCase("Friday") || value.equalsIgnoreCase("Friday Night")) {
				return getContext().getString(R.string.day_friday);
			} else if (value.equalsIgnoreCase("Sat") || value.equalsIgnoreCase("Saturday") || value.equalsIgnoreCase("Saturday Night")) {
				return getContext().getString(R.string.day_saturday);
			} else if (value.equalsIgnoreCase("Sun") || value.equalsIgnoreCase("Sunday") || value.equalsIgnoreCase("Sunday Night")) {
				return getContext().getString(R.string.day_sunday);
			}
		}
		return "-";
	}
}
