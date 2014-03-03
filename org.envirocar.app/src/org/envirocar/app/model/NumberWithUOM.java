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
 * class for a {@link Number} with an attached
 * unit of measurement. A phenomenon usually has
 * a unit.
 */
public class NumberWithUOM {

	private Number value;
	private String unit;
	
	public NumberWithUOM(Number val, String unit) {
		this.value = val;
		this.unit = unit;
	}

	public Number getValue() {
		return value;
	}
	
	public void setValue(Number value) {
		this.value = value;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public void setUnit(String unit) {
		this.unit = unit;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject result = new JSONObject();
		result.put("value", this.value);
		result.put("unit", this.unit);
		return result;
	}
	
	
	
}
