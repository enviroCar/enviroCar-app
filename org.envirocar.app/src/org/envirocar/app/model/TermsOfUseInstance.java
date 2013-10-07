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

public class TermsOfUseInstance {

	public static TermsOfUseInstance fromJson(JSONObject json) throws JSONException {
		String date = json.getString("issuedDate");
		String contents = json.optString("contents", null);
		String id = json.optString("id", null);
		TermsOfUseInstance result = new TermsOfUseInstance(id, date, contents);
		return result;
	}
	
	public static TermsOfUseInstance fromIssuedDate(String date) {
		TermsOfUseInstance result = new TermsOfUseInstance(null, date, null);
		return result;
	}
	
	private String issuedDate;
	private String contents;
	private String id;
	
	private TermsOfUseInstance(String id, String issuedDate, String contents) {
		this.id = id;
		this.issuedDate = issuedDate;
		this.contents = contents;
	}

	public String getIssuedDate() {
		return issuedDate;
	}

	public String getContents() {
		return contents;
	}
	
	public String getId() {
		return id;
	}

	
}
