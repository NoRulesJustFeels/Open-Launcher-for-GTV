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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.format.DateUtils;
import android.util.Log;

import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.utils.Utils;

/**
 * XML parser for NOAA weather data:
 * http://forecast.weather.gov/MapClick.php?FcstType
 * =dwml&lat=33.07871627807617&lon=-96.80830383300781
 * 
 * @author leon_nicholls
 * 
 */
public class NoaaWeatherHandler extends DefaultHandler {

	private static final String LOG_TAG = "NoaaWeatherHandler";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 2012-05-28

	protected boolean isDay = true;
	protected WeatherSet weatherSet = null;

	private boolean in_forecast_information = false;
	private boolean in_current_conditions = false;
	private boolean in_current_temperature = false;
	private boolean in_max_temperature = false;
	private boolean in_min_temperature = false;
	private boolean in_day_names = false;
	private boolean tonight = false;

	private int day = 0;
	private ArrayList<String> dayMapping = new ArrayList<String>();
	private ArrayList<String> conditionMapping = new ArrayList<String>();
	private ArrayList<String> minTempMapping = new ArrayList<String>();
	private ArrayList<String> maxTempMapping = new ArrayList<String>();
	private ArrayList<String> hazardNameMapping = new ArrayList<String>();
	private ArrayList<String> hazardUrlMapping = new ArrayList<String>();

	// Current characters being accumulated
	private StringBuffer chars = new StringBuffer();
	private String today;
	private String tomorrow;

	public NoaaWeatherHandler(boolean isDay) {
		this.isDay = isDay;
	}

	public WeatherSet getWeatherSet() {
		return this.weatherSet;
	}

	@Override
	public void startDocument() throws SAXException {
		this.weatherSet = new WeatherSet();
		Calendar cal = Calendar.getInstance();
		today = sdf.format(cal.getTime());
		cal.add(Calendar.DAY_OF_MONTH, 1);
		tomorrow = sdf.format(cal.getTime());
	}

