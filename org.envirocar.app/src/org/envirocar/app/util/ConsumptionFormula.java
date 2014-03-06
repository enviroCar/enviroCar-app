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

public enum ConsumptionFormula {

	MILESPERUSGALLON("mpg(us)"), MILESPERIMPERIALGALLON("mpg(imp)"), LITERSPER100KILOMETERS("L/100km"), KILOMETERSPERLITER("km/L"), USGALLONSPER100MILES("g(us)/100mi"), IMPERIALGALLONSPER100MILES("g(imp)/100mi"), UNKNOWNFORMULA("unknown");
	
	private String value;
	
	private ConsumptionFormula(String stringValue){
		this.value = stringValue;
	}
	
	public static ConsumptionFormula parse(String value) {
		
		if(value.equals(MILESPERUSGALLON.toString())){
			return MILESPERUSGALLON;
		}else if(value.equals(MILESPERIMPERIALGALLON.toString())){
			return MILESPERIMPERIALGALLON;
		}else if(value.equals(USGALLONSPER100MILES.toString())){
			return USGALLONSPER100MILES;
		}else if(value.equals(LITERSPER100KILOMETERS.toString())){
			return LITERSPER100KILOMETERS;
		}else if(value.equals(KILOMETERSPERLITER.toString())){
			return KILOMETERSPERLITER;
		}else if(value.equals(USGALLONSPER100MILES.toString())){
			return USGALLONSPER100MILES;
		}else if(value.equals(IMPERIALGALLONSPER100MILES.toString())){
			return IMPERIALGALLONSPER100MILES;
		}
		
		return UNKNOWNFORMULA;
	}
	
	@Override
	public String toString() {
		return value;
	}
				
}
