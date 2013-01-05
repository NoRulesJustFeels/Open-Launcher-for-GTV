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

import java.io.StringReader;
import java.net.SocketException;
import java.util.Calendar;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.util.Log;

import com.entertailion.android.launcher.LauncherApplication;
import com.entertailion.android.launcher.utils.LocationData;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Download the lates weather data.
 * 
 * @author leon_nicholls
 * 
 */
public class ProcessWeather implements Runnable {
	private static final String LOG_TAG = "ProcessWeather";

	public static String NOAA_URL = "http://forecast.weather.gov/MapClick.php?FcstType=dwml";

	private Context context = null;

	public ProcessWeather(Context context) {
		this.context = context;
	}

	@Override
	public void run() {
		LocationData locationData = ((LauncherApplication) context.getApplicationContext()).getLocationData();
		if (locationData != null) {
			updateNoaa(locationData.getLatitude(), locationData.getLongitude());
		}
	}

	// http://en.wikipedia.org/wiki/Severe_weather_terminology_(United_States)
	private void updateNoaa(double latitudeValue, double longitudeValue) {
		Log.i(LOG_TAG, "About to get NOAA data");
		try {
			final String queryString = NOAA_URL + "&lat=" + latitudeValue + "&lon=" + longitudeValue; // http://forecast.weather.gov/MapClick.php?FcstType=dwml&lat=33.07871627807617&lon=-96.80830383300781
			// String queryString =
			// "http://forecast.weather.gov/MapClick.php?FcstType=dwml&lat=33.086936950683594&lon=-96.7601089477539";
			// String queryString =
			// "http://forecast.weather.gov/MapClick.php?FcstType=dwml&lat=30.1011833&lon=-89.99067339999999";
			String data = Utils.getCachedData(context, queryString, true);
			// data = Utils.readAssetFile(context, "weather.xml"); // testing
			InputSource inStream = new org.xml.sax.InputSource();
			inStream.setCharacterStream(new StringReader(data));
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			NoaaWeatherHandler noaaWeatherHandler = new NoaaWeatherHandler(isDay());
			xr.setContentHandler(noaaWeatherHandler);
			Log.d(LOG_TAG, "Retrieving weather data from: " + queryString);
			xr.parse(inStream);

			WeatherSet currentWeather = noaaWeatherHandler.getWeatherSet();

			boolean alertFound = false;
			if (currentWeather != null && currentWeather.getHazardUrl() != null) {
				if (!currentWeather.getHazard().equalsIgnoreCase("Child Abduction Emergency")) {
					// /data = new
					// HttpRequestHelper().sendGet(currentWeather.getHazardUrl());
					data = Utils.getCachedData(context, currentWeather.getHazardUrl(), true);
					// http://forecast.weather.gov/showsigwx.php?warnzone=TXZ104&warncounty=TXC085&firewxzone=TXZ104&local_place1=4+Miles+NW+Plano+TX&product1=Short+Term+Forecast
					// http://forecast.weather.gov/showsigwx.php?warnzone=TXZ104&warncounty=TXC085&firewxzone=TXZ104&local_place1=4+Miles+NW+Plano+TX&product1=Dense+Fog+Advisory
					// http://forecast.weather.gov/showsigwx.php?warnzone=TXZ104&warncounty=TXC085&firewxzone=TXZ104&local_place1=4+Miles+NW+Plano+TX&product1=Hazardous+Weather+Outlook
					if (data != null) {
						Document doc = Jsoup.parse(data);
						if (doc != null) {
							Elements headings = doc.getElementsByTag("h3");
							String headingText = currentWeather.getHazard();
							if (headings.size() > 0) {
								Element heading = headings.first();
								headingText = heading.text();
							}
							if (headingText.equalsIgnoreCase(currentWeather.getHazard())) {
								Elements elements = doc.select("pre");
								if (elements != null) {
									Element link = elements.first();
									if (link != null) {
										data = link.text();
										String alert = extractAlert(currentWeather.getHazard(), data);
										currentWeather.setHazard(alert);
										alertFound = true;
									}
								}
							}
						}
					}
				}
				if (!alertFound) {
					currentWeather.setHazard(null);
				}
			}
			((LauncherApplication) context.getApplicationContext()).setWeatherSet(currentWeather);
		} catch (SocketException sex) {
			Log.e(LOG_TAG, "updateNoaa", sex);
		} catch (Exception e) {
			Log.e(LOG_TAG, "updateNoaa", e);
		}
	}

	private boolean isDay() {
		Calendar now = Calendar.getInstance();
		int hour = now.get(Calendar.HOUR_OF_DAY);
		return hour < 19 && hour > 5;
	}

	private String extractAlert(String alert, String data) {
		String result = alert;
		if (data != null) {
			if (alert.equalsIgnoreCase("Child Abduction Emergency")) {
				result = null; // ignore for now
			} else {
				String status = "";
				String[] lines = data.split("\\n");
				boolean inStatus = false;
				boolean dayOne = false;
				if (alert.equalsIgnoreCase("Special Weather Statement")) {
					int emptyCount = 0;
					for (int i = 0; i < lines.length; i++) {
						if (lines[i].trim().length() == 0) {
							emptyCount++;
						} else if (emptyCount >= 3) {
							status = status + " " + lines[i];
						}
					}
				} else if (alert.equalsIgnoreCase("Short Term Forecast")) {
					boolean foundNow = false;
					for (int i = 0; i < lines.length; i++) {
						int index = lines[i].indexOf(".NOW...");
						if (lines[i].trim().length() == 0) {
							if (status.length() > 0) {
								foundNow = false;
								if (!status.trim().startsWith("AT")) {
									status = status.replaceAll("\\.\\.\\.", "\\.");
								}
								break;
							}
						} else if (foundNow || index != -1) {
							if (index != -1) {
								foundNow = true;
							} else {
								status = status + " " + lines[i];
							}
						}
					}
				} else {
					for (int i = 0; i < lines.length; i++) {
						if (lines[i].trim().length() == 0) {
							if (status.length() > 0) {
								inStatus = false;
								break;
							}
						} else if (inStatus || lines[i].startsWith(".")) {
							if (!lines[i].startsWith(".DAY ONE")) {
								if (status.length() == 0) {
									status = lines[i];
								} else {
									status = status + " " + lines[i];
								}
							} else {
								dayOne = true;
							}
							inStatus = true;
						}
					}
				}
				data = status.trim();
				int index = data.indexOf("...");
				if (!dayOne && index != -1) {
					data = data.substring(index + 3);
				}
				index = data.indexOf(".");
				if (index != -1) {
					data = data.substring(0, index).trim();
					if (data.trim().length() != 0) {
						result = data;
					}
				}
			}
		} else {
			result = null;
		}
		return result;
	}

}
