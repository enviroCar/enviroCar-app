/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.storage;

import java.util.HashMap;
import java.util.Map;

import org.envirocar.app.application.UserManager;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;


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
	
	public TrackMetadata(Context ctx) {
		putEntry(APP_VERSION, Util.getVersionString(ctx));
		putEntry(TOU_VERSION, UserManager.instance().getUser().getTouVersion());
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
			this.entries.put(key, newMetadata.entries.get(key));
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
