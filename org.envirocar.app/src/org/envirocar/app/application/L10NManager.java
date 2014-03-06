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
package org.envirocar.app.application;

import java.util.Locale;

import org.envirocar.app.R;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.NumberWithUOM;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.ConsumptionFormula;

import android.content.Context;

/**
 * This class returns different locale dependent values, e.g. currency, speed, distance.
 * 
 * @author bpross-52n
 *
 */
public class L10NManager {

	private static Logger logger = Logger.getLogger(L10NManager.class);
	private Context context;
	
	public static final String UNIT_KILOMETER = "km";
	public static final String UNIT_MILES = "mi";
	public static final String UNIT_KILOMETER_PER_HOUR = "km/h";
	public static final String UNIT_MILES_PER_HOUR = "mph";
	public static final double KM_TO_MILE_FACTOR = 1.609344;	
	
	public static final double LITER_TO_USGALLON_FACTOR = 3.785411784;
	public static final double LITER_TO_IMPERIALGALLON_FACTOR = 4.54609;
	
	private double LPer100kmTompgusFactor = KM_TO_MILE_FACTOR / LITER_TO_USGALLON_FACTOR;
	private double LPer100kmTompgimperialFactor = KM_TO_MILE_FACTOR / LITER_TO_IMPERIALGALLON_FACTOR;
	
	public L10NManager(Context ctx){
		this.context = ctx;
	}
	
	public NumberWithUOM getSpeed(int speedInKMperHour){
		Number speed = 0.0;
		String unit = context.getResources().getString(R.string.speed_unit);
		
		if(unit.equals(UNIT_KILOMETER_PER_HOUR)){
			speed = speedInKMperHour;
		}else if(unit.equals(UNIT_MILES_PER_HOUR)){
			speed = speedInKMperHour/KM_TO_MILE_FACTOR;
		}
		NumberWithUOM result = new NumberWithUOM(speed, unit);
		
		return result;
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that represents the fuel consumption per hour in the volume unit of the current locale.
	 * As the fuel consumption in liter per hour already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other fuel volume units.
	 * 
	 * @param literOn100km
	 * @return
	 */
	public NumberWithUOM getConsumptionValuePerHour(double consumptionPerHour){		
		
		Number fuelConsumption = 0.0;
		String unit = context.getResources().getString(R.string.fuel_volume_unit) + "/h";
		
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
		
		if(locale.equals(Locale.GERMANY.getDisplayName())){
			fuelConsumption = consumptionPerHour;
		}else if(locale.equals(Locale.UK.getDisplayName())){
			fuelConsumption = consumptionPerHour / LITER_TO_IMPERIALGALLON_FACTOR;
		}else if(locale.equals(Locale.US.getDisplayName())){
			fuelConsumption = consumptionPerHour / LITER_TO_USGALLON_FACTOR;
		}
		
		NumberWithUOM result = new NumberWithUOM(fuelConsumption, unit);
		
		return result;
		
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that represents the fuel consumption and unit in the current locale's common format.
	 * As the fuel consumption in liter per 100 km already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other common fuel consumption representations.
	 * 
	 * @param literOn100km
	 * @return
	 */
	public NumberWithUOM getCommonConsumptionValue(double literOn100km){		
		
		Number fuelConsumption = 0.0;
		String unit = context.getResources().getString(R.string.consumption_unit);
		
		ConsumptionFormula localConsumptionFormula = ConsumptionFormula.parse(context.getResources().getString(R.string.consumption_formula));
		
		switch (localConsumptionFormula) {
		case MILESPERUSGALLON:
			fuelConsumption = (100/LPer100kmTompgusFactor)/literOn100km;
			break;
		case MILESPERIMPERIALGALLON:
			fuelConsumption = (100/LPer100kmTompgimperialFactor)/literOn100km;
			break;
		case LITERSPER100KILOMETERS:
			fuelConsumption = literOn100km;			
			break;
		case KILOMETERSPERLITER:
			fuelConsumption = (1/literOn100km)*100;
			break;
		case USGALLONSPER100MILES:
			fuelConsumption = LPer100kmTompgusFactor*literOn100km;
			break;
		case IMPERIALGALLONSPER100MILES:
			fuelConsumption = LPer100kmTompgimperialFactor*literOn100km;			
			break;
		default:
			logger.info("No consumption formula found.");
			break;
		}

		NumberWithUOM result = new NumberWithUOM(fuelConsumption, unit);
		
		return result;
		
	}
	
	public NumberWithUOM getDistance(double distanceInKM){
		Number distance = 0.0;
		String unit = context.getResources().getString(R.string.distance_unit);
		
		if(unit.equals(UNIT_KILOMETER)){
			distance = distanceInKM;
		}else if(unit.equals(UNIT_MILES)){
			distance = distanceInKM/KM_TO_MILE_FACTOR;
		}
		NumberWithUOM result = new NumberWithUOM(distance, unit);
		
		return result;
	}
	
	public void getCurrency(){
		
	}
	
	public void getTime(){
		
	}
	
}
