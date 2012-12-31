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
package com.entertailion.android.launcher.weather;

//http://code.google.com/p/ongoingweather/

/**
 * Data structure to store weather forecast data.
 * 
 * @author leon_nicholls
 * 
 */
public class WeatherForecastCondition {

	private String dayofWeek = null;
	private float tempMin = 0;
	private float tempMax = 0;
	private String iconURL = null;
	private int icon;
	private String condition = null;

	public WeatherForecastCondition() {

	}

	public String getDayofWeek() {
		return dayofWeek;
	}

	public void setDayofWeek(String dayofWeek) {
		this.dayofWeek = dayofWeek;
	}

	public float getTempMinCelsius() {
		return tempMin;
	}

	public void setTempMinCelsius(float tempMin) {
		this.tempMin = tempMin;
	}

	public float getTempMaxCelsius() {
		return tempMax;
	}

	public void setTempMaxCelsius(float tempMax) {
		this.tempMax = tempMax;
	}

	public String getIconURL() {
		return iconURL;
	}

	public void setIconURL(String iconURL) {
		this.iconURL = iconURL;
	}

	public int getIcon() {
		return this.icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
