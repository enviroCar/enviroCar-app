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
	private double engineDisplacement;
	
	public Car(FuelType fuelType, String manufacturer, String model, String id,
			double engineDisplacement) {
		this.fuelType = fuelType;
		this.manufacturer = manufacturer;
		this.model = model;
		this.id = id;
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

	public double getEngineDisplacement() {
		return engineDisplacement;
	}
	
	
}
