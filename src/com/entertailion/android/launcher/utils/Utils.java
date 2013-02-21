/*
 * Copyright (C) 2012 ENTERTAILION LLC
 * Copyright (C) 2008 The Android Open Source Project
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
package com.entertailion.android.launcher.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;

import com.entertailion.android.launcher.R;

/**
 * Utility class.
 * 
 * Some code from:
 * https://github.com/AnderWeb/android_packages_apps_Launcher/tree/froyo
 * 
 * @author leon_nicholls
 */
public class Utils {
	private static final String LOG_TAG = "Utils";

	private static final char[] ALPHABET = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
			'X', 'Y', 'Z' };

	private static int utilsIconWidth = -1;
	private static int utilsIconHeight = -1;
	private static final Paint utilsPaint = new Paint();
	private static final Rect utilsBounds = new Rect();
	private static final Rect utilsOldBounds = new Rect();
	private static final Canvas utilsCanvas = new Canvas();

	static {
		utilsCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
	}

	/**
	 * Returns a Drawable representing the thumbnail of the specified Drawable.
	 * The size of the thumbnail is defined by the dimension
	 * android.R.dimen.launcher_application_icon_size.
	 * 
	 * This method is not thread-safe and should be invoked on the UI thread
	 * only.
	 * 
	 * @param icon
	 *            The icon to get a thumbnail of.
	 * @param context
	 *            The application's context.
	 * 
	 * @return A thumbnail for the specified icon or the icon itself if the
	 *         thumbnail could not be created.
	 */
	public static final Drawable createIconThumbnail(Drawable icon, Context context) {
		if (icon != null) {
			if (utilsIconWidth == -1) {
				final Resources resources = context.getResources();
				utilsIconWidth = utilsIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
			}
			int width = utilsIconWidth;
			int height = utilsIconHeight;

			float scale = 1.0f;
			if (icon instanceof PaintDrawable) {
				PaintDrawable painter = (PaintDrawable) icon;
				painter.setIntrinsicWidth(width);
				painter.setIntrinsicHeight(height);
			} else if (icon instanceof BitmapDrawable) {
				// Ensure the bitmap has a density.
				BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
					bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
				}
			}
			int iconWidth = icon.getIntrinsicWidth();
			int iconHeight = icon.getIntrinsicHeight();

			if (width > 0 && height > 0) {
				if (width < iconWidth || height < iconHeight || scale != 1.0f) {
					final float ratio = (float) iconWidth / iconHeight;

					if (iconWidth > iconHeight) {
						height = (int) (width / ratio);
					} else if (iconHeight > iconWidth) {
						width = (int) (height * ratio);
					}

					final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
					final Bitmap thumb = Bitmap.createBitmap(utilsIconWidth, utilsIconHeight, c);
					final Canvas canvas = utilsCanvas;
					canvas.setBitmap(thumb);
					// Copy the old bounds to restore them later
					// If we were to do oldBounds = icon.getBounds(),
					// the call to setBounds() that follows would
					// change the same instance and we would lose the
					// old bounds
					utilsOldBounds.set(icon.getBounds());
					final int x = (utilsIconWidth - width) / 2;
					final int y = (utilsIconHeight - height) / 2;
					icon.setBounds(x, y, x + width, y + height);
					icon.draw(canvas);
					icon.setBounds(utilsOldBounds);
					icon = new FastBitmapDrawable(thumb);
				} else if (iconWidth < width && iconHeight < height) {
					final Bitmap.Config c = Bitmap.Config.ARGB_8888;
					final Bitmap thumb = Bitmap.createBitmap(utilsIconWidth, utilsIconHeight, c);
					final Canvas canvas = utilsCanvas;
					canvas.setBitmap(thumb);
					utilsOldBounds.set(icon.getBounds());
					final int x = (width - iconWidth) / 2;
					final int y = (height - iconHeight) / 2;
					icon.setBounds(x, y, x + iconWidth, y + iconHeight);
					icon.draw(canvas);
					icon.setBounds(utilsOldBounds);
					icon = new FastBitmapDrawable(thumb);
				}
			}
		}

		return icon;
	}

	/**
	 * Returns a Bitmap representing the thumbnail of the specified Bitmap. The
	 * size of the thumbnail is defined by the dimension
	 * android.R.dimen.launcher_application_icon_size.
	 * 
	 * This method is not thread-safe and should be invoked on the UI thread
	 * only.
	 * 
	 * @param bitmap
	 *            The bitmap to get a thumbnail of.
	 * @param context
	 *            The application's context.
	 * 
	 * @return A thumbnail for the specified bitmap or the bitmap itself if the
	 *         thumbnail could not be created.
	 */
	public static final Bitmap createBitmapThumbnail(Bitmap bitmap, Context context) {
		if (utilsIconWidth == -1) {
			final Resources resources = context.getResources();
			utilsIconWidth = utilsIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
		}

		int width = utilsIconWidth;
		int height = utilsIconHeight;

		final int bitmapWidth = bitmap.getWidth();
		final int bitmapHeight = bitmap.getHeight();

		if (width > 0 && height > 0) {
			if (width < bitmapWidth || height < bitmapHeight) {
				final float ratio = (float) bitmapWidth / bitmapHeight;

				if (bitmapWidth > bitmapHeight) {
					height = (int) (width / ratio);
				} else if (bitmapHeight > bitmapWidth) {
					width = (int) (height * ratio);
				}

				final Bitmap.Config c = (width == utilsIconWidth && height == utilsIconHeight && bitmap.getConfig() != null) ? bitmap.getConfig()
						: Bitmap.Config.ARGB_8888;
				final Bitmap thumb = Bitmap.createBitmap(utilsIconWidth, utilsIconHeight, c);
				final Canvas canvas = utilsCanvas;
				final Paint paint = utilsPaint;
				canvas.setBitmap(thumb);
				paint.setDither(false);
				paint.setFilterBitmap(true);
				utilsBounds.set((utilsIconWidth - width) / 2, (utilsIconHeight - height) / 2, width, height);
				utilsOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
				canvas.drawBitmap(bitmap, utilsOldBounds, utilsBounds, paint);
				return thumb;
			} else if (bitmapWidth < width || bitmapHeight < height) {
				final Bitmap.Config c = Bitmap.Config.ARGB_8888;
				final Bitmap thumb = Bitmap.createBitmap(utilsIconWidth, utilsIconHeight, c);
				final Canvas canvas = utilsCanvas;
				final Paint paint = utilsPaint;
				canvas.setBitmap(thumb);
				paint.setDither(false);
				paint.setFilterBitmap(true);
				canvas.drawBitmap(bitmap, (utilsIconWidth - bitmapWidth) / 2, (utilsIconHeight - bitmapHeight) / 2, paint);
				return thumb;
			}
		}

		return bitmap;
	}

	/**
	 * ADW Create an icon drawable with reflection :P Thanks to
	 * http://www.inter-fuser.com/2009/12/android-reflections-with-bitmaps.html
	 * 
	 * @param icon
	 * @param context
	 * @return
	 */
	public static final Drawable drawReflection(Drawable icon, Context context) {
		final Resources resources = context.getResources();
		utilsIconWidth = utilsIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
		// The gap we want between the reflection and the original image
		final float scale = 1.30f;

		int width = utilsIconWidth;
		int height = utilsIconHeight;
		float ratio = utilsIconHeight / (utilsIconHeight * scale);
		Bitmap original;
		try {
			original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return icon;
		}
		final Canvas cv = new Canvas();
		cv.setBitmap(original);
		icon.setBounds(0, 0, width, height);
		icon.draw(cv);
		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage;
		try {
			reflectionImage = Bitmap.createBitmap(original, 0, height / 2, width, height / 2, matrix, false);
		} catch (OutOfMemoryError e) {
			return new FastBitmapDrawable(original);
		}

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap bitmapWithReflection;
		try {
			bitmapWithReflection = Bitmap.createBitmap(width, (int) (height * scale), Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return new FastBitmapDrawable(original);
		}

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(bitmapWithReflection);
		// Draw in the gap
		// Paint deafaultPaint = new Paint();
		// canvas.drawRect(0, height, width, height + reflectionGap,
		// deafaultPaint);
		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, height - 6, null);

		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, original.getHeight(), 0, bitmapWithReflection.getHeight(), 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);
		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, height - 6, width, bitmapWithReflection.getHeight(), paint);
		// Draw in the original image
		canvas.drawBitmap(original, 0, 0, null);
		original.recycle();
		reflectionImage.recycle();
		try {
			return new FastBitmapDrawable(Bitmap.createScaledBitmap(bitmapWithReflection, Math.round((float) utilsIconWidth * ratio), utilsIconHeight, true));
		} catch (OutOfMemoryError e) {
			return icon;
		}
	}

	/**
	 * ADW Create an icon drawable scaled Used for Action Buttons
	 * 
	 * @param icon
	 * @param context
	 * @param tint
	 * @return
	 */
	public static final Drawable scaledDrawable(Drawable icon, Context context, boolean tint, float scale, int color) {
		final Resources resources = context.getResources();
		utilsIconWidth = utilsIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);

		int width = utilsIconWidth;
		int height = utilsIconHeight;
		Bitmap original;
		try {
			original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return icon;
		}
		Canvas canvas = new Canvas(original);
		canvas.setBitmap(original);
		icon.setBounds(0, 0, width, height);
		icon.draw(canvas);

		if (tint) {
			Paint paint = new Paint();
			LinearGradient shader = new LinearGradient(width / 2, 0, width / 2, height,
					Color.argb(220, Color.red(color), Color.green(color), Color.blue(color)), Color.argb(50, Color.red(color), Color.green(color),
							Color.blue(color)), TileMode.CLAMP);
			paint.setShader(shader);
			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawRect(0, 0, width, height, paint);
		}
		try {
			Bitmap endImage = Bitmap.createScaledBitmap(original, (int) (width * scale), (int) (height * scale), true);
			original.recycle();
			return new FastBitmapDrawable(endImage);
		} catch (OutOfMemoryError e) {
			return icon;
		}
	}

	/**
	 * ADW: Use donut syule wallpaper rendering, we need this method to fit
	 * wallpaper bitmap
	 */
	public static final Bitmap centerToFit(Bitmap bitmap, int width, int height, Context context) {
		final int bitmapWidth = bitmap.getWidth();
		final int bitmapHeight = bitmap.getHeight();

		if (bitmapWidth < width || bitmapHeight < height) {
			int color = context.getResources().getColor(R.color.transparent);

			Bitmap centered = Bitmap.createBitmap(bitmapWidth < width ? width : bitmapWidth, bitmapHeight < height ? height : bitmapHeight,
					Bitmap.Config.RGB_565);
			centered.setDensity(bitmap.getDensity());
			Canvas canvas = new Canvas(centered);
			canvas.drawColor(color);
			canvas.drawBitmap(bitmap, (width - bitmapWidth) / 2.0f, (height - bitmapHeight) / 2.0f, null);

			bitmap = centered;
		}

		return bitmap;
	}

	public static final void uninstallPackage(Context context, String packageName) {
		Uri packageUri = Uri.parse(packageName);
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
		context.startActivity(uninstallIntent);
	}

	/**
	 * Clean a string so it can be used for a file name
	 * 
	 * @param value
	 * @return
	 */
	public static final String clean(String value) {
		return value.replaceAll(":", "_").replaceAll("/", "_").replaceAll("\\\\", "_").replaceAll("\\?", "_").replaceAll("#", "_");
	}

	/**
	 * Escape XML entities
	 * 
	 * @param aText
	 * @return
	 */
	public static final String escapeXML(String aText) {
		if (null == aText) {
			return "";
		}
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else if (character == '\"') {
				result.append("&quot;");
			} else if (character == '\'') {
				result.append("&#039;");
			} else if (character == '&') {
				result.append("&amp;");
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	/**
	 * Show Android system statusbar
	 * 
	 * @param context
	 */
	public static final void showStatusbar(Context context) {
		try {
			Object service = context.getSystemService("statusbar");
			if (service != null) {
				Method expand = service.getClass().getMethod("expand");
				expand.invoke(service);
			}
		} catch (Exception e) {
			Log.d(LOG_TAG, "showStatusbar", e);
		}
	}

	public static final Bitmap getBitmapFromURL(String src) {
		try {
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			int size = Math.max(myBitmap.getWidth(), myBitmap.getHeight());
			Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			c.drawBitmap(myBitmap, (size - myBitmap.getWidth()) / 2, (size - myBitmap.getHeight()) / 2, paint);
			return b;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Faild to get the image from URL:" + src, e);
			return null;
		}
	}

	public static final String getVersion(Context context) {
		String versionString = context.getString(R.string.unknown_build);
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionString = info.versionName;
		} catch (Exception e) {
			// do nothing
		}
		return versionString;
	}

	public static final void logDeviceInfo(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			Log.i(LOG_TAG, "Version=" + pi.versionName);
			Log.i(LOG_TAG, "IP Address=" + Utils.getLocalIpAddress());
			Log.i(LOG_TAG, "android.os.Build.VERSION.RELEASE=" + android.os.Build.VERSION.RELEASE);
			Log.i(LOG_TAG, "android.os.Build.VERSION.INCREMENTAL=" + android.os.Build.VERSION.INCREMENTAL);
			Log.i(LOG_TAG, "android.os.Build.DEVICE=" + android.os.Build.DEVICE);
			Log.i(LOG_TAG, "android.os.Build.MODEL=" + android.os.Build.MODEL);
			Log.i(LOG_TAG, "android.os.Build.PRODUCT=" + android.os.Build.PRODUCT);
			Log.i(LOG_TAG, "android.os.Build.MANUFACTURER=" + android.os.Build.MANUFACTURER);
			Log.i(LOG_TAG, "android.os.Build.BRAND=" + android.os.Build.BRAND);
		} catch (NameNotFoundException e) {
			Log.e(LOG_TAG, "logDeviceInfo", e);
		}
	}

	public static final String getLocalIpAddress() {
		InetAddress inetAddress = Utils.getLocalInetAddress();
		if (inetAddress != null) {
			return inetAddress.getHostAddress().toString();
		}
		return null;
	}

	public static final InetAddress getLocalInetAddress() {
		InetAddress selectedInetAddress = null;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (intf.isUp()) {
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							if (inetAddress instanceof Inet4Address) { // only
																		// want
																		// ipv4
																		// address
								if (inetAddress.getHostAddress().toString().charAt(0) != '0') {
									if (selectedInetAddress == null) {
										selectedInetAddress = inetAddress;
									} else if (intf.getName().startsWith("eth")) { // prefer
																					// wired
																					// interface
										selectedInetAddress = inetAddress;
									}
								}
							}
						}
					}
				}
			}
			return selectedInetAddress;
		} catch (Throwable e) {
			Log.e(LOG_TAG, "Failed to get the IP address", e);
		}
		return null;
	}

	public static final boolean isVizioCoStar() {
		return android.os.Build.PRODUCT.equals("StreamPlayer") && android.os.Build.MANUFACTURER.equals("VIZIO");
	}

	public static final void showNotifications(Context context) {
		if (Utils.isVizioCoStar()) {
			// Vizio broke the statusbar logic for showing notifications
			// Find their activity to show notifications instead
			// com.vizio.tv.notification/.Activities.NotificationActivity
			PackageManager manager = context.getPackageManager();

			Intent notificationIntent = new Intent("android.intent.action.SHOW_NOTIFICATION", null);
			notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);

			final List<ResolveInfo> activities = manager.queryIntentActivities(notificationIntent, 0);

			if (activities.size() > 0) {
				ResolveInfo info = activities.get(0);
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} else {
				Log.w(LOG_TAG, "no notifications activity");
			}
		} else {
			// Show the statusbar which has the notifications
			Utils.showStatusbar(context);
		}
	}

	public static final void showSystemSettings(Context context) {
		String settingsAction = "android.settings.SETTINGS";
		if (Utils.isVizioCoStar()) {
			// Vizio has a custom system settings app
			settingsAction = "viaplus.intent.settings.SETTINGS";
		}

		PackageManager manager = context.getPackageManager();

		Intent systemIntent = new Intent(settingsAction, null);

		final List<ResolveInfo> activities = manager.queryIntentActivities(systemIntent, 0);

		if (activities.size() > 0) {
			ResolveInfo info = activities.get(0);
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} else {
			Log.w(LOG_TAG, "no systems activity");
		}
	}

	public static final boolean isUsa() {
		return Locale.getDefault().equals(Locale.US);
	}

	public static final LocationData getLocationData(Context context) {
		try {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			Location location = locationManager.getLastKnownLocation("static");
			LocationData locationData = new LocationData();
			locationData.setLongitude(location.getLongitude());
			locationData.setLatitude(location.getLatitude());
			Geocoder geocoder = new Geocoder(context);
			Address address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
			locationData.setZipcode(address.getPostalCode());
			locationData.setCountryCode(address.getCountryCode());
			locationData.setCountryName(address.getCountryName());
			return locationData;
		} catch (Exception e) {
			Log.e(LOG_TAG, "getLocationData", e);
		}
		return null;
	}

	public synchronized static final String getCachedData(Context context, String url, boolean refresh) {
		Log.d(LOG_TAG, "getCachedData: " + url);
		String data = null;
		boolean exists = false;
		String cleanUrl = "cache." + clean(url);
		File file = context.getFileStreamPath(cleanUrl);
		if (file != null && file.exists()) {
			exists = true;
		}

		if (!refresh && exists) {
			try {
				FileInputStream fis = context.openFileInput(cleanUrl);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				StringBuffer buffer = new StringBuffer();
				for (String line; (line = br.readLine()) != null;) {
					buffer.append(line);
				}
				fis.close();
				data = buffer.toString();
			} catch (Exception e) {
				Log.e(LOG_TAG, "Error getData: " + url, e);
			}
		} else {
			boolean found = false;
			StringBuilder builder = new StringBuilder();
			try {
				InputStream stream = new HttpRequestHelper().getHttpStream(url);
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				stream.close();
				found = true;
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error getData: " + url, e);
			} catch (Exception e) {
				Log.e(LOG_TAG, "stream is NULL");
			}
			data = builder.toString();
			if (!found && exists) {
				try {
					FileInputStream fis = context.openFileInput(cleanUrl);
					BufferedReader br = new BufferedReader(new InputStreamReader(fis));
					StringBuffer buffer = new StringBuffer();
					for (String line; (line = br.readLine()) != null;) {
						buffer.append(line);
					}
					fis.close();
					data = buffer.toString();
				} catch (Exception e) {
					Log.e(LOG_TAG, "Error getData: " + url, e);
				}
			}
			if (data != null && data.trim().length() > 0) {
				try {
					FileOutputStream fos = context.openFileOutput(cleanUrl, Context.MODE_PRIVATE);
					fos.write(data.getBytes());
					fos.close();
				} catch (FileNotFoundException e) {
					Log.e(LOG_TAG, "Error getData: " + url, e);
				} catch (IOException e) {
					Log.e(LOG_TAG, "Error getData: " + url, e);
				}
			}
		}
		return data;
	}

	public static final float fahrenheitToCelsius(float fahrenheit) {
		return ((5.0f / 9.0f) * (fahrenheit - 32));
	}

	public static final float celsiusToFahrenheit(float celsius) {
		return ((9.0f / 5.0f) * celsius + 32);
	}

	public static final char keyCodeToLetter(int keyCode) {
		return ALPHABET[keyCode - KeyEvent.KEYCODE_A];
	}

	public static final Bitmap crop(Bitmap bitmap, int xSide, int ySide) {
		int origWidth = bitmap.getWidth();
		int origHeight = bitmap.getHeight();
		int targetWidth = origWidth - 2 * xSide;
		int targetHeight = origHeight - 2 * ySide;
		return Bitmap.createBitmap(bitmap, xSide, ySide, targetWidth, targetHeight).copy(Bitmap.Config.ARGB_8888, true);
	}

	public static final void saveToFile(Context context, Bitmap bitmap, int targetWidth, int targetHeight, String fileName) throws IOException {
		FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
		// FileOutputStream fos = new FileOutputStream(fileName);
		if (bitmap.getWidth() == targetWidth && bitmap.getHeight() == targetHeight) {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		} else {
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
			scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		}
		fos.close();
	}

	public static final Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	/**
	 * Determine if there is a high resolution icon available for the web site.
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	public static final String getWebSiteIcon(Context context, String url) {
		String icon = null;
		if (url != null) {
			String data = Utils.getCachedData(context, url, true);
			if (data != null) {
				Document doc = Jsoup.parse(data);
				if (doc != null) {
					String href = null;
					Elements metas = doc.select("meta[itemprop=image]");
					if (metas.size() > 0) {
						Element meta = metas.first();
						href = meta.attr("abs:content");
						// weird jsoup bug: abs doesn't always work
						if (href==null || href.trim().length()==0) {
							href = url + meta.attr("content");
						}
					}
					if (href == null || href.trim().length() == 0) {
						// Find the Microsoft tile icon
						metas = doc.select("meta[name=msapplication-TileImage]");
						if (metas.size() > 0) {
							Element meta = metas.first();
							href = meta.attr("abs:content");
							// weird jsoup bug: abs doesn't always work
							if (href==null || href.trim().length()==0) {
								href = url + meta.attr("content");
							}
						}
					}
					if (href == null || href.trim().length() == 0) {
						// Find the Apple touch icon
						Elements links = doc.select("link[rel=apple-touch-icon]");
						if (links.size() > 0) {
							Element link = links.first();
							href = link.attr("abs:href");
							// weird jsoup bug: abs doesn't always work
							if (href==null || href.trim().length()==0) {
								href = url +link.attr("href");
							}
						}
					}
					if (href == null || href.trim().length() == 0) {
						// Find the Facebook open graph icon
						metas = doc.select("meta[property=og:image]");
						if (metas.size() > 0) {
							Element link = metas.first();
							href = link.attr("abs:content");
							// weird jsoup bug: abs doesn't always work
							if (href==null || href.trim().length()==0) {
								href = url +link.attr("content");
							}
						}
					}
					if (href == null || href.trim().length() == 0) {
						// Find the Twitter card icon
						metas = doc.select("meta[name=twitter:image]");
						if (metas.size() > 0) {
							Element link = metas.first();
							href = link.attr("abs:value");
							// weird jsoup bug: abs doesn't always work
							if (href==null || href.trim().length()==0) {
								href = url +link.attr("value");
							}
						}
					}
					if (href == null || href.trim().length() == 0) {
						metas = doc.select("link[itemprop=thumbnailUrl]");
						if (metas.size() > 0) {
							Element link = metas.first();
							href = link.attr("abs:href");
							// weird jsoup bug: abs doesn't always work
							if (href==null || href.trim().length()==0) {
								href = url +link.attr("href");
							}
						}
					}
					if (href != null && href.trim().length() > 0) {
						try {
							Bitmap bitmap = Utils.getBitmapFromURL(href);
							if (bitmap != null) {
								icon = "web_site_icon_" + Utils.clean(href) + ".png";
								Utils.saveToFile(context, bitmap, bitmap.getWidth(), bitmap.getHeight(), icon);
								bitmap.recycle();
							}
						} catch (Exception e) {
							Log.d(LOG_TAG, "getWebSiteIcon", e);
						}
					}
				}
			}
		}
		return icon;
	}

	public static final void launchLiveTV(Context context) {
		Log.d(LOG_TAG, "launchLiveTV");
		PackageManager manager = context.getPackageManager();
		Intent notificationIntent = new Intent("com.google.android.tv.intent.action.TV", null);
		notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);

		final List<ResolveInfo> activities = manager.queryIntentActivities(notificationIntent, 0);
		if (activities.size() > 0) {
			// Intent { act=android.intent.action.VIEW dat=tv://passthrough
			// flg=0x10200000 cmp=com.google.tv.player/.PlayerActivity }
			ResolveInfo info = activities.get(0);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tv://passthrough"));
			intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			context.startActivity(intent);
		} else {
			Log.w(LOG_TAG, "no live TV activity");
		}
	}

	// FCC TV data: http://transition.fcc.gov/mb/databases/cdbs/
	// (http://transition.fcc.gov/mb/video/tvq.html)
	// Facility data:
	// http://transition.fcc.gov/ftp/Bureaus/MB/Databases/cdbs/facility.zip
	// Clean data: Column AA not empty AND colum AC not empty; keep columns F,
	// AA; remove '-TV', remove '-DT'
	// Read from static asset file since data doesn't change that often
	public static final Hashtable<String, String> readUsLocalCallsigns(Context context) {
		Hashtable<String, String> localCallsigns = new Hashtable<String, String>(1800);
		try {
			InputStream is = context.getAssets().open("ustvcallsigns.csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			Pattern pattern = Pattern.compile("\t");
			while ((line = reader.readLine()) != null) {
				String[] temp = pattern.split(line);
				if (temp.length == 2) {
					localCallsigns.put(temp[0], temp[1]);
				}
			}
			is.close();
		} catch (Throwable x) {
		}
		return localCallsigns;
	}

	public static final String stripBrackets(String n) {
		if (n.endsWith(")")) { // remove ()
			int index = n.lastIndexOf("(");
			if (index != -1) {
				n = n.substring(0, index - 1).trim();
			}
		}
		return n;
	}
	
	public static final String readAssetFile(Context context, String filename) {
		StringBuffer buffer = new StringBuffer();
		try {
			InputStream is = context.getAssets().open(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = reader.readLine()) != null) {
				buffer.append(line).append('\n');
			}
			is.close();
		} catch (Throwable x) {
		}
		return buffer.toString();
	}

}
