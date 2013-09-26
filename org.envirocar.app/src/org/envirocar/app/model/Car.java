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

/**
 * Class holding all information for a car instance
 * 
 * @author matthes rieke
 *
 */
public class Car {

	public static final String TEMPORARY_SENSOR_ID = "%TMP_ID%";
	
	public enum FuelType {
		GASOLINE {
		    public String toString() {
		        return "gasoline";
		    }
		},
		DIESEL {
			public String toString() {
		        return "diesel";
		    }
		}
	}
	
	private FuelType fuelType;
	private String manufacturer;
	private String model;
	private String id;
	private int constructionYear;
	private double engineDisplacement;
	
	public Car(FuelType fuelType, String manufacturer, String model, String id, int year, double engineDisplacement) {
		this.fuelType = fuelType;
		this.manufacturer = manufacturer;
		this.model = model;
		this.id = id;
		this.constructionYear = year;
		this.engineDisplacement = engineDisplacement;
	}
	
	public Car(String fuelType, String manufacturer, String model, String id, int year, double engineDisplacement) {
		if (fuelType.equalsIgnoreCase(FuelType.GASOLINE.toString())) {
			this.fuelType = FuelType.GASOLINE;
		} else if (fuelType.equalsIgnoreCase(FuelType.DIESEL.toString())){
			this.fuelType = FuelType.DIESEL;
		}
		this.manufacturer = manufacturer;
		this.model = model;
		this.id = id;
		this.constructionYear = year;
		this.engineDisplacement = engineDisplacement;
	}

	public FuelType getFuelType() {
		return fuelType;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getModel() {
		return model;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String newID){
		this.id = newID;
	}

	public int getConstructionYear() {
		return constructionYear;
	}

	public void setConstructionYear(int year) {
		this.constructionYear = year;
	}

	public double getEngineDisplacement() {
		return engineDisplacement;
	}
}
