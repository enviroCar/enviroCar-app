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

import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.NumberWithUOM;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.ConsumptionFormula;
import org.envirocar.app.util.ConsumptionVolumeUnit;
import org.envirocar.app.util.DistanceUnit;
import org.envirocar.app.util.SpeedUnit;

import android.content.Context;
import android.preference.PreferenceManager;

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
		String unit = context.getResources().getString(R.string.not_applicable);
		
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
	
		String preferredSpeedUnit = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.SPEED_UNITS_LIST_KEY, null);
		
		SpeedUnit speedUnit = SpeedUnit.UNKNOWN;
		
		if (preferredSpeedUnit != null) {			
			logger.debug("Using preferredSpeedUnit: " + preferredSpeedUnit);				
			speedUnit = SpeedUnit.parse(preferredSpeedUnit);
		}else{
			logger.debug("A preferredSpeedUnit was not set, using common unit for locale: " + locale);			
			speedUnit = SpeedUnit.parse(context.getResources().getString(R.string.local_speed_unit));		
		}
		
		switch (speedUnit) {
		case KILOMETER_PER_HOUR:
			speed = speedInKMperHour;
			unit = context.getResources().getString(R.string.unit_kilometer_per_hour);
			break;
		case MILES_PER_HOUR:
			speed = speedInKMperHour/KM_TO_MILE_FACTOR;
			unit = context.getResources().getString(R.string.unit_miles_per_hour);
			break;
		default:
			break;
		}
		
		NumberWithUOM result = new NumberWithUOM(speed, unit);
		
		return result;		
		
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that represents the fuel consumption per hour in the volume unit of the current locale.
	 * As the fuel consumption in liter per hour already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other fuel volume units.
	 * 
	 * @param consumptionPerHour
	 * @return
	 */
	public NumberWithUOM getConsumptionValuePerHour(double consumptionPerHour){	
		
		Number fuelConsumption = 0.0;
		String unit = context.getResources().getString(R.string.not_applicable);
		
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
	
		String preferredFuelVolumeUnit = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.FUEL_VOLUME_UNITS_LIST_KEY, null);
		
		ConsumptionVolumeUnit consumptionVolumeUnit = ConsumptionVolumeUnit.UNKNOWN;
		
		if (preferredFuelVolumeUnit != null) {			
			logger.debug("Using preferredFuelVolumeUnit: " + preferredFuelVolumeUnit);			
			consumptionVolumeUnit = ConsumptionVolumeUnit.parse(preferredFuelVolumeUnit);			
		}else{			
			logger.debug("A preferredFuelVolumeUnit was not set, using common unit for locale: " + locale);			
			consumptionVolumeUnit = ConsumptionVolumeUnit.parse(context.getResources().getString(R.string.local_fuel_volume_unit));
		}
		
		switch (consumptionVolumeUnit) {
		case LITER:
			fuelConsumption = consumptionPerHour;
			unit = context.getResources().getString(R.string.liter) + "/h";
			break;
		case IMPERIAL_GALLON:
			fuelConsumption = consumptionPerHour / LITER_TO_IMPERIALGALLON_FACTOR;
			unit = context.getResources().getString(R.string.unit_imperial_gallon) + "/h";
			break;
		case US_GALLON:
			fuelConsumption = consumptionPerHour / LITER_TO_USGALLON_FACTOR;
			unit = context.getResources().getString(R.string.unit_us_liquid_gallon) + "/h";
			break;

		default:
			break;
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
		String unit = context.getResources().getString(R.string.not_applicable);
		
		ConsumptionFormula localConsumptionFormula = ConsumptionFormula.parse(context.getResources().getString(R.string.consumption_formula));
		
		switch (localConsumptionFormula) {
		case MILESPERUSGALLON:
			fuelConsumption = (100/LPer100kmTompgusFactor)/literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_us_gallon);
			break;
		case MILESPERIMPERIALGALLON:
			fuelConsumption = (100/LPer100kmTompgimperialFactor)/literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_imperial_gallon);
			break;
		case LITERSPER100KILOMETERS:
			fuelConsumption = literOn100km;		
			unit = context.getResources().getString(R.string.unit_liters_per_100_km);
			break;
		case KILOMETERSPERLITER:
			fuelConsumption = (1/literOn100km)*100;
			unit = context.getResources().getString(R.string.unit_kilometer_per_liter);
			break;
		case USGALLONSPER100MILES:
			fuelConsumption = LPer100kmTompgusFactor*literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_us_gallon);
			break;
		case IMPERIALGALLONSPER100MILES:
			fuelConsumption = LPer100kmTompgimperialFactor*literOn100km;	
			unit = context.getResources().getString(R.string.unit_imperial_gallons_per_100_miles);
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
		String unit = context.getResources().getString(R.string.not_applicable);
		
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
	
		String preferredDistanceUnit = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.DISTANCE_UNITS_LIST_KEY, null);
		
		DistanceUnit speedUnit = DistanceUnit.UNKNOWN;
		
		if (preferredDistanceUnit != null) {			
			logger.debug("Using preferredDistanceUnit: " + preferredDistanceUnit);				
			speedUnit = DistanceUnit.parse(preferredDistanceUnit);
		}else{			
			logger.debug("A preferredDistanceUnit was not set, using common unit for locale: " + locale);			
			speedUnit = DistanceUnit.parse(context.getResources().getString(R.string.local_distance_unit));		
		}
		
		switch (speedUnit) {
		case KILOMETER:
			distance = distanceInKM;
			unit = context.getResources().getString(R.string.unit_kilometer);
			break;
		case MILES:
			distance = distanceInKM/KM_TO_MILE_FACTOR;
			unit = context.getResources().getString(R.string.unit_miles);
			break;
		default:
			break;
		}
		
		NumberWithUOM result = new NumberWithUOM(distance, unit);
		
		return result;	
		
	}
	
	public void getCurrency(){
		
	}
	
	public void getTime(){
		
	}
	
}
