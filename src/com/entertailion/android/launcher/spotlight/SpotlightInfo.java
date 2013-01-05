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
package com.entertailion.android.launcher.spotlight;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;

import com.entertailion.android.launcher.Launcher;
import com.entertailion.android.launcher.R;
import com.entertailion.android.launcher.database.DatabaseHelper;
import com.entertailion.android.launcher.database.ItemsTable;
import com.entertailion.android.launcher.item.ItemInfo;
import com.entertailion.android.launcher.utils.Analytics;
import com.entertailion.android.launcher.utils.FastBitmapDrawable;
import com.entertailion.android.launcher.utils.Utils;

/**
 * Data structure for the Spotlight web apps:
 * http://www.google.com/tv/static/js/spotlight_sites.js
 * 
 * @author leon_nicholls
 * 
 */
public class SpotlightInfo extends ItemInfo {
	private static final String LOG_TAG = "SpotlightInfo";

	private String logo;
	private String icon;
	private Integer resource;

	public SpotlightInfo(int position, String title, Intent intent, String logo, String icon) {
		this(DatabaseHelper.NO_ID, position, title, intent, logo, icon);
	}

	public SpotlightInfo(int id, int position, String title, Intent intent, String logo, String icon) {
		super(id, position, title, intent);
		this.logo = logo;
		this.icon = icon;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Integer getResource() {
		return resource;
	}

	public void setResource(Integer resource) {
		this.resource = resource;
	}

	@Override
	public void invoke(Launcher context) {
		super.invoke(context);
		Analytics.logEvent(Analytics.INVOKE_SPOTLIGHT_WEB_APP);
	}

	@Override
	public void renderIcon(ImageView imageView) {

		if (resource != null) {
			imageView.setImageResource(resource);
			return;
		}

		resource = SpotlightInfo.spotlightIconMap.get(getTitle().toLowerCase());
		if (null != resource) {
			imageView.setImageResource(resource);
			return;
		}

		if (getDrawable() != null) {
			imageView.setImageDrawable(getDrawable());
			return;
		}

		if (icon != null) {
			try {
				FileInputStream fis = imageView.getContext().openFileInput(icon);
				Bitmap bitmap = BitmapFactory.decodeStream(fis);
				fis.close();
				bitmap = Utils.createBitmapThumbnail(bitmap, imageView.getContext());
				setDrawable(new FastBitmapDrawable(bitmap));
				imageView.setImageDrawable(getDrawable());
				return;
			} catch (Exception e) {
				Log.d(LOG_TAG, "renderIcon", e);
			}
		}

		super.renderIcon(imageView);
	}

	@Override
	public void persistInsert(Context context, int rowId, int position) throws Exception {
		ItemsTable.insertItem(context, rowId, position, getTitle(), getIntent(), icon, DatabaseHelper.SPOTLIGHT_TYPE);
	}

	@Override
	public void persistUpdate(Context context, int rowId, int position) throws Exception {
		ItemsTable.updateItem(context, getId(), rowId, position, getTitle(), getIntent(), icon, DatabaseHelper.SPOTLIGHT_TYPE);
	}

	@Override
	public String toString() {
		return "Spotlight [title=" + getTitle() + ", intent=" + getIntent() + ", logo=" + logo + ", icon=" + getIcon() + "]";
	}

	/**
	 * Cache of local copies of the icons for the Spotlight web apps. The icons
	 * have been resized/clipped to fit in the gallery views.
	 */
	public static Map<String, Integer> spotlightIconMap = new HashMap<String, Integer>();
	static {
		spotlightIconMap.put("adc", R.drawable.spotlight_adc);
		spotlightIconMap.put("adult swim", R.drawable.spotlight_adultswim);
		spotlightIconMap.put("aiotv", R.drawable.spotlight_aiotv);
		spotlightIconMap.put("amazon instant video", R.drawable.spotlight_amazoninstantvideo);
		spotlightIconMap.put("amos tv", R.drawable.spotlight_amostv);
		spotlightIconMap.put("asian crush", R.drawable.spotlight_asiancrush);
		spotlightIconMap.put("baeble music", R.drawable.spotlight_baeblemusic);
		spotlightIconMap.put("bg live tv", R.drawable.spotlight_bglive);
		spotlightIconMap.put("blip.tv", R.drawable.spotlight_bliptv);
		spotlightIconMap.put("c-span video library", R.drawable.spotlight_cspanvideolibrary);
		spotlightIconMap.put("cartoon network", R.drawable.spotlight_cartoonnetwork);
		spotlightIconMap.put("chow", R.drawable.spotlight_chow);
		spotlightIconMap.put("clicker", R.drawable.spotlight_clicker);
		spotlightIconMap.put("cnet", R.drawable.spotlight_cnet);
		spotlightIconMap.put("cnn", R.drawable.spotlight_cnn);
		spotlightIconMap.put("comedy time", R.drawable.spotlight_comedytime);
		spotlightIconMap.put("crackle", R.drawable.spotlight_crackle);
		spotlightIconMap.put("dailymotion", R.drawable.spotlight_dailymotion);
		spotlightIconMap.put("euronews", R.drawable.spotlight_euronews);
		spotlightIconMap.put("focus features", R.drawable.spotlight_focusfeatures);
		spotlightIconMap.put("funny or die", R.drawable.spotlight_funnyordie);
		spotlightIconMap.put("google tv help", R.drawable.spotlight_googletvhelp);
		spotlightIconMap.put("grab games", R.drawable.spotlight_grabgames);
		spotlightIconMap.put("guardian for tv", R.drawable.spotlight_guardianfortv);
		spotlightIconMap.put("hbo go", R.drawable.spotlight_hbogo);
		spotlightIconMap.put("huffington post", R.drawable.spotlight_huffingtonpost);
		spotlightIconMap.put("ign", R.drawable.spotlight_ign);
		spotlightIconMap.put("iheartradio", R.drawable.spotlight_iheartradio);
		spotlightIconMap.put("khan academy", R.drawable.spotlight_khanacademy);
		spotlightIconMap.put("kontrol.tv", R.drawable.spotlight_kontroltv);
		spotlightIconMap.put("kqed", R.drawable.spotlight_kqed);
		spotlightIconMap.put("mediawall", R.drawable.spotlight_mediawall);
		spotlightIconMap.put("meegenius!", R.drawable.spotlight_meegenius);
		spotlightIconMap.put("metacafe", R.drawable.spotlight_metacafe);
		spotlightIconMap.put("metatube", R.drawable.spotlight_metatube);
		spotlightIconMap.put("moshcam", R.drawable.spotlight_moshcam);
		spotlightIconMap.put("mspot movies", R.drawable.spotlight_mspotmovies);
		spotlightIconMap.put("net-a-porter.com", R.drawable.spotlight_netaportercom);
		spotlightIconMap.put("new york times", R.drawable.spotlight_newyorktimes);
		spotlightIconMap.put("newslook", R.drawable.spotlight_newslook);
		spotlightIconMap.put("nhl", R.drawable.spotlight_nhl);
		spotlightIconMap.put("npr", R.drawable.spotlight_npr);
		spotlightIconMap.put("o'reilly", R.drawable.spotlight_oreilly);
		spotlightIconMap.put("pbs kids", R.drawable.spotlight_pbskids);
		spotlightIconMap.put("playjam", R.drawable.spotlight_playjam);
		spotlightIconMap.put("pokerfun", R.drawable.spotlight_pokerfun);
		spotlightIconMap.put("raaga", R.drawable.spotlight_raaga);
		spotlightIconMap.put("red bull tv", R.drawable.spotlight_redbulltv);
		spotlightIconMap.put("redux", R.drawable.spotlight_redux);
		spotlightIconMap.put("revision3", R.drawable.spotlight_revision3);
		spotlightIconMap.put("russiantv", R.drawable.spotlight_russiantv);
		spotlightIconMap.put("sec", R.drawable.spotlight_sec);
		spotlightIconMap.put("shortform", R.drawable.spotlight_shortform);
		spotlightIconMap.put("slingplayer", R.drawable.spotlight_slingplayer);
		spotlightIconMap.put("snagfilms", R.drawable.spotlight_snagfilms);
		spotlightIconMap.put("soundtracker", R.drawable.spotlight_soundtracker);
		spotlightIconMap.put("tbs", R.drawable.spotlight_tbs);
		spotlightIconMap.put("the astrologer", R.drawable.spotlight_theastrologer);
		spotlightIconMap.put("the karaoke channel", R.drawable.spotlight_thekaraokechannel);
		spotlightIconMap.put("the onion", R.drawable.spotlight_theonion);
		spotlightIconMap.put("thenewcontent", R.drawable.spotlight_thenewcontent);
		spotlightIconMap.put("this week in", R.drawable.spotlight_thisweekin);
		spotlightIconMap.put("tnt", R.drawable.spotlight_tnt);
		spotlightIconMap.put("tourfactory", R.drawable.spotlight_tourfactory);
		spotlightIconMap.put("triviatv", R.drawable.spotlight_triviatv);
		spotlightIconMap.put("tune in", R.drawable.spotlight_tunein);
		spotlightIconMap.put("uinterview", R.drawable.spotlight_uinterview);
		spotlightIconMap.put("usa today", R.drawable.spotlight_usatoday);
		spotlightIconMap.put("vanguard cinema", R.drawable.spotlight_vanguardcinema);
		spotlightIconMap.put("vimeo", R.drawable.spotlight_vimeo);
		spotlightIconMap.put("watch mojo", R.drawable.spotlight_watchmojo);
		spotlightIconMap.put("wedraw", R.drawable.spotlight_wedraw);
		spotlightIconMap.put("weteli", R.drawable.spotlight_weteli);
		spotlightIconMap.put("xos college sports", R.drawable.spotlight_xoscollegesports);
	}
}
