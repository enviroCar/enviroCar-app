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

package org.envirocar.app.application;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents a user with a name and a password.
 * 
 */
public class User {

	private String username;
	private String token;
	private String acceptedTermsOfUseVersion;

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

	public String getAcceptedTermsOfUseVersion() {
		return acceptedTermsOfUseVersion;
	}

	public void setAcceptedTermsOfUseVersion(String acceptedTermsOfUseVersion) {
		this.acceptedTermsOfUseVersion = acceptedTermsOfUseVersion;
	}

	public static User fromJson(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject(json);
		return fromJson(jsonObject);
	}

	public static User fromJson(JSONObject json) throws JSONException {
		String name = json.getString("name");
		String touVersion = json.optString("acceptedTermsOfUseVersion", null);
		User result = new User(name, null);
		result.setAcceptedTermsOfUseVersion(touVersion);
		return result;
	}

	
}
