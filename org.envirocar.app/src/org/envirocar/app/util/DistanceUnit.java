/* 
 * enviroCar 2014
 * Copyright (C) 2014 enviroCar contributors
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
package org.envirocar.app.util;

public enum DistanceUnit {

	KILOMETER("km"), MILES("mi"), UNKNOWN("unknown");
	
	private String value;
	
	private DistanceUnit(String stringValue){
		this.value = stringValue;
	}
	
	public static DistanceUnit parse(String value) {
		
		if(value.equals(KILOMETER.toString())){
			return KILOMETER;
		}else if(value.equals(MILES.toString())){
			return MILES;
		}
		
		return UNKNOWN;
	}
	
	@Override
	public String toString() {
		return value;
	}
				
}
