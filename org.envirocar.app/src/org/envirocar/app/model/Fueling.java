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

import java.util.Date;

import org.envirocar.app.activity.LogbookFragment;
import org.envirocar.app.model.dao.FuelingDAO;

/**
 * Simple POJO representing a fueling stop as used by {@link LogbookFragment}
 * and {@link FuelingDAO}
 */
public class Fueling {

	private Car car;
	private String comment;
	private NumberWithUOM mileage;
	private NumberWithUOM cost;
	private NumberWithUOM volume;
	private boolean missedFuelStop;
	private Date time;
	
	/**
	 * @return the car used
	 */
	public Car getCar() {
		return car;
	}
	
	public void setCar(Car type) {
		this.car = type;
	}
	
	/**
	 * @return a generic comment
	 */
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	/**
	 * @return the total mileage of the car
	 */
	public NumberWithUOM getMileage() {
		return mileage;
	}
	
	public void setMileage(NumberWithUOM mileage) {
		this.mileage = mileage;
	}
	
	/**
	 * @return the financial cost for this fueling
	 */
	public NumberWithUOM getCost() {
		return cost;
	}
	
	public void setCost(NumberWithUOM cost) {
		this.cost = cost;
	}
	
	/**
	 * @return the volume for this fueling
	 */
	public NumberWithUOM getVolume() {
		return volume;
	}

	public void setVolume(NumberWithUOM volume) {
		this.volume = volume;
	}

	/**
	 * @return true if the user implied that he missed
	 * to log an etry since the last app fueling entry.
	 */
	public boolean isMissedFuelStop() {
		return missedFuelStop;
	}

	public void setMissedFuelStop(boolean missedFuelStop) {
		this.missedFuelStop = missedFuelStop;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
	
	
	
}
