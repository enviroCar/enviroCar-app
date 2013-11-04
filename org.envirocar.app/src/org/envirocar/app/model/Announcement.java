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
package org.envirocar.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.envirocar.app.util.VersionRange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.envirocar.app.R;
import android.content.Context;

public class Announcement {

	public enum Priority {
		LOW {
			@Override
			public String toString() {
				return "low";
			}
		},
		MEDIUM {
			@Override
			public String toString() {
				return "medium";
			}
		},
		HIGH {
			@Override
			public String toString() {
				return "high";
			}
		};
		
		public static Priority fromString(String s) {
			for (Priority p : values()) {
				if (p.toString().equals(s)) {
					return p;
				}
			}
			
			return LOW;
		}
	}

	public static List<Announcement> fromJsonList(JSONObject content) throws JSONException {
		JSONArray a = content.getJSONArray("announcements");
		
		List<Announcement> result = new ArrayList<Announcement>();
		
		for (int i = 0; i < a.length(); i++) {
			JSONObject obj = a.getJSONObject(i);
			Announcement anno = fromJson(obj);
			if (anno != null) {
				result.add(anno);
			}
		}
		
		return result;
	}

	
	public static Announcement fromJson(JSONObject json) throws JSONException {
		Announcement result = new Announcement();
		
		result.id = json.getString("id");
		result.versionRange = VersionRange.fromString(json.getString("versions"));
		result.category = json.getString("category");
		
		if (result.category == null || !(result.category.equalsIgnoreCase("app") || 
				result.category.equalsIgnoreCase("general"))) {
			return null;
		}
			
		
		result.contents = json.getJSONObject("content");
		result.priority = Priority.fromString(json.optString("priority", Priority.LOW.toString()));
		
		return result;
	}

	private String id;
	private VersionRange versionRange;
	private Priority priority;
	private String category;
	private JSONObject contents;

	public String getId() {
		return id;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public String getCategory() {
		return category;
	}
	
	public String getContent() {
		Locale locale = Locale.getDefault();
		return getContent(locale);
	}

	public String getContent(Locale locale) {
		String result = null;
		if (contents != null) {
			try {
				result = contents.getString(locale.getLanguage());
			} catch (JSONException e) {
			}
			
			if (result == null) {
				try {
					result = contents.getString(Locale.ENGLISH.getLanguage());
				} catch (JSONException e) {
				}
			}
			
		}
		return result;
	}
	
	public String createUITitle(Context ctx) {
		String priorityi18n;
		if (priority.equals(Priority.HIGH)) {
			priorityi18n = ctx.getString(R.string.category_high);
		}
		else if (priority.equals(Priority.MEDIUM)) {
			priorityi18n = ctx.getString(R.string.category_normal);
		}
		else {
			priorityi18n = ctx.getString(R.string.category_low);
		}
		return String.format("[%s] %s %s", priorityi18n, category, ctx.getString(R.string.announcement));
	}


	public Priority getPriority() {
		return priority;
	}

}
