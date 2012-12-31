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

// http://code.google.com/p/ongoingweather/

import java.util.ArrayList;

/**
 * Data structure to store current and forecast weather data.
 * 
 * @author leon_nicholls
 * 
 */
public class WeatherSet {
	private WeatherCurrentCondition myCurrentCondition = null;
	private ArrayList<WeatherForecastCondition> myForecastConditions = new ArrayList<WeatherForecastCondition>(4);
	private String hazard;
	private String hazardUrl;

	public WeatherCurrentCondition getWeatherCurrentCondition() {
		return myCurrentCondition;
	}

	public void setWeatherCurrentCondition(WeatherCurrentCondition myCurrentWeather) {
		this.myCurrentCondition = myCurrentWeather;
	}

	public ArrayList<WeatherForecastCondition> getWeatherForecastConditions() {
		return this.myForecastConditions;
	}

	public WeatherForecastCondition getLastWeatherForecastCondition() {
		return this.myForecastConditions.get(this.myForecastConditions.size() - 1);
	}

	public String getHazard() {
		return this.hazard;
	}

	public void setHazard(String hazard) {
		this.hazard = hazard;
	}

	public String getHazardUrl() {
		return this.hazardUrl;
	}

	public void setHazardUrl(String hazardUrl) {
		this.hazardUrl = hazardUrl;
	}
}
