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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents a user with a name and a password.
 * 
 */
public class User {

	private static final String TOU_VERSION = "touVersion";
	private static final String NAME = "name";
	private String username;
	private String token;
	private String touVersion;

	/**
	 * Creates a new user with given parameters
	 * 
	 * @param username
	 *            username
	 * @param token
	 *            password
	 */
	public User(String username, String token) {
		this.username = username;
		this.token = token;
		// TODO write to sharedpreferences
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
	
	public void setToken(String token2) {
		this.token = token2;
	}

	public String getTouVersion() {
		return touVersion;
	}

	public void setTouVersion(String acceptedTermsOfUseVersion) {
		this.touVersion = acceptedTermsOfUseVersion;
	}

	public static User fromJson(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject(json);
		return fromJson(jsonObject);
	}

	public static User fromJson(JSONObject json) throws JSONException {
		String name = json.getString(NAME);
		String touVersion = json.optString(TOU_VERSION, null);
		User result = new User(name, null);
		result.setTouVersion(touVersion);
		return result;
	}

	public String toJson() throws JSONException {
		return toJson(false);
	}
	
	public String toJson(boolean withUsername) throws JSONException {
		JSONObject result = new JSONObject();
		if (withUsername) {
			result.put(NAME, getUsername());
		}
		result.put(TOU_VERSION, getTouVersion());
		return result.toString();
	}

	
}