	@Override
	public void endDocument() throws SAXException {
		// Nothing
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		chars.delete(0, chars.length());

		if (localName.equals("data")) {
			String dataAttribute = atts.getValue("type");
			if (dataAttribute.equalsIgnoreCase("forecast")) {
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				this.weatherSet.getWeatherForecastConditions().add(new WeatherForecastCondition());
				in_forecast_information = true;
			} else if (dataAttribute.equalsIgnoreCase("current observations")) {
				this.weatherSet.setWeatherCurrentCondition(new WeatherCurrentCondition());
				in_current_conditions = true;
			}

		} else if (localName.equals("temperature")) {
			String dataAttribute = atts.getValue("type");
			if (in_current_conditions && dataAttribute.equalsIgnoreCase("apparent")) {
				in_current_temperature = true;
			} else if (in_forecast_information && dataAttribute.equalsIgnoreCase("maximum")) {
				in_max_temperature = true;
			} else if (in_forecast_information && dataAttribute.equalsIgnoreCase("minimum")) {
				in_min_temperature = true;
			}
		} else if (localName.equals("weather-conditions")) {
			if (in_current_conditions) {
				String dataAttribute = atts.getValue("weather-summary");
				if (dataAttribute != null) {
					Log.d(LOG_TAG, dataAttribute);
					this.weatherSet.getWeatherCurrentCondition().setCondition(dataAttribute);
					this.weatherSet.getWeatherCurrentCondition().setIcon(getIcon(dataAttribute.trim(), isDay));
				}
			} else if (in_forecast_information) {
				String dataAttribute = atts.getValue("weather-summary");
				if (dataAttribute != null) {
					conditionMapping.add(dataAttribute);
				}
			}
		} else if (in_day_names && localName.equals("start-valid-time")) {
			/*
			 * if (in_forecast_information) { String dataAttribute =
			 * atts.getValue("period-name"); if (dataAttribute!=null) {
			 * dayMapping.add(dataAttribute); } }
			 */
		} else if (localName.equals("hazard")) {
			if (in_forecast_information) {
				String dataAttribute = atts.getValue("headline");
				if (dataAttribute != null) {
					hazardNameMapping.add(dataAttribute);
				}
			}
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (localName.equals("value")) {
			if (in_current_temperature) {
				int value = Integer.parseInt(chars.toString());
				this.weatherSet.getWeatherCurrentCondition().setTempFahrenheit(value);
				this.weatherSet.getWeatherCurrentCondition().setTempCelcius((int) Utils.fahrenheitToCelsius(value));
			} else if (in_min_temperature) {
				if (tonight) {
					minTempMapping.add(String.valueOf(-10000));
					maxTempMapping.add(String.valueOf(Utils.fahrenheitToCelsius(Integer.parseInt(chars.toString()))));
					tonight = false;
				} else {
					minTempMapping.add(String.valueOf(Utils.fahrenheitToCelsius(Integer.parseInt(chars.toString()))));
				}
			} else if (in_max_temperature) {
				maxTempMapping.add(String.valueOf(Utils.fahrenheitToCelsius(Integer.parseInt(chars.toString()))));
			}
		} else if (in_forecast_information && localName.equals("weather")) {
			int day = 0;
			int counter = 0;
			boolean today = false;
			for (String dayName : dayMapping) {
				String condition = conditionMapping.get(day);
				if (!today
						&& (dayName.equalsIgnoreCase("Today") || dayName.equalsIgnoreCase("This Afternoon") || dayName.equalsIgnoreCase("Late Afternoon")
								|| dayName.equalsIgnoreCase("Tonight") || dayName.equalsIgnoreCase("Overnight"))) {
					try {
						this.weatherSet.getWeatherForecastConditions().get(counter).setDayofWeek(dayName);
						this.weatherSet.getWeatherForecastConditions().get(counter).setCondition(condition);
						this.weatherSet.getWeatherForecastConditions().get(counter).setIcon(getIcon(condition.trim(), true));
						this.weatherSet.getWeatherForecastConditions().get(counter).setTempMinCelsius(Float.parseFloat(minTempMapping.get(counter)));
						this.weatherSet.getWeatherForecastConditions().get(counter).setTempMaxCelsius(Float.parseFloat(maxTempMapping.get(counter)));
						today = true;
						counter++;
					} catch (Exception e) {
					}
				} else if (dayName.equalsIgnoreCase("Monday") || dayName.equalsIgnoreCase("Tuesday") || dayName.equalsIgnoreCase("Wednesday")
						|| dayName.equalsIgnoreCase("Thursday") || dayName.equalsIgnoreCase("Friday") || dayName.equalsIgnoreCase("Saturday")
						|| dayName.equalsIgnoreCase("Sunday")) {
					if (day < this.weatherSet.getWeatherForecastConditions().size()) {
						try {
							this.weatherSet.getWeatherForecastConditions().get(counter).setDayofWeek(dayName);
							this.weatherSet.getWeatherForecastConditions().get(counter).setCondition(condition);
							this.weatherSet.getWeatherForecastConditions().get(counter).setIcon(getIcon(condition.trim(), true));
							this.weatherSet.getWeatherForecastConditions().get(counter).setTempMinCelsius(Float.parseFloat(minTempMapping.get(counter)));
							this.weatherSet.getWeatherForecastConditions().get(counter).setTempMaxCelsius(Float.parseFloat(maxTempMapping.get(counter)));
							counter++;
						} catch (Exception e) {
						}
					}
				}
				day++;
			}
		} else if (localName.equals("temperature")) {
			this.in_current_temperature = false;
			this.in_max_temperature = false;
			this.in_min_temperature = false;
			this.day = 0;
		} else if (localName.equals("weather")) {
			this.day = 0;
		} else if (localName.equals("layout-key")) {
			if (chars.toString().startsWith("k-p12h-n1")) {
				this.day = 0;
				in_day_names = true;
			}
		} else if (in_day_names && localName.equals("start-valid-time")) {
			if (in_forecast_information) {
				String timePeriod = chars.toString();
				String date = timePeriod.substring(0, 10);
				if (tomorrow.equals(date) && (timePeriod.endsWith("00:00:00-05:00") || timePeriod.endsWith("00:00:00-06:00"))) {
					dayMapping.add("Overnight");
				} else if (today.equals(date)) {
					if (timePeriod.endsWith("18:00:00-05:00") || timePeriod.endsWith("18:00:00-06:00")) {
						dayMapping.add("Tonight");
					} else {
						dayMapping.add("Today");
					}
				} else {
					try {
						Calendar cal = Calendar.getInstance();
						cal.setTime(sdf.parse(date));
						String day = DateUtils.getDayOfWeekString(cal.get(Calendar.DAY_OF_WEEK), DateUtils.LENGTH_LONG);
						if (timePeriod.endsWith("18:00:00-05:00") || timePeriod.endsWith("18:00:00-06:00")) {
							day = day + " Night";
						}
						dayMapping.add(day);
					} catch (ParseException e) {
						Log.e(LOG_TAG, "start-valid-time", e);
					}
				}
			}
		} else if (in_day_names && localName.equals("time-layout")) {
			int todays = 0;
			for (String dayName : dayMapping) {
				if (dayName.equalsIgnoreCase("Today") || dayName.equalsIgnoreCase("This Afternoon") || dayName.equalsIgnoreCase("Late Afternoon")
						|| dayName.equalsIgnoreCase("Tonight") || dayName.equalsIgnoreCase("Overnight")) {
					todays++;
				}
			}
			tonight = todays == 1;
			this.day = 0;
			in_day_names = false;
		} else if (localName.equals("time-layout")) {
			this.day = 0;
			in_day_names = false;
		} else if (localName.equals("data")) {
			if (in_forecast_information) {
				if (hazardNameMapping.size() > 0) {
					for (int i = 0; i < hazardNameMapping.size(); i++) {
						if (hazardNameMapping.get(i).equalsIgnoreCase("Short Term Forecast")) {
							this.weatherSet.setHazard(hazardNameMapping.get(i));
							this.weatherSet.setHazardUrl(hazardUrlMapping.get(i));
							break;
						} else if (this.weatherSet.getHazard() == null) {
							this.weatherSet.setHazard(hazardNameMapping.get(i));
							this.weatherSet.setHazardUrl(hazardUrlMapping.get(i));
						}
					}
				}
			}
			in_forecast_information = false;
			in_current_conditions = false;
		} else if (localName.equals("hazardTextURL")) {
			hazardUrlMapping.add(chars.toString());
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		chars.append(new String(ch, start, length));
	}

	private int getIcon(String icon, boolean isDay) {
		if (icon != null) {
			// http://graphical.weather.gov/xml/xml_fields_icon_weather_conditions.php
			// http://www.weather.gov/xml/current_obs/weather.php
			if (icon.equalsIgnoreCase("Sunny") || icon.equalsIgnoreCase("Clear") || icon.equalsIgnoreCase("Clearing")
					|| icon.equalsIgnoreCase("Gradual Clearing") || icon.equalsIgnoreCase("Clearing Late") || icon.equalsIgnoreCase("Becoming Sunny")
					|| icon.equalsIgnoreCase("Hot")) {
				if (isDay) {
					return R.drawable.sunny;
				} else {
					return R.drawable.sunny_night;
				}
			} else if (icon.equalsIgnoreCase("Partly Sunny") || icon.equalsIgnoreCase("Mostly Clear") || icon.equalsIgnoreCase("Mostly Sunny")) {
				if (isDay) {
					return R.drawable.cloudy1;
				} else {
					return R.drawable.cloudy1_night;
				}
			} else if (icon.equalsIgnoreCase("Partly Cloudy") || icon.equalsIgnoreCase("Decreasing Clouds")) {
				if (isDay) {
					return R.drawable.cloudy2;
				} else {
					return R.drawable.cloudy2_night;
				}
			} else if (icon.equalsIgnoreCase("Mostly Cloudy") || icon.equalsIgnoreCase("Cloudy") || icon.equalsIgnoreCase("Increasing Clouds")
					|| icon.equalsIgnoreCase("Becoming Cloudy")) {
				if (isDay) {
					return R.drawable.cloudy4;
				} else {
					return R.drawable.cloudy4_night;
				}
			} else if (icon.equalsIgnoreCase("Sleet") || icon.equalsIgnoreCase("Sleet Likely") || icon.equalsIgnoreCase("Slight Chance Sleet")
					|| icon.equalsIgnoreCase("Chance Sleet") || icon.equalsIgnoreCase("Rain/Snow Likely") || icon.equalsIgnoreCase("Chance Rain/Sleet")
					|| icon.equalsIgnoreCase("Rain/Sleet Likely") || icon.equalsIgnoreCase("Rain/Sleet") || icon.equalsIgnoreCase("Slight Chance Rain/Sleet")
					|| icon.equalsIgnoreCase("Slight Chance Snow/Sleet") || icon.equalsIgnoreCase("Chance Snow/Sleet")
					|| icon.equalsIgnoreCase("Snow/Sleet Likely") || icon.equalsIgnoreCase("Snow/Sleet") || icon.equalsIgnoreCase("Slight Chance Wintry Mix")
					|| icon.equalsIgnoreCase("Chance Wintry Mix") || icon.equalsIgnoreCase("Wintry Mix Likely") || icon.equalsIgnoreCase("Wintry Mix")
					|| icon.equalsIgnoreCase("Slight Chance Rain/Snow") || icon.equalsIgnoreCase("Chance Rain/Snow") || icon.equalsIgnoreCase("Rain/Snow")
					|| icon.equalsIgnoreCase("Slight Chance Freezing Rain") || icon.equalsIgnoreCase("Freezing Drizzle Likely")
					|| icon.equalsIgnoreCase("Freezing Drizzle") || icon.equalsIgnoreCase("Slight Chance Rain/Freezing Rain")
					|| icon.equalsIgnoreCase("Freezing Rain Likely") || icon.equalsIgnoreCase("Freezing Rain")
					|| icon.equalsIgnoreCase("Slight Chance Freezing Drizzle") || icon.equalsIgnoreCase("Chance Freezing Drizzle")
					|| icon.equalsIgnoreCase("Chance Freezing Rain") || icon.equalsIgnoreCase("Chance Rain/Freezing Rain")
					|| icon.equalsIgnoreCase("Rain/Freezing Rain Likely") || icon.equalsIgnoreCase("Rain/Freezing Rain")
					|| icon.equalsIgnoreCase("Freezing Spray")) {
				if (isDay) {
					return R.drawable.sleet;
				} else {
					return R.drawable.sleet;
				}
			} else if (icon.equalsIgnoreCase("Snow") || icon.equalsIgnoreCase("Snow Likely") || icon.equalsIgnoreCase("Blizzard")) {
				if (isDay) {
					return R.drawable.snow1;
				} else {
					return R.drawable.snow1_night;
				}
			} else if (icon.equalsIgnoreCase("Chance Rain") || icon.equalsIgnoreCase("Slight Chance Rain Showers")
					|| icon.equalsIgnoreCase("Slight Chance Rain") || icon.equalsIgnoreCase("Chance Rain Showers")
					|| icon.equalsIgnoreCase("Slight Chance Drizzle") || icon.equalsIgnoreCase("Chance Drizzle") || icon.equalsIgnoreCase("Drizzle Likely")
					|| icon.equalsIgnoreCase("Drizzle")) {
				if (isDay) {
					return R.drawable.shower1;
				} else {
					return R.drawable.shower1_night;
				}
			} else if (icon.equalsIgnoreCase("Rain Showers Likely") || icon.equalsIgnoreCase("Rain") || icon.equalsIgnoreCase("Rain Showers")
					|| icon.equalsIgnoreCase("Heavy Rain") || icon.equalsIgnoreCase("Rain Likely") || icon.equalsIgnoreCase("Water Spouts")) {
				if (isDay) {
					return R.drawable.shower2;
				} else {
					return R.drawable.shower2_night;
				}
			} else if (icon.equalsIgnoreCase("Slight Chance Snow Showers") || icon.equalsIgnoreCase("Chance Snow Showers")
					|| icon.equalsIgnoreCase("Slight Chance Snow")) {
				if (isDay) {
					return R.drawable.snow2;
				} else {
					return R.drawable.snow2_night;
				}
			} else if (icon.equalsIgnoreCase("Chance Snow") || icon.equalsIgnoreCase("Snow Showers") || icon.equalsIgnoreCase("Snow Showers Likely")) {
				if (isDay) {
					return R.drawable.snow3;
				} else {
					return R.drawable.snow3_night;
				}
			} else if (icon.equalsIgnoreCase("Chance Thunderstorms") || icon.equalsIgnoreCase("Isolated Thunderstorms")
					|| icon.equalsIgnoreCase("Slight Chance Thunderstorms") || icon.equalsIgnoreCase("Isolated Tstms")
					|| icon.equalsIgnoreCase("Slight Chc Tstms") || icon.equalsIgnoreCase("Chance Tstms")) {
				if (isDay) {
					return R.drawable.tstorm1;
				} else {
					return R.drawable.tstorm1_night;
				}
			} else if (icon.equalsIgnoreCase("Thunderstorms") || icon.equalsIgnoreCase("Thunderstorms Likely") || icon.equalsIgnoreCase("Severe Tstms")) {
				if (isDay) {
					return R.drawable.tstorm2;
				} else {
					return R.drawable.tstorm2_night;
				}
			} else if (icon.equalsIgnoreCase("Fog") || icon.equalsIgnoreCase("Haze") || icon.equalsIgnoreCase("Patchy Fog")
					|| icon.equalsIgnoreCase("Dense Fog") || icon.equalsIgnoreCase("Areas Fog") || icon.equalsIgnoreCase("Patchy Freezing Fog")
					|| icon.equalsIgnoreCase("Areas Freezing Fog") || icon.equalsIgnoreCase("Freezing Fog")) {
				if (isDay) {
					return R.drawable.fog;
				} else {
					return R.drawable.fog_night;
				}
			} else if (icon.equalsIgnoreCase("Patchy Haze") || icon.equalsIgnoreCase("Areas Haze") || icon.equalsIgnoreCase("Patchy Smoke")
					|| icon.equalsIgnoreCase("Areas Smoke") || icon.equalsIgnoreCase("Smoke") || icon.equalsIgnoreCase("Patchy Ash")
					|| icon.equalsIgnoreCase("Areas Ash") || icon.equalsIgnoreCase("Volcanic Ash") || icon.equalsIgnoreCase("Blowing Dust")
					|| icon.equalsIgnoreCase("Blowing Sand")) {
				if (isDay) {
					return R.drawable.mist;
				} else {
					return R.drawable.mist_night;
				}
			} else if (icon.equalsIgnoreCase("ICY") || icon.equalsIgnoreCase("Patchy Ice Crystals") || icon.equalsIgnoreCase("Areas Ice Crystals")
					|| icon.equalsIgnoreCase("Ice Crystals") || icon.equalsIgnoreCase("Patchy Ice Fog") || icon.equalsIgnoreCase("Areas Ice Fog")
					|| icon.equalsIgnoreCase("Ice Fog")) {
				if (isDay) {
					return R.drawable.hail;
				} else {
					return R.drawable.hail;
				}
			} else if (icon.equalsIgnoreCase("OVERCAST") || icon.indexOf("Overcast") != -1) {
				if (isDay) {
					return R.drawable.overcast;
				} else {
					return R.drawable.overcast;
				}
			} else if (icon.equalsIgnoreCase("Slight Chance Flurries") || icon.equalsIgnoreCase("Blowing Snow") || icon.equalsIgnoreCase("Chance Flurries")
					|| icon.equalsIgnoreCase("Flurries Likely") || icon.equalsIgnoreCase("Flurries")) {
				if (isDay) {
					return R.drawable.snow5;
				} else {
					return R.drawable.snow5;
				}
			} else if (icon.equalsIgnoreCase("Windy") || icon.equalsIgnoreCase("Blustery") || icon.equalsIgnoreCase("Breezy")) {
				if (isDay) {
					return R.drawable.windy;
				} else {
					return R.drawable.windy;
				}
			} else if (icon.indexOf("Thunderstorm") != -1 || icon.indexOf("Tstm") != -1) {
				if (isDay) {
					return R.drawable.tstorm2;
				} else {
					return R.drawable.tstorm2_night;
				}
			} else if (icon.indexOf("Cloudy") != -1 || icon.indexOf("Clouds") != -1) {
				if (isDay) {
					return R.drawable.cloudy2;
				} else {
					return R.drawable.cloudy2_night;
				}
			} else if (icon.indexOf("Fog") != -1) {
				if (isDay) {
					return R.drawable.fog;
				} else {
					return R.drawable.fog_night;
				}
			} else if (icon.indexOf("Sleet") != -1 || icon.indexOf("Freezing") != -1 || (icon.indexOf("Rain") != -1 && icon.indexOf("Snow") != -1)
					|| (icon.indexOf("Drizzle") != -1 && icon.indexOf("Snow") != -1)) {
				if (isDay) {
					return R.drawable.sleet;
				} else {
					return R.drawable.sleet;
				}
			} else if (icon.indexOf("Pellets") != -1 || icon.indexOf("Hail") != -1 || icon.indexOf("Ice") != -1 || icon.indexOf("Icy") != -1) {
				if (isDay) {
					return R.drawable.hail;
				} else {
					return R.drawable.hail;
				}
			} else if (icon.indexOf("Showers") != -1) {
				if (isDay) {
					return R.drawable.shower2;
				} else {
					return R.drawable.shower2_night;
				}
			} else if (icon.indexOf("Snow") != -1) {
				if (isDay) {
					return R.drawable.snow3;
				} else {
					return R.drawable.snow3_night;
				}
			} else if (icon.indexOf("Windy") != -1) {
				if (isDay) {
					return R.drawable.windy;
				} else {
					return R.drawable.windy;
				}
			} else if (icon.indexOf("Drizzle") != -1 || icon.indexOf("Rain") != -1) {
				if (isDay) {
					return R.drawable.shower1;
				} else {
					return R.drawable.shower1_night;
				}
			} else if (icon.indexOf("Haze") != -1 || icon.indexOf("Dust") != -1 || icon.indexOf("Sand") != -1 || icon.indexOf("Fog") != -1
					|| icon.indexOf("Smoke") != -1 || icon.indexOf("Ash") != -1) {
				if (isDay) {
					return R.drawable.fog;
				} else {
					return R.drawable.fog_night;
				}
			}
		}
		if (isDay) {
			return R.drawable.sunny;
		} else {
			return R.drawable.sunny_night;
		}

	}

}