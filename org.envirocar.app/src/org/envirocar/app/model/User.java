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

import com.google.gson.annotations.SerializedName;

import org.envirocar.app.model.dao.service.UserService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents a user with a name and a password.
 * 
 */
public class User {
	private static final String TOU_VERSION = "touVersion";
	private static final String NAME = "name";
	private static final String MAIL = "mail";
	private static final String TOKEN = "token";

	@SerializedName(UserService.KEY_USER_NAME)
	private String username;
	private String token;
	@SerializedName(UserService.KEY_USER_TOU_VERSION)
	private String touVersion;
	@SerializedName(UserService.KEY_USER_MAIL)
	private String mail;

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
	}

    public User(String username, String token, String mail){
        this.username = username;
        this.token = token;
        this.mail = mail;
    }

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	
	public void setMail(String mail) {
		this.mail = mail;
	}
	
	public String getMail() {
		return mail;
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
		User result = new User(name, null);
		
		String touVersion = json.optString(TOU_VERSION, null);
		result.setTouVersion(touVersion);
		
		String m = json.optString(MAIL, null);
		result.setMail(m);
		return result;
	}

	public String toJson() throws JSONException {
		return toJson(false);
	}
	
	public String toJson(boolean withUsernameToken) throws JSONException {
		JSONObject result = new JSONObject();

		if (getMail() != null) {
			result.put(MAIL, getMail());
		}
		
		if (getTouVersion() != null) {
			result.put(TOU_VERSION, getTouVersion());
		}
		
		if (withUsernameToken) {
			result.put(TOKEN, getToken());
			result.put(NAME, getUsername());
		}
		

		return result.toString();
	}

	
}
