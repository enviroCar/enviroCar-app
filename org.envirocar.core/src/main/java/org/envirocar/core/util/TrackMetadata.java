/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dewall
 */
public class TrackMetadata {

	public static final String APP_VERSION = "appVersion";
	public static final String OBD_DEVICE = "obdDevice";
	public static final String TOU_VERSION = "touVersion";
	
	private Map<String, String> entries = new HashMap<String, String>();

	public static TrackMetadata fromJson(String string) throws JSONException {
		if (string == null || string.isEmpty()) {
			throw new JSONException("Empty contents");
		}
		
		TrackMetadata result = new TrackMetadata();
		
		JSONObject json = new JSONObject(string);
		
		JSONArray names = json.names();
		for (int i = 0; i < names.length(); i++) {
			String key = names.get(i).toString();
			result.putEntry(key, json.getString(key));
		}
		
		return result;
	}
	
	public TrackMetadata() {
	}
	
//	public TrackMetadata(Context context) {
//		UserManager userManager = ((Injector) context).getObjectGraph().get(UserManager.class);
//		putEntry(APP_VERSION, Util.getVersionString(context));
//		putEntry(TOU_VERSION, userManager.getUser().getTouVersion());
//	}

	public TrackMetadata(String appVersion, String touVersion){
		putEntry(APP_VERSION, appVersion);
		putEntry(TOU_VERSION, touVersion);
	}
	
	public void putEntry(String key, String value) {
		if (value == null) return;
		
		this.entries.put(key, value);
	}

	public void merge(TrackMetadata newMetadata) {
		if (newMetadata == null) {
			return;
		}
		
		for (String key : newMetadata.entries.keySet()) {
			String newValue = newMetadata.entries.get(key);
			if (newValue != null && !newValue.isEmpty()) {
				this.entries.put(key, newValue);
			}
		}
	}

	public String toJsonString() throws JSONException {
		return toJson().toString();
	}

	public JSONObject toJson() throws JSONException {
		JSONObject result = new JSONObject();
		for (String key : this.entries.keySet()) {
			result.put(key, this.entries.get(key));
		}
		return result;
	}
	
	
}
