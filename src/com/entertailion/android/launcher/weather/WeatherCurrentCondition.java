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
 * Data structure to store weather current data.
 * 
 * @author leon_nicholls
 * 
 */
public class WeatherCurrentCondition {
	private String dayofWeek = null;
	private Integer tempCelcius = null;
	private Integer tempFahrenheit = null;
	private String iconURL = null;
	private String condition = null;
	private int icon;
	private String windCondition = null;
	private String humidity = null;

	public WeatherCurrentCondition() {

	}

	public String getDayofWeek() {
		return this.dayofWeek;
	}

	public void setDayofWeek(String dayofWeek) {
		this.dayofWeek = dayofWeek;
	}

	public Integer getTempCelcius() {
		return this.tempCelcius;
	}

	public void setTempCelcius(Integer temp) {
		this.tempCelcius = temp;
	}

	public Integer getTempFahrenheit() {
		return this.tempFahrenheit;
	}

	public void setTempFahrenheit(Integer temp) {
		this.tempFahrenheit = temp;
	}

	public String getIconURL() {
		return this.iconURL;
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
		return this.condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getWindCondition() {
		return this.windCondition;
	}

	public void setWindCondition(String windCondition) {
		this.windCondition = windCondition;
	}

	public String getHumidity() {
		return this.humidity;
	}

	public void setHumidity(String humidity) {
		this.humidity = humidity;
	}

}
