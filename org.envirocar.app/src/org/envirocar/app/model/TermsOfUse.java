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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TermsOfUse {

	public static TermsOfUse fromJson(JSONObject json) throws JSONException {
		JSONArray array = json.getJSONArray("termsOfUse");
		
		List<TermsOfUseInstance> list = new ArrayList<TermsOfUseInstance>(array.length());
		for (int i = 0; i < array.length(); i++) {
			list.add(TermsOfUseInstance.fromJson(array.getJSONObject(i)));
		}
		
		return new TermsOfUse(list);
	}
	
	
	private List<TermsOfUseInstance> instances;

	private TermsOfUse(List<TermsOfUseInstance> instances) {
		this.instances = instances;
	}

	public List<TermsOfUseInstance> getInstances() {
		return instances;
	}
	
	
	
}
