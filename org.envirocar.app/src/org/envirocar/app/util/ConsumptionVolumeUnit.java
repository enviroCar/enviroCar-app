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

public enum ConsumptionVolumeUnit {

	US_GALLON("gal(us)"), IMPERIAL_GALLON("gal(imp)"), LITER("l"), UNKNOWN("unknown");
	
	private String value;
	
	private ConsumptionVolumeUnit(String stringValue){
		this.value = stringValue;
	}
	
	public static ConsumptionVolumeUnit parse(String value) {
		
		if(value.equals(US_GALLON.toString())){
			return US_GALLON;
		}else if(value.equals(IMPERIAL_GALLON.toString())){
			return IMPERIAL_GALLON;
		}else if(value.equals(LITER.toString())){
			return LITER;
		}
		
		return UNKNOWN;
	}
	
	@Override
	public String toString() {
		return value;
	}
				
}
